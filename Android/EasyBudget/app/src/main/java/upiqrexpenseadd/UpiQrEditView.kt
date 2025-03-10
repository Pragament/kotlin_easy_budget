package upiqrexpenseadd

import android.app.Activity
import android.content.ContextWrapper
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.AppWithTopAppBarScaffold
import com.benoitletondor.easybudgetapp.compose.BackButtonBehavior
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.helper.sanitizeFromUnsupportedInputForDecimals
import com.benoitletondor.easybudgetapp.helper.serialization.SerializedExpense
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.view.expenseedit.ExpenseEditViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.shreyaspatil.easyupipayment.EasyUpiPayment
import dev.shreyaspatil.easyupipayment.listener.PaymentStatusListener
import dev.shreyaspatil.easyupipayment.model.PaymentApp
import dev.shreyaspatil.easyupipayment.model.TransactionDetails
import dev.shreyaspatil.easyupipayment.model.TransactionStatus
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.Serializable
import java.net.URLDecoder
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Date
import java.util.Locale

@Serializable
data class UpiQrExpenseAddDestination(val dateEpochDay: Long) {
    constructor(
        date: LocalDate,
    ) : this(date.toEpochDay())
}

@Serializable
data class UpiQrExpenseEditDestination(val dateEpochDay: Long, val editedExpense: SerializedExpense) {
    constructor(
        date: LocalDate,
        editedExpense: Expense,
    ) : this(date.toEpochDay(), SerializedExpense(editedExpense))
}

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun UpiQrEditView(
    viewModel: UpiQrEditViewModel,
    navigateUp: () -> Unit,
    finish: () -> Unit,
) {
    UpiQrEditView(
        stateFlow = viewModel.stateFlow,
        eventFlow = viewModel.eventFlow,
        userCurrencyFlow = viewModel.userCurrencyFlow,
        navigateUp = navigateUp,
        finish = finish,
        onTitleUpdate = viewModel::onTitleChanged,
        onAmountUpdate = viewModel::onAmountChanged,
        onSaveButtonClicked = viewModel::onSave,
        onIsRevenueChanged = viewModel::onExpenseRevenueValueChanged,
        onDateClicked = viewModel::onDateClicked,
        onDateSelected = viewModel::onDateSelected,
        onAddExpenseBeforeInitDateConfirmed = viewModel::onAddExpenseBeforeInitDateConfirmed,
        onAddExpenseBeforeInitDateCancelled = viewModel::onAddExpenseBeforeInitDateCancelled,
    )
}

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpiQrEditView(
    stateFlow: StateFlow<UpiQrEditViewModel.State>,
    eventFlow: Flow<UpiQrEditViewModel.Event>,
    userCurrencyFlow: StateFlow<Currency>,
    navigateUp: () -> Unit,
    finish: () -> Unit,
    onTitleUpdate: (String) -> Unit,
    onAmountUpdate: (String) -> Unit,
    onSaveButtonClicked: () -> Unit,
    onIsRevenueChanged: (Boolean) -> Unit,
    onDateClicked: () -> Unit,
    onDateSelected: (Long?) -> Unit,
    onAddExpenseBeforeInitDateConfirmed: () -> Unit,
    onAddExpenseBeforeInitDateCancelled: () -> Unit,
) {
    val context = LocalContext.current
    var showDatePickerWithDate by remember { mutableStateOf<LocalDate?>(null) }
    var amountValueError: String? by remember { mutableStateOf(null) }
    var titleValueError: String? by remember { mutableStateOf(null) }
    var descriptionTextFieldState by rememberSaveable { mutableStateOf("") }
    var currentAmountTextFieldState by rememberSaveable { mutableStateOf("") }
    var upiUrl by rememberSaveable { mutableStateOf("") }
    val activity1 = getActivity()

    val scanQRCodeLauncher = rememberLauncherForActivityResult(contract = ScanQRCode(), onResult = { result ->
        when (result) {
            is QRResult.QRSuccess -> {
                val scannedContent = result.content.rawValue
                if (scannedContent != null && scannedContent.startsWith("upi://")) {
                    // Extract merchant name and UPI URL from the QR code
                    val merchantName = extractMerchantName(scannedContent)
                    val amount = extractAmount(scannedContent)
                    descriptionTextFieldState = merchantName
                    currentAmountTextFieldState = amount
                    upiUrl = scannedContent
                    Log.d("UPI QR Scan", "Scanned UPI URL: $upiUrl")
                } else {
                    Toast.makeText(context, "Not a UPI QR code: $scannedContent", Toast.LENGTH_LONG).show()
                }
            }
            is QRResult.QRUserCanceled -> {
                Toast.makeText(context, "Cancelled", Toast.LENGTH_LONG).show()
            }
            is QRResult.QRMissingPermission -> {
                Toast.makeText(context, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show()
            }
            is QRResult.QRError -> {
                Toast.makeText(context, "Error scanning QR code: ${result.exception.message}", Toast.LENGTH_LONG).show()
            }
        }
    })

    // Launch the QR code scanner when the screen is first opened
    LaunchedEffect(Unit) {
        scanQRCodeLauncher.launch(null)
    }

    LaunchedEffect(key1 = "eventsListener") {
        launchCollect(eventFlow) { event ->
            when (event) {
                UpiQrEditViewModel.Event.ExpenseAddBeforeInitDateError -> MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.expense_add_before_init_date_dialog_title)
                    .setMessage(R.string.expense_add_before_init_date_dialog_description)
                    .setPositiveButton(R.string.expense_add_before_init_date_dialog_positive_cta) { _, _ ->
                        onAddExpenseBeforeInitDateConfirmed()
                    }
                    .setNegativeButton(R.string.expense_add_before_init_date_dialog_negative_cta) { _, _ ->
                        onAddExpenseBeforeInitDateCancelled()
                    }
                    .show()
                UpiQrEditViewModel.Event.Finish -> finish()
                UpiQrEditViewModel.Event.UnableToLoadDB -> MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.expense_edit_unable_to_load_db_error_title)
                    .setMessage(R.string.expense_edit_unable_to_load_db_error_message)
                    .setPositiveButton(R.string.expense_edit_unable_to_load_db_error_cta) { _, _ ->
                        finish()
                    }
                    .setCancelable(false)
                    .show()
                is UpiQrEditViewModel.Event.ErrorPersistingExpense -> MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.expense_edit_error_saving_title)
                    .setMessage(R.string.expense_edit_error_saving_message)
                    .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                    .show()
                UpiQrEditViewModel.Event.EmptyTitleError -> titleValueError = context.getString(R.string.no_description_error)
                is UpiQrEditViewModel.Event.ShowDatePicker -> showDatePickerWithDate = event.date
                UpiQrEditViewModel.Event.EmptyAmountError -> amountValueError = context.getString(R.string.no_amount_error)
            }
        }
    }

    val state by stateFlow.collectAsState()

    AppWithTopAppBarScaffold(
        title = stringResource(if (state.isEditing) {
            if (state.isRevenue) { R.string.title_activity_edit_income } else { R.string.title_activity_edit_expense }
        } else {
            if (state.isRevenue) { R.string.title_activity_add_income } else { R.string.title_activity_add_expense }
        }),
        backButtonBehavior = BackButtonBehavior.NavigateBack(
            onBackButtonPressed = navigateUp,
        ),
        content = { contentPadding ->
            val titleFocusRequester = remember { FocusRequester() }
            val amountFocusRequester = remember { FocusRequester() }
            LaunchedEffect(key1 = "focusRequester") {
                titleFocusRequester.requestFocus()
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = colorResource(R.color.action_bar_background))
                            .padding(horizontal = 26.dp)
                            .padding(top = 10.dp, bottom = 20.dp),
                    ) {
                        LaunchedEffect(key1 = "descriptionTextFieldStateWatcher") {
                            snapshotFlow { descriptionTextFieldState }
                                .collectLatest { text ->
                                    titleValueError = null
                                    onTitleUpdate(text)
                                }
                        }

                        TextField(
                            value = descriptionTextFieldState,
                            onValueChange = {
                                descriptionTextFieldState = it
                                titleValueError = null
                                onTitleUpdate(it)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(titleFocusRequester),
                            label = { Text(if (titleValueError != null) "${stringResource(R.string.description)}: $titleValueError" else stringResource(R.string.description)) },
                            isError = titleValueError != null,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next,
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.Sentences,
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val currency by userCurrencyFlow.collectAsState()

                        LaunchedEffect(key1 = "currentAmountTextFieldStateWatcher") {
                            snapshotFlow { currentAmountTextFieldState }
                                .collectLatest { text ->
                                    val newText = text.sanitizeFromUnsupportedInputForDecimals(supportsNegativeValue = false)

                                    if (newText != text) {
                                        currentAmountTextFieldState = newText
                                    }

                                    amountValueError = null
                                    onAmountUpdate(newText)
                                }
                        }

                        TextField(
                            value = currentAmountTextFieldState,
                            onValueChange = {
                                val newText = it.sanitizeFromUnsupportedInputForDecimals(supportsNegativeValue = false)
                                currentAmountTextFieldState = newText
                                amountValueError = null
                                onAmountUpdate(newText)
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .focusRequester(amountFocusRequester),
                            label = { Text(if (amountValueError != null) "${stringResource(R.string.amount, currency.symbol)}: $amountValueError" else stringResource(R.string.amount, currency.symbol)) },
                            isError = amountValueError != null,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    FloatingActionButton(
                        modifier = Modifier
                            .align(Alignment.End)
                            .offset(y = (-30).dp, x = (-26).dp),
                        onClick = {
                            onSaveButtonClicked()
                            val amount = currentAmountTextFieldState.toDoubleOrNull()
                            val description = descriptionTextFieldState
                            val payeeUpiId = extractUpiId(upiUrl)

                            if (amount != null && description.isNotEmpty() && activity1 != null) {
                                Log.d("UPI QR Scan", "Using UPI URL: $upiUrl for payment")
                                launchUPIUrl(activity1, payeeUpiId, amount, description)
                            }
                        },
                        containerColor = colorResource(R.color.secondary),
                        contentColor = colorResource(R.color.white),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_save_white_24dp),
                            contentDescription = stringResource(R.string.fab_add_expense),
                        )
                    }

                    Row(
                        modifier = Modifier
                            .offset(y = (-20).dp)
                            .fillMaxWidth()
                            .padding(horizontal = 26.dp),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = stringResource(R.string.type),
                                color = colorResource(R.color.expense_edit_title_text_color),
                                fontSize = 14.sp,
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Switch(
                                    checked = false,
                                    onCheckedChange = { },
                                    enabled = false,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = colorResource(R.color.add_expense_expense_thumb_background_color),
                                        checkedThumbColor = colorResource(R.color.budget_green),
                                        uncheckedThumbColor = colorResource(R.color.budget_red),
                                        uncheckedTrackColor = colorResource(R.color.add_expense_expense_thumb_background_color),
                                        uncheckedBorderColor = Color.Transparent,
                                        checkedBorderColor = Color.Transparent,
                                    ),
                                    thumbContent = {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                        )
                                    }
                                )

                                Spacer(modifier = Modifier.width(5.dp))

                                Text(
                                    text = stringResource(R.string.payment),
                                    color = colorResource(R.color.budget_red),
                                    fontSize = 14.sp,
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = stringResource(R.string.date),
                                color = colorResource(R.color.expense_edit_title_text_color),
                                fontSize = 14.sp,
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            val dateFormatter = remember {
                                DateTimeFormatter.ofPattern(context.getString(R.string.add_expense_date_format), Locale.getDefault())
                            }
                            val dateString = remember(state.expense.date) {
                                dateFormatter.format(state.expense.date)
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onDateClicked)
                                    .padding(top = 3.dp),
                            ) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = dateString,
                                    textAlign = TextAlign.Center,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = colorResource(R.color.expense_edit_field_accent_color),
                                    thickness = 1.dp,
                                )
                            }
                        }
                    }
                }
            }

            val datePickerDate = showDatePickerWithDate
            if (datePickerDate != null) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = Date.from(datePickerDate.atStartOfDay().atZone(ZoneId.of("UTC")).toInstant()).time
                )

                DatePickerDialog(
                    onDismissRequest = { showDatePickerWithDate = null },
                    confirmButton = {
                        Button(
                            modifier = Modifier.padding(end = 16.dp, bottom = 10.dp),
                            onClick = {
                                onDateSelected(datePickerState.selectedDateMillis)
                                showDatePickerWithDate = null
                            }
                        ) {
                            Text(text = stringResource(R.string.ok))
                        }
                    },
                    content = {
                        DatePicker(state = datePickerState)
                    },
                )
            }
        },
    )
}

@Composable
fun getActivity(): Activity? {
    val context = LocalContext.current
    return remember(context) {
        generateSequence(context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>()
            .firstOrNull()
    }
}

private fun extractMerchantName(upiUrl: String): String {
    val params = upiUrl.substringAfter("?").split("&")
    for (param in params) {
        val keyValue = param.split("=")
        if (keyValue.size == 2 && keyValue[0] == "pn") {
            return URLDecoder.decode(keyValue[1], "UTF-8")
        }
    }
    return "Merchant Name"
}

private fun extractAmount(upiUrl: String): String {
    val params = upiUrl.substringAfter("?").split("&")
    for (param in params) {
        val keyValue = param.split("=")
        if (keyValue.size == 2 && keyValue[0] == "am") {
            return URLDecoder.decode(keyValue[1], "UTF-8")
        }
    }
    return ""
}

fun launchUPIUrl(activity: Activity, upiId: String, amount: Double, payeeName: String) {
    try {
        val easyUpiPayment = EasyUpiPayment(activity) {
            this.payeeVpa = upiId
            this.payeeName = payeeName
            this.payeeMerchantCode = "12345"
            this.transactionId = System.currentTimeMillis().toString()
            this.transactionRefId = "TID${System.currentTimeMillis()}"
            this.amount = amount.toString()
            this.description = "Payment for $payeeName"
            paymentApp = PaymentApp.ALL
        }

        easyUpiPayment.setPaymentStatusListener(object : PaymentStatusListener {
            override fun onTransactionCompleted(transactionDetails: TransactionDetails) {
                val status = transactionDetails.transactionStatus
                when (status) {
                    TransactionStatus.SUCCESS -> {
                        Toast.makeText(activity, "Transaction Successful", Toast.LENGTH_SHORT)
                            .show()
                    }

                    TransactionStatus.FAILURE -> {
                        Toast.makeText(activity, "Transaction Failed", Toast.LENGTH_SHORT).show()
                    }

                    TransactionStatus.SUBMITTED -> {
                        Toast.makeText(activity, "Transaction Pending", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onTransactionCancelled() {
                Toast.makeText(activity, "Transaction Cancelled", Toast.LENGTH_SHORT).show()
            }
        })


        easyUpiPayment.startPayment()
    } catch (e: Exception) {
        Toast.makeText(activity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
    }

}
fun extractUpiId(upiUrl: String): String {
    val uri = Uri.parse(upiUrl)
    return uri.getQueryParameter("pa") ?: ""
}