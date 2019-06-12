/*
 *   Copyright 2015 Benoit LETONDOR
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.benoitletondor.easybudgetapp.push

import com.batch.android.Batch
import com.benoitletondor.easybudgetapp.BuildConfig
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.ParameterKeys
import com.benoitletondor.easybudgetapp.helper.Parameters
import com.benoitletondor.easybudgetapp.helper.UserHelper
import com.benoitletondor.easybudgetapp.iab.Iab
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

import java.util.Calendar
import java.util.Date

import org.koin.java.KoinJavaComponent.get

/**
 * Service that handles Batch pushes
 *
 * @author Benoit LETONDOR
 */
class PushService : FirebaseMessagingService() {

    private val iab = get(Iab::class.java)
    private val parameters = get(Parameters::class.java)

// ----------------------------------->

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check that the push is valid
        if (Batch.Push.shouldDisplayPush(this, remoteMessage)) {
            if (!shouldDisplayPush(remoteMessage)) {
                Logger.debug("Not displaying push cause conditions are not matching")
                return
            }

            // Display the notification
            Batch.Push.displayNotification(this, remoteMessage)
        }
    }

    /**
     * Check if the push should be displayed
     *
     * @param remoteMessage
     * @return true if should display the push, false otherwise
     */
    private fun shouldDisplayPush(remoteMessage: RemoteMessage): Boolean {
        return isUserOk(remoteMessage) && isVersionCompatible(remoteMessage) && isPremiumCompatible(remoteMessage)
    }

    /**
     * Check if the push should be displayed according to user choice
     *
     * @param remoteMessage
     * @return true if should display the push, false otherwise
     */
    private fun isUserOk(remoteMessage: RemoteMessage): Boolean {
        try {
            // Check if it's a daily reminder
            if (remoteMessage.data.containsKey(DAILY_REMINDER_KEY) && "true" == remoteMessage.data[DAILY_REMINDER_KEY]) {
                if (!iab.isUserPremium())
                // Only for premium users
                {
                    return false
                }

                if (!UserHelper.isUserAllowingDailyReminderPushes(parameters))
                // Check user choice
                {
                    return false
                }

                // Check if the app hasn't been opened today
                val lastOpenTimestamp = parameters.getLong(ParameterKeys.LAST_OPEN_DATE, 0)
                if (lastOpenTimestamp == 0L) {
                    return false
                }

                val lastOpen = Date(lastOpenTimestamp)

                val cal = Calendar.getInstance()
                val currentDay = cal.get(Calendar.DAY_OF_YEAR)
                cal.time = lastOpen
                val lastOpenDay = cal.get(Calendar.DAY_OF_YEAR)

                return currentDay != lastOpenDay
            } else if (remoteMessage.data.containsKey(MONTHLY_REMINDER_KEY) && "true" == remoteMessage.data[MONTHLY_REMINDER_KEY]) {
                return iab.isUserPremium() && UserHelper.isUserAllowingMonthlyReminderPushes(parameters)
            }

            // Else it must be an update push
            return UserHelper.isUserAllowingUpdatePushes(parameters)
        } catch (e: Exception) {
            Logger.error("Error while checking user ok for push", e)
            return false
        }

    }

    /**
     * Check if the push should be displayed according to version constrains
     *
     * @param remoteMessage
     * @return true if should display the push, false otherwise
     */
    private fun isVersionCompatible(remoteMessage: RemoteMessage): Boolean {
        try {
            var maxVersion = BuildConfig.VERSION_CODE
            var minVersion = 1

            if (remoteMessage.data.containsKey(INTENT_MAX_VERSION_KEY)) {
                maxVersion = Integer.parseInt(remoteMessage.data[INTENT_MAX_VERSION_KEY]!!)
            }

            if (remoteMessage.data.containsKey(INTENT_MIN_VERSION_KEY)) {
                minVersion = Integer.parseInt(remoteMessage.data[INTENT_MIN_VERSION_KEY]!!)
            }

            return BuildConfig.VERSION_CODE in minVersion..maxVersion
        } catch (e: Exception) {
            Logger.error("Error while checking app version for push", e)
            return false
        }

    }

    /**
     * Check the user status if a push is marked as for premium or not.
     *
     * @param remoteMessage push intent
     * @return true if compatible, false otherwise
     */
    private fun isPremiumCompatible(remoteMessage: RemoteMessage): Boolean {
        try {
            if (remoteMessage.data.containsKey(INTENT_PREMIUM_KEY)) {
                val isForPremium = "true" == remoteMessage.data[INTENT_PREMIUM_KEY]

                return isForPremium == iab.isUserPremium()
            }

            return true
        } catch (e: Exception) {
            Logger.error("Error while checking premium compatible for push", e)
            return false
        }

    }

    companion object {
        /**
         * Key to retrieve the max version for a push
         */
        private const val INTENT_MAX_VERSION_KEY = "maxVersion"
        /**
         * Key to retrieve the max version for a push
         */
        private const val INTENT_MIN_VERSION_KEY = "minVersion"
        /**
         * Key to retrieve if a push is intented for premium user or not
         */
        private const val INTENT_PREMIUM_KEY = "premium"
        /**
         * Key to retrieve the daily reminder key for a push
         */
        const val DAILY_REMINDER_KEY = "daily"
        /**
         * Key to retrieve the monthly reminder key for a push
         */
        const val MONTHLY_REMINDER_KEY = "monthly"
    }
}
