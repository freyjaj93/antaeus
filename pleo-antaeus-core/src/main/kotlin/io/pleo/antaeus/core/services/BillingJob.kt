package io.pleo.antaeus.core.services

import mu.KotlinLogging
import org.quartz.Job
import org.quartz.JobExecutionContext

private val logger = KotlinLogging.logger {}

class BillingJob: Job {
    override fun execute(context: JobExecutionContext?) {
        logger.info { "Running the scheduler" }
        val billingService = context?.scheduler?.context?.get("billingService") as BillingService
        billingService.processInvoice()
    }
}