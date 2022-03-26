/*
 *   Copyright 2021 Benoit LETONDOR
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

package com.benoitletondor.easybudgetapp.view.main

import android.app.Dialog
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle

import com.benoitletondor.easybudgetapp.view.welcome.WelcomeActivity
import com.google.android.material.snackbar.Snackbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.viewModels

import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.databinding.ActivityMainBinding
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpenseDeleteType
import com.benoitletondor.easybudgetapp.view.main.calendar.CalendarFragment
import com.benoitletondor.easybudgetapp.view.selectcurrency.SelectCurrencyFragment
import com.getbase.floatingactionbutton.FloatingActionButton
import com.getbase.floatingactionbutton.FloatingActionsMenu
import com.roomorama.caldroid.CaldroidFragment
import com.roomorama.caldroid.CaldroidListener

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import com.benoitletondor.easybudgetapp.helper.*
import com.benoitletondor.easybudgetapp.iab.INTENT_IAB_STATUS_CHANGED
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.parameters.*
import com.benoitletondor.easybudgetapp.view.expenseedit.ExpenseEditActivity
import com.benoitletondor.easybudgetapp.view.recurringexpenseadd.RecurringExpenseEditActivity
import com.benoitletondor.easybudgetapp.view.report.base.MonthlyReportBaseActivity
import com.benoitletondor.easybudgetapp.view.settings.SettingsActivity
import com.benoitletondor.easybudgetapp.view.settings.SettingsActivity.Companion.SHOW_BACKUP_INTENT_KEY
import com.benoitletondor.easybudgetapp.view.welcome.getOnboardingStep
import com.google.android.material.snackbar.BaseTransientBottomBar
import dagger.hilt.android.AndroidEntryPoint

import javax.inject.Inject

/**
 * Main activity containing Calendar and List of expenses
 *
 * @author Benoit LETONDOR
 */
@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private lateinit var receiver: BroadcastReceiver

    private lateinit var calendarFragment: CalendarFragment
    private lateinit var expensesViewAdapter: ExpensesRecyclerViewAdapter

    private lateinit var menu: FloatingActionsMenu

    private var isUserPremium = false

    private var lastStopDate: Date? = null

    private val viewModel: MainViewModel by viewModels()

    @Inject lateinit var parameters: Parameters
    @Inject lateinit var iab: Iab

// ------------------------------------------>

    override fun createBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Launch welcome screen if needed
        if (parameters.getOnboardingStep() != WelcomeActivity.STEP_COMPLETED) {
            val startIntent = Intent(this, WelcomeActivity::class.java)
            ActivityCompat.startActivityForResult(this, startIntent, WELCOME_SCREEN_ACTIVITY_CODE, null)
        }

        setSupportActionBar(binding.toolbar)

        initCalendarFragment(savedInstanceState)
        initRecyclerView()

        // Register receiver
        val filter = IntentFilter()
        filter.addAction(INTENT_EXPENSE_DELETED)
        filter.addAction(INTENT_RECURRING_EXPENSE_DELETED)
        filter.addAction(SelectCurrencyFragment.CURRENCY_SELECTED_INTENT)
        filter.addAction(INTENT_SHOW_WELCOME_SCREEN)
        filter.addAction(Intent.ACTION_VIEW)
        filter.addAction(INTENT_IAB_STATUS_CHANGED)
        filter.addAction(INTENT_SHOW_CHECKED_BALANCE_CHANGED)

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    INTENT_EXPENSE_DELETED -> {
                        val expense = intent.getParcelableExtra<Expense>("expense")!!

                        viewModel.onDeleteExpenseClicked(expense)
                    }
                    INTENT_RECURRING_EXPENSE_DELETED -> {
                        val expense = intent.getParcelableExtra<Expense>("expense")!!
                        val deleteType = RecurringExpenseDeleteType.fromValue(intent.getIntExtra("deleteType", RecurringExpenseDeleteType.ALL.value))!!

                        viewModel.onDeleteRecurringExpenseClicked(expense, deleteType)
                    }
                    SelectCurrencyFragment.CURRENCY_SELECTED_INTENT -> viewModel.onCurrencySelected()
                    INTENT_SHOW_WELCOME_SCREEN -> {
                        val startIntent = Intent(this@MainActivity, WelcomeActivity::class.java)
                        ActivityCompat.startActivityForResult(this@MainActivity, startIntent, WELCOME_SCREEN_ACTIVITY_CODE, null)
                    }
                    INTENT_IAB_STATUS_CHANGED -> viewModel.onIabStatusChanged()
                    INTENT_SHOW_CHECKED_BALANCE_CHANGED -> viewModel.onShowCheckedBalanceChanged()
                }
            }
        }

        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(receiver, filter)

        if (intent != null) {
            openSettingsIfNeeded(intent)
            openMonthlyReportIfNeeded(intent)
            openPremiumIfNeeded(intent)
            openAddExpenseIfNeeded(intent)
            openAddRecurringExpenseIfNeeded(intent)
            openSettingsForBackupIfNeeded(intent)
        }

        viewModel.expenseDeletionSuccessEventStream.observe(this) { (deletedExpense, newBalance, maybeNewCheckecBalance) ->

            expensesViewAdapter.removeExpense(deletedExpense)
            updateBalanceDisplayForDay(
                expensesViewAdapter.getDate(),
                newBalance,
                maybeNewCheckecBalance
            )
            calendarFragment.refreshView()

            val snackbar = Snackbar.make(
                binding.coordinatorLayout,
                if (deletedExpense.isRevenue()) R.string.income_delete_snackbar_text else R.string.expense_delete_snackbar_text,
                Snackbar.LENGTH_LONG
            )
            snackbar.setAction(R.string.undo) {
                viewModel.onExpenseDeletionCancelled(deletedExpense)
            }
            snackbar.setActionTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.snackbar_action_undo
                )
            )

            snackbar.duration = BaseTransientBottomBar.LENGTH_LONG
            snackbar.show()
        }

        viewModel.expenseDeletionErrorEventStream.observe(this) {
            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.expense_delete_error_title)
                .setMessage(R.string.expense_delete_error_message)
                .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                .show()
        }

        viewModel.expenseRecoverySuccessEventStream.observe(this) {
            // Nothing to do
        }

        viewModel.expenseRecoveryErrorEventStream.observe(this) { expense ->
            Logger.error("Error restoring deleted expense: $expense")
        }

        var expenseDeletionDialog: ProgressDialog? = null
        viewModel.recurringExpenseDeletionProgressEventStream.observe(this) { status ->
            when (status) {
                is MainViewModel.RecurringExpenseDeleteProgressState.Starting -> {
                    val dialog = ProgressDialog(this@MainActivity)
                    dialog.isIndeterminate = true
                    dialog.setTitle(R.string.recurring_expense_delete_loading_title)
                    dialog.setMessage(resources.getString(R.string.recurring_expense_delete_loading_message))
                    dialog.setCanceledOnTouchOutside(false)
                    dialog.setCancelable(false)
                    dialog.show()

                    expenseDeletionDialog = dialog
                }
                is MainViewModel.RecurringExpenseDeleteProgressState.ErrorCantDeleteBeforeFirstOccurrence -> {
                    expenseDeletionDialog?.dismiss()
                    expenseDeletionDialog = null

                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.recurring_expense_delete_first_error_title)
                        .setMessage(R.string.recurring_expense_delete_first_error_message)
                        .setNegativeButton(R.string.ok, null)
                        .show()
                }
                is MainViewModel.RecurringExpenseDeleteProgressState.ErrorRecurringExpenseDeleteNotAssociated -> {
                    showGenericRecurringDeleteErrorDialog()

                    expenseDeletionDialog?.dismiss()
                    expenseDeletionDialog = null
                }
                is MainViewModel.RecurringExpenseDeleteProgressState.ErrorIO -> {
                    showGenericRecurringDeleteErrorDialog()

                    expenseDeletionDialog?.dismiss()
                    expenseDeletionDialog = null
                }
                is MainViewModel.RecurringExpenseDeleteProgressState.Deleted -> {
                    val snackbar = Snackbar.make(
                        binding.coordinatorLayout,
                        R.string.recurring_expense_delete_success_message,
                        Snackbar.LENGTH_LONG
                    )

                    snackbar.setAction(R.string.undo) {
                        viewModel.onRestoreRecurringExpenseClicked(
                            status.recurringExpense,
                            status.restoreRecurring,
                            status.expensesToRestore
                        )
                    }

                    snackbar.setActionTextColor(
                        ContextCompat.getColor(
                            this@MainActivity,
                            R.color.snackbar_action_undo
                        )
                    )
                    snackbar.duration = BaseTransientBottomBar.LENGTH_LONG
                    snackbar.show()

                    expenseDeletionDialog?.dismiss()
                    expenseDeletionDialog = null
                }
            }
        }

        var expenseRestoreDialog: Dialog? = null
        viewModel.recurringExpenseRestoreProgressEventStream.observe(this) { status ->
            when (status) {
                is MainViewModel.RecurringExpenseRestoreProgressState.Starting -> {
                    val dialog = ProgressDialog(this@MainActivity)
                    dialog.isIndeterminate = true
                    dialog.setTitle(R.string.recurring_expense_restoring_loading_title)
                    dialog.setMessage(resources.getString(R.string.recurring_expense_restoring_loading_message))
                    dialog.setCanceledOnTouchOutside(false)
                    dialog.setCancelable(false)
                    dialog.show()

                    expenseRestoreDialog = dialog
                }
                is MainViewModel.RecurringExpenseRestoreProgressState.ErrorIO -> {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.recurring_expense_restore_error_title)
                        .setMessage(resources.getString(R.string.recurring_expense_restore_error_message))
                        .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                        .show()

                    expenseRestoreDialog?.dismiss()
                    expenseRestoreDialog = null
                }
                is MainViewModel.RecurringExpenseRestoreProgressState.Restored -> {
                    Snackbar.make(
                        binding.coordinatorLayout,
                        R.string.recurring_expense_restored_success_message,
                        Snackbar.LENGTH_LONG
                    ).show()

                    expenseRestoreDialog?.dismiss()
                    expenseRestoreDialog = null
                }
            }
        }

        viewModel.startCurrentBalanceEditorEventStream.observe(this) { currentBalance ->
            val dialogView = layoutInflater.inflate(R.layout.dialog_adjust_balance, null)
            val amountEditText = dialogView.findViewById<EditText>(R.id.balance_amount)
            amountEditText.setText(
                if (currentBalance == 0.0) "0" else CurrencyHelper.getFormattedAmountValue(
                    currentBalance
                )
            )
            amountEditText.preventUnsupportedInputForDecimals()
            amountEditText.setSelection(amountEditText.text.length) // Put focus at the end of the text

            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.adjust_balance_title)
            builder.setMessage(R.string.adjust_balance_message)
            builder.setView(dialogView)
            builder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            builder.setPositiveButton(R.string.ok) { dialog, _ ->
                try {
                    val stringValue = amountEditText.text.toString()
                    if (stringValue.isNotBlank()) {
                        val newBalance = java.lang.Double.valueOf(stringValue)
                        viewModel.onNewBalanceSelected(
                            newBalance,
                            getString(R.string.adjust_balance_expense_title)
                        )
                    }
                } catch (e: Exception) {
                    Logger.error("Error parsing new balance", e)
                }

                dialog.dismiss()
            }

            val dialog = builder.show()

            // Directly show keyboard when the dialog pops
            amountEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                // Check if the device doesn't have a physical keyboard
                if (hasFocus && resources.configuration.keyboard == Configuration.KEYBOARD_NOKEYS) {
                    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                }
            }
        }

        viewModel.currentBalanceEditingErrorEventStream.observe(this) { exception ->
            Logger.error("Error while adjusting balance", exception)

            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.adjust_balance_error_title)
                .setMessage(R.string.adjust_balance_error_message)
                .setNegativeButton(R.string.ok) { dialog1, _ -> dialog1.dismiss() }
                .show()
        }

        viewModel.currentBalanceEditedEventStream.observe(this) { (expense, diff, newBalance) ->
            //Show snackbar
            val snackbar = Snackbar.make(
                binding.coordinatorLayout,
                resources.getString(
                    R.string.adjust_balance_snackbar_text,
                    CurrencyHelper.getFormattedCurrencyString(parameters, newBalance)
                ),
                Snackbar.LENGTH_LONG
            )
            snackbar.setAction(R.string.undo) {
                viewModel.onCurrentBalanceEditedCancelled(expense, diff)
            }
            snackbar.setActionTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.snackbar_action_undo
                )
            )

            snackbar.duration = BaseTransientBottomBar.LENGTH_LONG
            snackbar.show()
        }

        viewModel.currentBalanceRestoringEventStream.observe(this) {
            // Nothing to do
        }

        viewModel.currentBalanceRestoringErrorEventStream.observe(this) { exception ->
            Logger.error("An error occurred during balance", exception)

            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.adjust_balance_error_title)
                .setMessage(R.string.adjust_balance_error_message)
                .setNegativeButton(R.string.ok) { dialog1, _ -> dialog1.dismiss() }
                .show()
        }

        viewModel.premiumStatusLiveData.observe(this) { isPremium ->
            isUserPremium = isPremium
            invalidateOptionsMenu()
        }

        viewModel.selectedDateChangeLiveData.observe(this) { (date, balance, maybeCheckedBalance, expenses) ->
            refreshAllForDate(date, balance, maybeCheckedBalance, expenses)
        }

        viewModel.expenseCheckedErrorEventStream.observe(this) { exception ->
            Logger.error("Error while checking expense", exception)

            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.expense_check_error_title)
                .setMessage(
                    getString(
                        R.string.expense_check_error_message,
                        exception.localizedMessage
                    )
                )
                .setNegativeButton(R.string.ok) { dialog2, _ -> dialog2.dismiss() }
                .show()
        }

        viewModel.showGoToCurrentMonthButtonLiveData.observe(this) {
            invalidateOptionsMenu()
        }

        viewModel.goBackToCurrentMonthEventStream.observe(this) {
            calendarFragment.goToCurrentMonth()
        }

        viewModel.confirmCheckAllPastEntriesEventStream.observe(this) {
            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.check_all_past_expences_title)
                .setMessage(getString(R.string.check_all_past_expences_message))
                .setPositiveButton(R.string.check_all_past_expences_confirm_cta) { dialog2, _ ->
                    viewModel.onCheckAllPastEntriesConfirmPressed()
                    dialog2.dismiss()
                }
                .setNegativeButton(android.R.string.cancel) { dialog2, _ -> dialog2.dismiss() }
                .show()
        }

        viewModel.checkAllPastEntriesErrorEventStream.observe(this) { error ->
            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.check_all_past_expences_error_title)
                .setMessage(
                    getString(
                        R.string.check_all_past_expences_error_message,
                        error.localizedMessage
                    )
                )
                .setNegativeButton(R.string.ok) { dialog2, _ -> dialog2.dismiss() }
                .show()
        }
    }

    override fun onStart() {
        super.onStart()

        // If the last stop happened yesterday (or another day), set and refresh to the current date
        if (lastStopDate != null) {
            val cal = Calendar.getInstance()
            val currentDay = cal.get(Calendar.DAY_OF_YEAR)

            cal.time = lastStopDate!!
            val lastStopDay = cal.get(Calendar.DAY_OF_YEAR)

            if (currentDay != lastStopDay) {
                viewModel.onDayChanged()
            }

            lastStopDate = null
        }
    }

    override fun onStop() {
        lastStopDate = Date()

        super.onStop()
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(receiver)

        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        calendarFragment.saveStatesToKey(outState, CALENDAR_SAVED_STATE)

        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_EXPENSE_ACTIVITY_CODE || requestCode == MANAGE_RECURRING_EXPENSE_ACTIVITY_CODE) {
            if (resultCode == RESULT_OK) {
                viewModel.onExpenseAdded()
            }
        } else if (requestCode == WELCOME_SCREEN_ACTIVITY_CODE) {
            if (resultCode == RESULT_OK) {
                viewModel.onWelcomeScreenFinished()
            } else if (resultCode == RESULT_CANCELED) {
                finish() // Finish activity if welcome screen is finish via back button
            }
        } else if (requestCode == SETTINGS_SCREEN_ACTIVITY_CODE) {
            calendarFragment.setFirstDayOfWeek(parameters.getCaldroidFirstDayOfWeek())
        }
    }

    override fun onBackPressed() {
        if ( menu.isExpanded) {
            menu.collapse()
        } else {
            super.onBackPressed()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent == null) {
            return
        }

        openSettingsIfNeeded(intent)
        openMonthlyReportIfNeeded(intent)
        openPremiumIfNeeded(intent)
        openAddExpenseIfNeeded(intent)
        openAddRecurringExpenseIfNeeded(intent)
        openSettingsForBackupIfNeeded(intent)
    }

// ------------------------------------------>

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        // Remove monthly report for non premium users
        if ( !isUserPremium ) {
            menu.removeItem(R.id.action_monthly_report)
            menu.removeItem(R.id.action_check_all_past_entries)
        } else if ( !parameters.hasUserSawMonthlyReportHint() ) {
            binding.monthlyReportHint.visibility = View.VISIBLE

            binding.monthlyReportHintButton.setOnClickListener {
                binding.monthlyReportHint.visibility = View.GONE
                parameters.setUserSawMonthlyReportHint()
            }
        }

        // Remove back to today button if needed
        if ( viewModel.showGoToCurrentMonthButtonLiveData.value != true ) {
            menu.removeItem(R.id.action_go_to_current_month)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                val startIntent = Intent(this, SettingsActivity::class.java)
                ActivityCompat.startActivityForResult(this@MainActivity, startIntent, SETTINGS_SCREEN_ACTIVITY_CODE, null)

                return true
            }
            R.id.action_balance -> {
                viewModel.onAdjustCurrentBalanceClicked()

                return true
            }
            R.id.action_monthly_report -> {
                val startIntent = Intent(this, MonthlyReportBaseActivity::class.java)
                ActivityCompat.startActivity(this@MainActivity, startIntent, null)

                return true
            }
            R.id.action_go_to_current_month -> {
                viewModel.onGoBackToCurrentMonthButtonPressed()

                return true
            }
            R.id.action_check_all_past_entries -> {
                viewModel.onCheckAllPastEntriesPressed()

                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

// ------------------------------------------>

    /**
     * Update the balance for the given day
     * TODO optimization
     */
    private fun updateBalanceDisplayForDay(day: Date, balance: Double, maybeCheckedBalance: Double?) {
        val format = SimpleDateFormat(resources.getString(R.string.account_balance_date_format), Locale.getDefault())

        var formatted = resources.getString(R.string.account_balance_format, format.format(day))

        // FIXME it's ugly!!
        if (formatted.endsWith(".:")) {
            formatted = formatted.substring(0, formatted.length - 2) + ":" // Remove . at the end of the month (ex: nov.: -> nov:)
        } else if (formatted.endsWith(". :")) {
            formatted = formatted.substring(0, formatted.length - 3) + " :" // Remove . at the end of the month (ex: nov. : -> nov :)
        }

        binding.budgetLine.text = formatted
        binding.budgetLineAmount.text = if (maybeCheckedBalance != null ) {
            resources.getString(
                R.string.account_balance_checked_format,
                CurrencyHelper.getFormattedCurrencyString(parameters, balance),
                CurrencyHelper.getFormattedCurrencyString(parameters, maybeCheckedBalance),
            )
        } else {
            CurrencyHelper.getFormattedCurrencyString(parameters, balance)
        }

        binding.budgetLineAmount.setTextColor(ContextCompat.getColor(this, when {
            balance <= 0 -> R.color.budget_red
            balance < parameters.getLowMoneyWarningAmount() -> R.color.budget_orange
            else -> R.color.budget_green
        }))
    }

    /**
     * Open the settings activity if the given intent contains the [.INTENT_REDIRECT_TO_SETTINGS_EXTRA]
     * extra.
     */
    private fun openSettingsIfNeeded(intent: Intent) {
        if (intent.getBooleanExtra(INTENT_REDIRECT_TO_SETTINGS_EXTRA, false)) {
            val startIntent = Intent(this, SettingsActivity::class.java)
            ActivityCompat.startActivityForResult(this@MainActivity, startIntent, SETTINGS_SCREEN_ACTIVITY_CODE, null)
        }
    }

    /**
     * Open the settings activity to display backup options if the given intent contains the
     * [.INTENT_REDIRECT_TO_SETTINGS_FOR_BACKUP_EXTRA] extra.
     */
    private fun openSettingsForBackupIfNeeded(intent: Intent) {
        if( intent.getBooleanExtra(INTENT_REDIRECT_TO_SETTINGS_FOR_BACKUP_EXTRA, false) ) {
            val startIntent = Intent(this, SettingsActivity::class.java).apply {
                putExtra(SHOW_BACKUP_INTENT_KEY, true)
            }
            ActivityCompat.startActivityForResult(this@MainActivity, startIntent, SETTINGS_SCREEN_ACTIVITY_CODE, null)
        }
    }

    /**
     * Open the monthly report activity if the given intent contains the monthly uri part.
     *
     * @param intent
     */
    private fun openMonthlyReportIfNeeded(intent: Intent) {
        try {
            val data = intent.data
            if (data != null && "true" == data.getQueryParameter("monthly")) {
                val startIntent = Intent(this, MonthlyReportBaseActivity::class.java)
                startIntent.putExtra(MonthlyReportBaseActivity.FROM_NOTIFICATION_EXTRA, true)
                ActivityCompat.startActivity(this@MainActivity, startIntent, null)
            }
        } catch (e: Exception) {
            Logger.error("Error while opening report activity", e)
        }

    }

    /**
     * Open the premium screen if the given intent contains the [.INTENT_REDIRECT_TO_PREMIUM_EXTRA]
     * extra.
     *
     * @param intent
     */
    private fun openPremiumIfNeeded(intent: Intent) {
        if (intent.getBooleanExtra(INTENT_REDIRECT_TO_PREMIUM_EXTRA, false)) {
            val startIntent = Intent(this, SettingsActivity::class.java)
            startIntent.putExtra(SettingsActivity.SHOW_PREMIUM_INTENT_KEY, true)

            ActivityCompat.startActivityForResult(this, startIntent, SETTINGS_SCREEN_ACTIVITY_CODE, null)
        }
    }

    /**
     * Open the add expense screen if the given intent contains the [.INTENT_SHOW_ADD_EXPENSE]
     * extra.
     *
     * @param intent
     */
    private fun openAddExpenseIfNeeded(intent: Intent) {
        if (intent.getBooleanExtra(INTENT_SHOW_ADD_EXPENSE, false)) {
            val startIntent = Intent(this, ExpenseEditActivity::class.java)
            startIntent.putExtra("date", Date().time)

            ActivityCompat.startActivityForResult(this, startIntent, ADD_EXPENSE_ACTIVITY_CODE, null)
        }
    }

    /**
     * Open the add recurring expense screen if the given intent contains the [.INTENT_SHOW_ADD_RECURRING_EXPENSE]
     * extra.
     *
     * @param intent
     */
    private fun openAddRecurringExpenseIfNeeded(intent: Intent) {
        if (intent.getBooleanExtra(INTENT_SHOW_ADD_RECURRING_EXPENSE, false)) {
            val startIntent = Intent(this, RecurringExpenseEditActivity::class.java)
            startIntent.putExtra("dateStart", Date().time)

            ActivityCompat.startActivityForResult(this, startIntent, ADD_EXPENSE_ACTIVITY_CODE, null)
        }
    }

// ------------------------------------------>

    private fun initCalendarFragment(savedInstanceState: Bundle?) {
        calendarFragment = CalendarFragment()

        if (savedInstanceState != null && savedInstanceState.containsKey(CALENDAR_SAVED_STATE) ) {
            calendarFragment.restoreStatesFromKey(savedInstanceState, CALENDAR_SAVED_STATE)
        } else {
            val args = Bundle()
            val cal = Calendar.getInstance()
            args.putInt(CaldroidFragment.MONTH, cal.get(Calendar.MONTH) + 1)
            args.putInt(CaldroidFragment.YEAR, cal.get(Calendar.YEAR))
            args.putBoolean(CaldroidFragment.ENABLE_SWIPE, true)
            args.putBoolean(CaldroidFragment.SIX_WEEKS_IN_CALENDAR, false)
            args.putInt(CaldroidFragment.START_DAY_OF_WEEK, parameters.getCaldroidFirstDayOfWeek())
            args.putBoolean(CaldroidFragment.ENABLE_CLICK_ON_DISABLED_DATES, false)
            args.putInt(CaldroidFragment.THEME_RESOURCE, R.style.caldroid_style)

            calendarFragment.arguments = args
            calendarFragment.setMinDate(Date(parameters.getInitTimestamp()).computeCalendarMinDateFromInitDate())
        }

        val listener = object : CaldroidListener() {
            override fun onSelectDate(date: Date, view: View) {
                viewModel.onSelectDate(date)
            }

            override fun onLongClickDate(date: Date, view: View?) // Add expense on long press
            {
                val startIntent = Intent(this@MainActivity, ExpenseEditActivity::class.java)
                startIntent.putExtra("date", date.time)

                // Get the absolute location on window for Y value
                val viewLocation = IntArray(2)
                view!!.getLocationInWindow(viewLocation)

                startIntent.putExtra(ANIMATE_TRANSITION_KEY, true)
                startIntent.putExtra(CENTER_X_KEY, view.x.toInt() + view.width / 2)
                startIntent.putExtra(CENTER_Y_KEY, viewLocation[1] + view.height / 2)

                ActivityCompat.startActivityForResult(this@MainActivity, startIntent, ADD_EXPENSE_ACTIVITY_CODE, null)
            }

            override fun onChangeMonth(month: Int, year: Int) {
                viewModel.onMonthChanged(month - 1)
            }

            override fun onCaldroidViewCreated() {
                val viewPager = calendarFragment.dateViewPager
                val leftButton = calendarFragment.leftArrowButton
                val rightButton = calendarFragment.rightArrowButton
                val textView = calendarFragment.monthTitleTextView
                val weekDayGreedView = calendarFragment.weekdayGridView
                val topLayout = this@MainActivity.findViewById<LinearLayout>(com.caldroid.R.id.calendar_title_view)

                val params = textView.layoutParams as LinearLayout.LayoutParams
                params.gravity = Gravity.TOP
                params.setMargins(0, 0, 0, this@MainActivity.resources.getDimensionPixelSize(R.dimen.calendar_month_text_padding_bottom))
                textView.layoutParams = params

                topLayout.setPadding(0, this@MainActivity.resources.getDimensionPixelSize(R.dimen.calendar_month_padding_top), 0, this@MainActivity.resources.getDimensionPixelSize(R.dimen.calendar_month_padding_bottom))

                val leftButtonParams = leftButton.layoutParams as LinearLayout.LayoutParams
                leftButtonParams.setMargins(this@MainActivity.resources.getDimensionPixelSize(R.dimen.calendar_month_buttons_margin), 0, 0, 0)
                leftButton.layoutParams = leftButtonParams

                val rightButtonParams = rightButton.layoutParams as LinearLayout.LayoutParams
                rightButtonParams.setMargins(0, 0, this@MainActivity.resources.getDimensionPixelSize(R.dimen.calendar_month_buttons_margin), 0)
                rightButton.layoutParams = rightButtonParams

                textView.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.calendar_header_month_color))
                topLayout.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.calendar_header_background))

                leftButton.text = "<"
                leftButton.textSize = 25f
                leftButton.gravity = Gravity.CENTER
                leftButton.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.calendar_month_button_color))
                leftButton.setBackgroundResource(R.drawable.calendar_month_switcher_button_drawable)

                rightButton.text = ">"
                rightButton.textSize = 25f
                rightButton.gravity = Gravity.CENTER
                rightButton.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.calendar_month_button_color))
                rightButton.setBackgroundResource(R.drawable.calendar_month_switcher_button_drawable)

                weekDayGreedView.setPadding(0, this@MainActivity.resources.getDimensionPixelSize(R.dimen.calendar_weekdays_padding_top), 0, this@MainActivity.resources.getDimensionPixelSize(R.dimen.calendar_weekdays_padding_bottom))

                viewPager.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.calendar_background))
                (viewPager.parent as View?)?.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.calendar_background))

                // Remove border
                leftButton.removeButtonBorder()
                rightButton.removeButtonBorder()
            }
        }

        calendarFragment.caldroidListener = listener

        val t = supportFragmentManager.beginTransaction()
        t.replace(R.id.calendarView, calendarFragment)
        t.commit()
    }

    private fun initRecyclerView() {
        /*
         * FAB
         */
        menu = findViewById(R.id.fab_choices)

        val background = this@MainActivity.findViewById<View>(R.id.fab_choices_background)
        val backgroundAlpha = 0.8f
        val backgroundAnimationDuration: Long = 200

        background.setOnClickListener { menu.collapse() }

        menu.setOnFloatingActionsMenuUpdateListener(object : FloatingActionsMenu.OnFloatingActionsMenuUpdateListener {
            override fun onMenuExpanded() {
                val fadeInAnimation = AlphaAnimation(0.0f, backgroundAlpha)
                fadeInAnimation.duration = backgroundAnimationDuration
                fadeInAnimation.fillAfter = true
                fadeInAnimation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {
                        background.visibility = View.VISIBLE
                        background.isClickable = true
                    }

                    override fun onAnimationEnd(animation: Animation) {

                    }

                    override fun onAnimationRepeat(animation: Animation) {

                    }
                })

                background.startAnimation(fadeInAnimation)
            }

            override fun onMenuCollapsed() {
                val fadeOutAnimation = AlphaAnimation(backgroundAlpha, 0.0f)
                fadeOutAnimation.duration = backgroundAnimationDuration
                fadeOutAnimation.fillAfter = true
                fadeOutAnimation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {

                    }

                    override fun onAnimationEnd(animation: Animation) {
                        background.visibility = View.GONE
                        background.isClickable = false
                    }

                    override fun onAnimationRepeat(animation: Animation) {

                    }
                })

                background.startAnimation(fadeOutAnimation)
            }
        })

        val fabNewExpense = findViewById<FloatingActionButton>(R.id.fab_new_expense)
        fabNewExpense.setOnClickListener {
            val startIntent = Intent(this@MainActivity, ExpenseEditActivity::class.java)
            startIntent.putExtra("date", calendarFragment.getSelectedDate().time)

            startIntent.putExtra(ANIMATE_TRANSITION_KEY, true)
            startIntent.putExtra(CENTER_X_KEY, menu.x.toInt() + (menu.width.toFloat() / 1.2f).toInt())
            startIntent.putExtra(CENTER_Y_KEY, menu.y.toInt() + (menu.height.toFloat() / 1.2f).toInt())

            ActivityCompat.startActivityForResult(this@MainActivity, startIntent, ADD_EXPENSE_ACTIVITY_CODE, null)

            menu.collapse()
        }

        val fabNewRecurringExpense = findViewById<FloatingActionButton>(R.id.fab_new_recurring_expense)
        fabNewRecurringExpense.setOnClickListener {
            val startIntent = Intent(this@MainActivity, RecurringExpenseEditActivity::class.java)
            startIntent.putExtra("dateStart", calendarFragment.getSelectedDate().time)

            startIntent.putExtra(ANIMATE_TRANSITION_KEY, true)
            startIntent.putExtra(CENTER_X_KEY, menu.x.toInt() + (menu.width.toFloat() / 1.2f).toInt())
            startIntent.putExtra(CENTER_Y_KEY, menu.y.toInt() + (menu.height.toFloat() / 1.2f).toInt())

            ActivityCompat.startActivityForResult(this@MainActivity, startIntent, ADD_EXPENSE_ACTIVITY_CODE, null)

            menu.collapse()
        }

        /*
         * Expense Recycler view
         */
        binding.expensesRecyclerView.layoutManager = LinearLayoutManager(this)

        expensesViewAdapter = ExpensesRecyclerViewAdapter(this, parameters, iab, Date()) { expense, checked ->
            viewModel.onExpenseChecked(expense, checked)
        }
        binding.expensesRecyclerView.adapter = expensesViewAdapter
    }

    private fun refreshRecyclerViewForDate(date: Date, expenses: List<Expense>) {
        expensesViewAdapter.setDate(date, expenses)

        if ( expenses.isNotEmpty() ) {
            binding.expensesRecyclerView.visibility = View.VISIBLE
            binding.emptyExpensesRecyclerViewPlaceholder.visibility = View.GONE
        } else {
            binding.expensesRecyclerView.visibility = View.GONE
            binding.emptyExpensesRecyclerViewPlaceholder.visibility = View.VISIBLE
        }
    }

    private fun refreshAllForDate(date: Date, balance: Double, maybeCheckedBalance: Double?, expenses: List<Expense>) {
        refreshRecyclerViewForDate(date, expenses)
        updateBalanceDisplayForDay(date, balance, maybeCheckedBalance)
        calendarFragment.setSelectedDates(date, date)
        calendarFragment.refreshView()
    }

    /**
     * Show a generic alert dialog telling the user an error occured while deleting recurring expense
     */
    private fun showGenericRecurringDeleteErrorDialog() {
        AlertDialog.Builder(this@MainActivity)
            .setTitle(R.string.recurring_expense_delete_error_title)
            .setMessage(R.string.recurring_expense_delete_error_message)
            .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    companion object {
        const val ADD_EXPENSE_ACTIVITY_CODE = 101
        const val MANAGE_RECURRING_EXPENSE_ACTIVITY_CODE = 102
        const val WELCOME_SCREEN_ACTIVITY_CODE = 103
        const val SETTINGS_SCREEN_ACTIVITY_CODE = 104
        const val INTENT_EXPENSE_DELETED = "intent.expense.deleted"
        const val INTENT_RECURRING_EXPENSE_DELETED = "intent.expense.monthly.deleted"
        const val INTENT_SHOW_WELCOME_SCREEN = "intent.welcomscreen.show"
        const val INTENT_SHOW_ADD_EXPENSE = "intent.addexpense.show"
        const val INTENT_SHOW_ADD_RECURRING_EXPENSE = "intent.addrecurringexpense.show"
        const val INTENT_SHOW_CHECKED_BALANCE_CHANGED = "intent.showcheckedbalance.changed"

        const val INTENT_REDIRECT_TO_PREMIUM_EXTRA = "intent.extra.premiumshow"
        const val INTENT_REDIRECT_TO_SETTINGS_EXTRA = "intent.extra.redirecttosettings"
        const val INTENT_REDIRECT_TO_SETTINGS_FOR_BACKUP_EXTRA = "intent.extra.redirecttosettingsforbackup"

        const val ANIMATE_TRANSITION_KEY = "animate"
        const val CENTER_X_KEY = "centerX"
        const val CENTER_Y_KEY = "centerY"

        private const val CALENDAR_SAVED_STATE = "calendar_saved_state"
    }
}