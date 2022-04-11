package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
const val MAX_RETRIES: Int = 3

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService
) {
    /**
     * Processes the invoice by charging it. Tries to handle different exceptions but throws an exception if it
     * cannot handle it.
     */
    fun processInvoice() {
        logger.info { "Running the scheduler" }
        val invoices = invoiceService.fetchAll()

        for(invoice in invoices) {
            // Only charge invoices that have not been paid
            if(invoice.status == InvoiceStatus.PAID) {
                continue
            }

            // A retry mechanism in case of an exception
            var exception: Exception? = null
            for (i in 0..MAX_RETRIES) {
                try {
                    if (paymentProvider.charge(invoice)) {
                        invoice.status = InvoiceStatus.PAID
                        invoiceService.update(invoice)
                    }
                    // exit the loop if the charge did not throw a Network exception
                    break;
                } catch (networkEx: NetworkException) {
                    exception = networkEx
                    // Sleep for 1 second and then retry
                    Thread.sleep(1000)
                } catch (customerNotFoundEx: CustomerNotFoundException) {
                    exception = customerNotFoundEx
                    // if the customer is not updated, then there is no need to retry
                    if (updateCustomer(invoice)) break
                } catch (currencyMismatchEx: CurrencyMismatchException) {
                    exception = currencyMismatchEx
                    // Get the currency from the customer table and update the invoice
                    updateCurrency(invoice)
                }

                if (i == MAX_RETRIES && exception != null) {
                    throw exception
                }
            }

        }
    }

    /**
     * Gets the invoice customer from the customer table and
     * updates the currency in the invoice with the currency from the customer
     *
     * @param invoice: to get the customer ID from and to update the amount in (with new currency)
     */
    private fun updateCurrency(invoice: Invoice) {
        val customer = customerService.fetch(invoice.customerId)
        val amount = Money(value = invoice.amount.value, currency = customer.currency)
        invoice.amount = amount
        invoiceService.update(invoice)
    }

    /**
     * Creates a new customer with the currency from an invoice and updates the invoice with it.
     *
     * @param invoice: the invoice to get the currency from and to update the customer in
     */
    private fun updateCustomer(invoice: Invoice): Boolean {
        // Add the customer from the invoice to the customer table and update the invoice
        val customer = customerService.save(invoice.amount.currency)
        if (customer != null) {
            invoice.customerId = customer.id
            invoiceService.update(invoice)
        } else {
            return true
        }
        return false
    }
}
