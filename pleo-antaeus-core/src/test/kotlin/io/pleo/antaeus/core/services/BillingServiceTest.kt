package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal


class BillingServiceTest {
    private val money = Money(value = BigDecimal(500.00), currency = Currency.SEK)
    private val invoice = Invoice(id = 1, customerId = 2, amount = money, status = InvoiceStatus.PENDING)
    private val paidInvoice = Invoice(id = 1, customerId = 2, amount = money, status = InvoiceStatus.PAID)

    private val invoiceService = mockk<InvoiceService> {
        every { fetchAll() } returns listOf(invoice)
        every { update(invoice) } returns paidInvoice
    }

    private val customerService = mockk<CustomerService> {}

    @Test
    fun `will process invoice successfully`() {
        // GIVEN
        val paymentProvider = mockk<PaymentProvider> {
            every {charge(invoice)} returns true
        }

        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = invoiceService,
            customerService = customerService
        )

        // WHEN/THEN
        assertDoesNotThrow { billingService.processInvoice() }
    }

    @Test
    fun `will fail to process invoice due to network error`() {
        // GIVEN
        val paymentProvider = mockk<PaymentProvider> {
            every {charge(invoice)} throws NetworkException()
        }

        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = invoiceService,
            customerService = customerService
        )

        // WHEN/THEN
        assertThrows<NetworkException> { billingService.processInvoice() }
    }

    @Test
    fun `will process invoice after CustomerNotFoundException`() {
        // GIVEN
        val customer = Customer(id = 3, currency = money.currency)
        val invoiceWithUpdatedCustomer = Invoice(id = 1, customerId = 3, amount = money, status = InvoiceStatus.PENDING)

        val paymentProvider = mockk<PaymentProvider> {
            every {charge(invoice)} throws CustomerNotFoundException(invoice.customerId)
            every { charge(invoiceWithUpdatedCustomer) } returns true
        }

        val customerService = mockk<CustomerService> {
            every { save(invoice.amount.currency) } returns customer
        }

        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = invoiceService,
            customerService = customerService
        )

        // WHEN/THEN
        assertDoesNotThrow { billingService.processInvoice() }
    }

    @Test
    fun `will process invoice after CurrencyMismatchException`() {
        // GIVEN
        val customer = Customer(id = 2, currency = Currency.EUR)
        val updateMoney = Money(value = BigDecimal(500.00), currency = Currency.EUR)
        val invoiceWithUpdatedCurrency = Invoice(id = 1, customerId = 2, amount = updateMoney, status = InvoiceStatus.PENDING)

        val paymentProvider = mockk<PaymentProvider> {
            every {charge(invoice)} throws CurrencyMismatchException(invoice.id, invoice.customerId)
            every { charge(invoiceWithUpdatedCurrency) } returns true
        }

        val customerService = mockk<CustomerService> {
            every { fetch(invoice.customerId) } returns customer
        }

        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = invoiceService,
            customerService = customerService
        )

        // WHEN/THEN
        assertDoesNotThrow { billingService.processInvoice() }
    }
}