package com.benoitletondor.FinanceTrackerapp.config

import kotlinx.coroutines.flow.StateFlow

interface Config {
    fun watchGlobalAlertMessage(): StateFlow<String?>
    fun watchProAlertMessage(): StateFlow<String?>
    fun watchProMigratedToPgAlertMessage(): StateFlow<String?>
}
