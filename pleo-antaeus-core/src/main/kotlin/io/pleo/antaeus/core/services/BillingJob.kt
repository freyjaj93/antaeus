package io.pleo.antaeus.core.services

import org.quartz.Job
import org.quartz.JobExecutionContext

class BillingJob: Job {
    override fun execute(context: JobExecutionContext?) {
        val billingService = context?.scheduler?.context?.get("billingService") as BillingService
        billingService.processInvoice()
    }
}