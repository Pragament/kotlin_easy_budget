/*
 *   Copyright 2025 Benoit Letondor
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
package com.benoitletondor.FinanceTrackerapp.compose

import android.os.Build
import android.os.Bundle
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.benoitletondor.FinanceTrackerapp.helper.serialization.SerializedExpense
import com.benoitletondor.FinanceTrackerapp.helper.serialization.SerializedSelectedOnlineAccount
import com.benoitletondor.FinanceTrackerapp.helper.serialization.SerializedYearMonth
import com.benoitletondor.FinanceTrackerapp.helper.launchCollect
import com.benoitletondor.FinanceTrackerapp.helper.serialization.toSerializedYearMonth
import com.benoitletondor.FinanceTrackerapp.view.createaccount.CreateAccountDestination
import com.benoitletondor.FinanceTrackerapp.view.createaccount.CreateAccountView
import com.benoitletondor.FinanceTrackerapp.view.expenseedit.ExpenseAddDestination
import com.benoitletondor.FinanceTrackerapp.view.expenseedit.ExpenseEditDestination
import com.benoitletondor.FinanceTrackerapp.view.expenseedit.ExpenseEditView
import com.benoitletondor.FinanceTrackerapp.view.expenseedit.ExpenseEditViewModelFactory
import com.benoitletondor.FinanceTrackerapp.view.login.LoginDestination
import com.benoitletondor.FinanceTrackerapp.view.login.LoginView
import com.benoitletondor.FinanceTrackerapp.view.login.LoginViewModelFactory
import com.benoitletondor.FinanceTrackerapp.view.main.MainDestination
import com.benoitletondor.FinanceTrackerapp.view.main.MainView
import com.benoitletondor.FinanceTrackerapp.view.manageaccount.ManageAccountDestination
import com.benoitletondor.FinanceTrackerapp.view.manageaccount.ManageAccountView
import com.benoitletondor.FinanceTrackerapp.view.manageaccount.ManageAccountViewModelFactory
import com.benoitletondor.FinanceTrackerapp.view.monthlyreport.MonthlyReportDestination
import com.benoitletondor.FinanceTrackerapp.view.monthlyreport.MonthlyReportView
import com.benoitletondor.FinanceTrackerapp.view.monthlyreport.MonthlyReportViewModelFactory
import com.benoitletondor.FinanceTrackerapp.view.monthlyreport.export.MonthlyReportExportDestination
import com.benoitletondor.FinanceTrackerapp.view.monthlyreport.export.MonthlyReportExportView
import com.benoitletondor.FinanceTrackerapp.view.monthlyreport.export.MonthlyReportExportViewModelFactory
import com.benoitletondor.FinanceTrackerapp.view.onboarding.OnboardingDestination
import com.benoitletondor.FinanceTrackerapp.view.onboarding.OnboardingResult
import com.benoitletondor.FinanceTrackerapp.view.onboarding.OnboardingView
import com.benoitletondor.FinanceTrackerapp.view.premium.PremiumDestination
import com.benoitletondor.FinanceTrackerapp.view.premium.PremiumView
import com.benoitletondor.FinanceTrackerapp.view.recurringexpenseadd.RecurringExpenseAddDestination
import com.benoitletondor.FinanceTrackerapp.view.recurringexpenseadd.RecurringExpenseEditDestination
import com.benoitletondor.FinanceTrackerapp.view.recurringexpenseadd.RecurringExpenseEditView
import com.benoitletondor.FinanceTrackerapp.view.recurringexpenseadd.RecurringExpenseEditViewModelFactory
import com.benoitletondor.FinanceTrackerapp.view.settings.SettingsView
import com.benoitletondor.FinanceTrackerapp.view.settings.SettingsViewDestination
import com.benoitletondor.FinanceTrackerapp.view.settings.backup.BackupSettingsDestination
import com.benoitletondor.FinanceTrackerapp.view.settings.backup.BackupSettingsView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import upiqrexpenseadd.UpiQrEditView
import upiqrexpenseadd.UpiQrEditViewModelFactory
import upiqrexpenseadd.UpiQrExpenseAddDestination
import upiqrexpenseadd.UpiQrExpenseEditDestination
import java.time.LocalDate
import kotlin.reflect.typeOf

private const val OnboardingResultKey = "OnboardingResult"

@Composable
fun AppNavHost(
    closeApp: () -> Unit,
    openSubscriptionScreenFlow: Flow<Unit>,
    openAddExpenseScreenLiveFlow: Flow<Unit>,
    openAddRecurringExpenseScreenLiveFlow: Flow<Unit>,
    openMonthlyReportScreenFlow: Flow<Unit>,
    openUpiQrExpenseScreenLiveFlow: Flow<Unit>,
) {
    val navController = rememberNavController()

    LaunchedEffect(key1 = "openSubscriptionScreenListener") {
        launchCollect(openSubscriptionScreenFlow) {
            navController.navigate(PremiumDestination(startOnPro = false))
        }
    }

    NavHost(
        modifier = Modifier.fillMaxSize(),
        navController = navController,
        startDestination = MainDestination,
        enterTransition = { slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Left,
        )},
        exitTransition = { slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Left,
        )},
        popEnterTransition = { slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Right,
        )},
        popExitTransition = { slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Right,
        )},
    ) {
        composable<MainDestination>(
            enterTransition = null,
        ) { navBackStackEntry ->
            val onboardingResultFlow = remember(navBackStackEntry) {
                navBackStackEntry.savedStateHandle.getStateFlow<OnboardingResult?>(OnboardingResultKey, null)
                    .filterNotNull()
                    .onEach {
                        navBackStackEntry.savedStateHandle.remove<OnboardingResult>(OnboardingResultKey)
                    }
            }

            MainView(
                openAddExpenseScreenLiveFlow = openAddExpenseScreenLiveFlow,
                openAddRecurringExpenseScreenLiveFlow = openAddRecurringExpenseScreenLiveFlow,
                openMonthlyReportScreenFromNotificationFlow = openMonthlyReportScreenFlow,
                openUpiQrExpenseScreenLiveFlow = openUpiQrExpenseScreenLiveFlow,
                navigateToOnboarding = {
                    navController.navigate(OnboardingDestination)
                },
                onboardingResultFlow = onboardingResultFlow,
                closeApp = closeApp,
                navigateToPremium = { startOnPro ->
                    navController.navigate(PremiumDestination(startOnPro = startOnPro))
                },
                navigateToMonthlyReport = { fromNotification ->
                    navController.navigate(MonthlyReportDestination(fromNotification = fromNotification))
                },
                navigateToManageAccount = { account ->
                    navController.navigate(ManageAccountDestination(selectedAccount = SerializedSelectedOnlineAccount(account)))
                },
                navigateToSettings = {
                    navController.navigate(SettingsViewDestination)
                },
                navigateToLogin = { shouldDismissAfterAuth ->
                    navController.navigate(LoginDestination(shouldDismissAfterAuth = shouldDismissAfterAuth))
                },
                navigateToCreateAccount = {
                    navController.navigate(CreateAccountDestination)
                },
                navigateToAddExpense = { date, editedExpense ->
                    if (editedExpense != null) {
                        navController.navigate(ExpenseEditDestination(date = date, editedExpense = editedExpense))
                    } else {
                        navController.navigate(ExpenseAddDestination(date = date))
                    }
                },
                navigateToUpiQrExpense = { date, editedExpense ->
                    if (editedExpense != null) {
                        navController.navigate(UpiQrExpenseEditDestination(date = date, editedExpense = editedExpense))
                    } else {
                        navController.navigate(UpiQrExpenseAddDestination(date = date))
                    }
                },
                navigateToAddRecurringExpense = { date, editedExpense ->
                    if (editedExpense != null) {
                        navController.navigate(RecurringExpenseEditDestination(date = date, editedExpense = editedExpense))
                    } else {
                        navController.navigate(RecurringExpenseAddDestination(date = date))
                    }
                },
            )
        }
        composable<OnboardingDestination>(
            popEnterTransition = null,
            enterTransition = null,
            popExitTransition = null,
        ) {
            OnboardingView(
                finishWithResult = { result ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(OnboardingResultKey, result)
                    navController.popBackStack()
                }
            )
        }
        composable<PremiumDestination> { backStackEntry ->
            val destination: PremiumDestination = backStackEntry.toRoute()
            PremiumView(
                startOnPro = destination.startOnPro,
                close = {
                    navController.popBackStack()
                }
            )
        }
        composable<MonthlyReportDestination> { backStackEntry ->
            val destination: MonthlyReportDestination = backStackEntry.toRoute()
            MonthlyReportView(
                viewModel = hiltViewModel(
                    creationCallback = { factory: MonthlyReportViewModelFactory ->
                        factory.create(
                            fromNotification = destination.fromNotification,
                        )
                    }
                ),
                navigateUp = {
                    navController.navigateUp()
                },
                navigateToExportToCsv = { month ->
                    navController.navigate(MonthlyReportExportDestination(month = month.toSerializedYearMonth()))
                }
            )
        }
        composable<ManageAccountDestination>(
            typeMap = mapOf(typeOf<SerializedSelectedOnlineAccount>() to OnlineAccountNavType),
        ) { backStackEntry ->
            val destination: ManageAccountDestination = backStackEntry.toRoute()
            ManageAccountView(
                viewModel = hiltViewModel(
                    creationCallback = { factory: ManageAccountViewModelFactory ->
                        factory.create(
                            selectedAccount = destination.selectedAccount.toSelectedAccount(),
                        )
                    }
                ),
                navigateUp = {
                    navController.navigateUp()
                },
                finish = {
                    navController.popBackStack()
                },
            )
        }
        composable<MonthlyReportExportDestination>(
            typeMap = mapOf(typeOf<SerializedYearMonth>() to SerializedYearMonthNavType),
        ){ backStackEntry ->
            val destination: MonthlyReportExportDestination = backStackEntry.toRoute()
            MonthlyReportExportView(
                viewModel = hiltViewModel(
                    creationCallback = { factory: MonthlyReportExportViewModelFactory ->
                        factory.create(
                            month = destination.month.toYearMonth(),
                        )
                    }
                ),
                navigateUp = {
                    navController.navigateUp()
                },
                finish = {
                    navController.popBackStack()
                },
            )
        }
        composable<SettingsViewDestination> {
            SettingsView(
                navigateUp = {
                    navController.navigateUp()
                },
                navigateToBackupSettings = {
                    navController.navigate(BackupSettingsDestination)
                },
                navigateToPremium = {
                    navController.navigate(PremiumDestination(startOnPro = false))
                }
            )
        }
        composable<BackupSettingsDestination> {
            BackupSettingsView(
                navigateUp = {
                    navController.navigateUp()
                }
            )
        }
        composable<LoginDestination> { backStackEntry ->
            val destination: LoginDestination = backStackEntry.toRoute()
            LoginView(
                viewModel = hiltViewModel(
                    creationCallback = { factory: LoginViewModelFactory ->
                        factory.create(
                            shouldDismissAfterAuth = destination.shouldDismissAfterAuth,
                        )
                    }
                ),
                navigateUp = {
                    navController.navigateUp()
                },
                finish = {
                    navController.popBackStack()
                },
            )
        }
        composable<CreateAccountDestination> {
            CreateAccountView(
                navigateUp = {
                    navController.navigateUp()
                },
                finish = {
                    navController.popBackStack()
                },
            )
        }
        composable<ExpenseEditDestination>(
            typeMap = mapOf(typeOf<SerializedExpense>() to SerializedExpenseNavType),
        ) { backStackEntry ->
            val destination: ExpenseEditDestination = backStackEntry.toRoute()
            ExpenseEditView(
                viewModel = hiltViewModel(
                    creationCallback = { factory: ExpenseEditViewModelFactory ->
                        factory.create(
                            date = LocalDate.ofEpochDay(destination.dateEpochDay),
                            editedExpense = destination.editedExpense.toExpense(),
                        )
                    }

                ),
                navigateUp = {
                    navController.navigateUp()
                },
                finish = {
                    navController.popBackStack()
                },
            )
        }
        composable<ExpenseAddDestination> { backStackEntry ->
            val destination: ExpenseAddDestination = backStackEntry.toRoute()
            ExpenseEditView(
                viewModel = hiltViewModel(
                    creationCallback = { factory: ExpenseEditViewModelFactory ->
                        factory.create(
                            date = LocalDate.ofEpochDay(destination.dateEpochDay),
                            editedExpense = null,
                        )
                    }

                ),
                navigateUp = {
                    navController.navigateUp()
                },
                finish = {
                    navController.popBackStack()
                },
            )
        }
        composable<RecurringExpenseEditDestination>(
            typeMap = mapOf(typeOf<SerializedExpense>() to SerializedExpenseNavType),
        ) { backStackEntry ->
            val destination: RecurringExpenseEditDestination = backStackEntry.toRoute()
            RecurringExpenseEditView(
                viewModel = hiltViewModel(
                    creationCallback = { factory: RecurringExpenseEditViewModelFactory ->
                        factory.create(
                            date = LocalDate.ofEpochDay(destination.dateEpochDay),
                            editedExpense = destination.editedExpense.toExpense(),
                        )
                    }

                ),
                navigateUp = {
                    navController.navigateUp()
                },
                finish = {
                    navController.popBackStack()
                },
            )
        }
        composable<RecurringExpenseAddDestination> { backStackEntry ->
            val destination: RecurringExpenseAddDestination = backStackEntry.toRoute()
            RecurringExpenseEditView(
                viewModel = hiltViewModel(
                    creationCallback = { factory: RecurringExpenseEditViewModelFactory ->
                        factory.create(
                            date = LocalDate.ofEpochDay(destination.dateEpochDay),
                            editedExpense = null,
                        )
                    }

                ),
                navigateUp = {
                    navController.navigateUp()
                },
                finish = {
                    navController.popBackStack()
                },
            )
        }

        composable<UpiQrExpenseEditDestination>(
            typeMap = mapOf(typeOf<SerializedExpense>() to SerializedExpenseNavType),
        ) { backStackEntry ->
            val destination: UpiQrExpenseEditDestination = backStackEntry.toRoute()
            UpiQrEditView(
                viewModel = hiltViewModel(
                    creationCallback = { factory: UpiQrEditViewModelFactory ->
                        factory.create(
                            date = LocalDate.ofEpochDay(destination.dateEpochDay),
                            editedExpense = destination.editedExpense.toExpense(),
                        )
                    }

                ),
                navigateUp = {
                    navController.navigateUp()
                },
                finish = {
                    navController.popBackStack()
                },
            )
        }
        composable<UpiQrExpenseAddDestination> { backStackEntry ->
            val destination: UpiQrExpenseAddDestination = backStackEntry.toRoute()
            UpiQrEditView(
                viewModel = hiltViewModel(
                    creationCallback = { factory: UpiQrEditViewModelFactory ->
                        factory.create(
                            date = LocalDate.ofEpochDay(destination.dateEpochDay),
                            editedExpense = null,
                        )
                    }

                ),
                navigateUp = {
                    navController.navigateUp()
                },
                finish = {
                    navController.popBackStack()
                },
            )
        }

    }
}

private val OnlineAccountNavType = object : NavType<SerializedSelectedOnlineAccount>(
    isNullableAllowed = false
) {
    override fun get(bundle: Bundle, key: String): SerializedSelectedOnlineAccount? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(key, SerializedSelectedOnlineAccount::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(key)
        }
    }

    override fun parseValue(value: String): SerializedSelectedOnlineAccount {
        return Json.decodeFromString<SerializedSelectedOnlineAccount>(value)
    }

    override fun serializeAsValue(value: SerializedSelectedOnlineAccount): String {
        return Json.encodeToString(value)
    }

    override fun put(bundle: Bundle, key: String, value: SerializedSelectedOnlineAccount) {
        bundle.putParcelable(key, value)
    }
}

private val SerializedYearMonthNavType = object : NavType<SerializedYearMonth>(
    isNullableAllowed = false
) {
    override fun get(bundle: Bundle, key: String): SerializedYearMonth? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(key, SerializedYearMonth::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(key) as? SerializedYearMonth
        }
    }

    override fun parseValue(value: String): SerializedYearMonth {
        val json = Json.parseToJsonElement(value)
        return SerializedYearMonth(json.jsonObject["year"]!!.jsonPrimitive.int, json.jsonObject["month"]!!.jsonPrimitive.int)
    }

    override fun serializeAsValue(value: SerializedYearMonth): String {
        return Json.encodeToJsonElement(mapOf("year" to value.year, "month" to value.month)).toString()
    }

    override fun put(bundle: Bundle, key: String, value: SerializedYearMonth) {
        bundle.putParcelable(key, value)
    }
}

private val SerializedExpenseNavType = object : NavType<SerializedExpense>(
    isNullableAllowed = false,
) {
    override fun get(bundle: Bundle, key: String): SerializedExpense? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(key, SerializedExpense::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(key) as? SerializedExpense
        }
    }

    override fun parseValue(value: String): SerializedExpense {
        return Json.decodeFromString(value) as SerializedExpense
    }

    override fun serializeAsValue(value: SerializedExpense): String {
        return Json.encodeToString(value)
    }

    override fun put(bundle: Bundle, key: String, value: SerializedExpense) {
        bundle.putParcelable(key, value)
    }
}
