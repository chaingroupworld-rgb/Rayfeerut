package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.calc.UserSettlementReport
import com.example.data.model.*
import com.example.ui.components.BannerAdWidget
import com.example.ui.viewmodel.RayfeerutViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessScreen(
    viewModel: RayfeerutViewModel,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0: Settlement, 1: Daily Meals, 2: Expenses & Deposits
    val sectionTitles = listOf("Settlement", "Daily Meals", "Ledger & Deposits")

    val context = LocalContext.current
    val messGroup by viewModel.messGroup.collectAsState()
    val members by viewModel.messMembers.collectAsState()
    val messReport by viewModel.messSummaryReport.collectAsState()

    var showExpenseDialog by remember { mutableStateOf(false) }
    var showDepositDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showSettleUpDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("mess_screen")
    ) {
        // Mess Group Header Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = messGroup?.groupName ?: "Pine Crest Hostel Flat 304",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manager: ${messGroup?.managerName ?: "Alex Chen"} • ${members.size} Roommates",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Invite Code Chip
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Mess Invite Code", messGroup?.inviteCode ?: "RAY304"))
                            viewModel.showNotification("Copied invite code: ${messGroup?.inviteCode ?: "RAY304"}")
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Code: ${messGroup?.inviteCode ?: "RAY304"}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }

        // Sub-tabs
        TabRow(
            selectedTabIndex = selectedSection,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            sectionTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSection == index,
                    onClick = { selectedSection = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedSection == index) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedSection) {
                0 -> SettlementSection(
                    viewModel = viewModel,
                    onSettleUpClick = { showSettleUpDialog = true },
                    onAddMemberClick = { showAddMemberDialog = true }
                )
                1 -> MealsSection(viewModel = viewModel)
                2 -> LedgerSection(
                    viewModel = viewModel,
                    onAddExpenseClick = { showExpenseDialog = true },
                    onAddDepositClick = { showDepositDialog = true }
                )
            }
        }

        // Secondary Banner Ad at bottom
        BannerAdWidget(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            adUnitTitle = "Hostel Groceries & Fast Delivery Deals"
        )
    }

    if (showExpenseDialog) {
        AddExpenseDialog(
            members = members,
            onDismiss = { showExpenseDialog = false },
            onAdd = { amount, desc, category, splitType, paidById, paidByName ->
                viewModel.addMessExpense(amount, desc, category, splitType, paidById, paidByName)
                showExpenseDialog = false
            }
        )
    }

    if (showDepositDialog) {
        AddDepositDialog(
            members = members,
            onDismiss = { showDepositDialog = false },
            onAdd = { memberId, name, amount ->
                viewModel.addMemberDeposit(memberId, name, amount)
                showDepositDialog = false
            }
        )
    }

    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onAdd = { name, room ->
                viewModel.addMessMember(name, room)
                showAddMemberDialog = false
            }
        )
    }

    if (showSettleUpDialog) {
        val userReport = messReport.memberReports.firstOrNull { it.isCurrentUser }
        SettleUpDialog(
            report = userReport,
            onDismiss = { showSettleUpDialog = false }
        )
    }
}

// -------------------------------------------------------------
// 1. SETTLEMENT DASHBOARD
// -------------------------------------------------------------
@Composable
fun SettlementSection(
    viewModel: RayfeerutViewModel,
    onSettleUpClick: () -> Unit,
    onAddMemberClick: () -> Unit
) {
    val report by viewModel.messSummaryReport.collectAsState()
    val currentUserReport = report.memberReports.firstOrNull { it.isCurrentUser }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Prominent Balance Card (Green for surplus, Red for deficit)
        item {
            val balance = currentUserReport?.netBalance ?: 0.0
            val isSurplus = balance >= 0.0
            val accentColor = if (isSurplus) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_balance_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "YOUR MESS NET BALANCE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = accentColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isSurplus) "SURPLUS (+)" else "DEFICIT (-)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isSurplus) "+$${String.format("%.2f", balance)}" else "-$${String.format("%.2f", -balance)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )

                    Text(
                        text = if (isSurplus) {
                            "You have a credit surplus in the mess treasury. You are owed money from the manager."
                        } else {
                            "You have consumed more than your deposits/payments. Please settle up before month end."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onSettleUpClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settle_up_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSurplus) "View Transfer QR" else "Settle Up via Bank Transfer / QR")
                    }
                }
            }
        }

        // Mess Summary Metrics (Prompt 2 formulas)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Mess Financial Overview (Current Month)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricColumn(label = "Total Meals", value = "${report.totalMealsConsumed}")
                        MetricColumn(label = "Total Expenses", value = "$${String.format("%.2f", report.totalExpenses)}")
                        MetricColumn(label = "Meal Rate", value = "$${String.format("%.2f", report.currentMealRate)}/meal", highlight = true)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricColumn(label = "Food Cost (Proportional)", value = "$${String.format("%.2f", report.totalFoodExpenses)}")
                        MetricColumn(label = "Fixed Cost (Split Equal)", value = "$${String.format("%.2f", report.totalFixedExpenses)}")
                        MetricColumn(label = "Mess Cash on Hand", value = "$${String.format("%.2f", report.cashOnHand)}")
                    }
                }
            }
        }

        // Roommate Settlement Breakdown
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Roommate Settlement Ledger",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onAddMemberClick) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Roommate")
                }
            }
        }

        items(report.memberReports) { memberReport ->
            val isMemberSurplus = memberReport.netBalance >= 0.0
            val statusColor = if (isMemberSurplus) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = memberReport.memberName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isMemberSurplus) "+$${String.format("%.2f", memberReport.netBalance)}" else "-$${String.format("%.2f", -memberReport.netBalance)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                    Text(
                        text = "Total Meals: ${memberReport.totalMeals} • Food Cost: $${String.format("%.2f", memberReport.mealCost)} • Fixed: $${String.format("%.2f", memberReport.fixedCost)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Total Paid/Deposited: $${String.format("%.2f", memberReport.totalContributed)} | Individual Bill: $${String.format("%.2f", memberReport.totalBill)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MetricColumn(label: String, value: String, highlight: Boolean = false) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

// -------------------------------------------------------------
// 2. DAILY MEAL TRACKING SECTION
// -------------------------------------------------------------
@Composable
fun MealsSection(viewModel: RayfeerutViewModel) {
    val members by viewModel.messMembers.collectAsState()
    val mealLogs by viewModel.mealLogs.collectAsState()

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = remember { sdf.format(Date()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockClock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Column {
                        Text(
                            text = "Daily Cutoff Time: 10:00 PM",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Toggle meal counts (0, 0.5, 1.0) before 10 PM. Automatic midnight lock active.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Today's Meal Ledger ($today)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(members) { member ->
            val log = mealLogs.firstOrNull { it.memberId == member.id && it.dateString == today }
            val bFast = log?.breakfastCount ?: 1.0
            val lunch = log?.lunchCount ?: 1.0
            val dinner = log?.dinnerCount ?: 1.0

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = member.memberName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Total Today: ${bFast + lunch + dinner} meals",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Meal toggle buttons (Breakfast, Lunch, Dinner)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MealToggleButton(
                            label = "Breakfast",
                            currentVal = bFast,
                            onToggle = { nextVal ->
                                viewModel.logDailyMeal(member.id, member.memberName, nextVal, lunch, dinner, today)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        MealToggleButton(
                            label = "Lunch",
                            currentVal = lunch,
                            onToggle = { nextVal ->
                                viewModel.logDailyMeal(member.id, member.memberName, bFast, nextVal, dinner, today)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        MealToggleButton(
                            label = "Dinner",
                            currentVal = dinner,
                            onToggle = { nextVal ->
                                viewModel.logDailyMeal(member.id, member.memberName, bFast, lunch, nextVal, today)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MealToggleButton(
    label: String,
    currentVal: Double,
    onToggle: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val nextVal = when (currentVal) {
        1.0 -> 0.0
        0.0 -> 0.5
        else -> 1.0
    }
    Surface(
        color = if (currentVal > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle(nextVal) }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = if (currentVal == 0.0) "OFF (0)" else if (currentVal == 0.5) "HALF (0.5)" else "ON (1.0)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (currentVal > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// -------------------------------------------------------------
// 3. EXPENSES & DEPOSITS LEDGER
// -------------------------------------------------------------
@Composable
fun LedgerSection(
    viewModel: RayfeerutViewModel,
    onAddExpenseClick: () -> Unit,
    onAddDepositClick: () -> Unit
) {
    val expenses by viewModel.messExpenses.collectAsState()
    val deposits by viewModel.memberDeposits.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAddExpenseClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Expense")
                }
                FilledTonalButton(
                    onClick = onAddDepositClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Record Deposit")
                }
            }
        }

        item {
            Text(
                text = "Recent Mess Expenses",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(expenses) { exp ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = exp.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (exp.splitType == ExpenseSplitType.MEAL_PROPORTIONAL) "Meal Proportional" else "Equal Split",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = exp.description,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Paid by: ${exp.paidByName} • ${exp.expenseDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "$${String.format("%.2f", exp.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            Text(
                text = "Member Cash Deposits",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(deposits) { dep ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = dep.memberName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Deposited on ${dep.depositDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "+$${String.format("%.2f", dep.amount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOGS
// -------------------------------------------------------------
@Composable
fun AddExpenseDialog(
    members: List<MessMember>,
    onDismiss: () -> Unit,
    onAdd: (Double, String, String, ExpenseSplitType, Long, String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Groceries") }
    var splitType by remember { mutableStateOf(ExpenseSplitType.MEAL_PROPORTIONAL) }
    var selectedMember by remember { mutableStateOf(members.firstOrNull() ?: MessMember(id = 1, memberName = "You")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Mess Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount ($) *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (e.g. Weekly Vegetables) *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category:", style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Groceries", "Wifi", "Rent", "Maid").forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = {
                                category = c
                                splitType = if (c == "Groceries") ExpenseSplitType.MEAL_PROPORTIONAL else ExpenseSplitType.EQUAL_SPLIT
                            },
                            label = { Text(c, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text("Split Logic:", style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = splitType == ExpenseSplitType.MEAL_PROPORTIONAL,
                        onClick = { splitType = ExpenseSplitType.MEAL_PROPORTIONAL },
                        label = { Text("Meal Proportional", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = splitType == ExpenseSplitType.EQUAL_SPLIT,
                        onClick = { splitType = ExpenseSplitType.EQUAL_SPLIT },
                        label = { Text("Equal Split", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0 && desc.isNotBlank()) {
                        onAdd(amt, desc, category, splitType, selectedMember.id, selectedMember.memberName)
                    }
                },
                enabled = amountStr.isNotBlank() && desc.isNotBlank()
            ) {
                Text("Save Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddDepositDialog(
    members: List<MessMember>,
    onDismiss: () -> Unit,
    onAdd: (Long, String, Double) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var selectedMember by remember { mutableStateOf(members.firstOrNull() ?: MessMember(id = 1, memberName = "You")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Cash Deposit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Deposit Amount ($) *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Text("Member Depositing:", style = MaterialTheme.typography.bodySmall)
                members.forEach { m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMember = m },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedMember.id == m.id, onClick = { selectedMember = m })
                        Text(m.memberName, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onAdd(selectedMember.id, selectedMember.memberName, amt)
                    }
                },
                enabled = amountStr.isNotBlank()
            ) {
                Text("Record Deposit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("304-D") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Roommate") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Roommate Full Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room / Bed Identifier") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onAdd(name, room) }, enabled = name.isNotBlank()) {
                Text("Add Member")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SettleUpDialog(
    report: UserSettlementReport?,
    onDismiss: () -> Unit
) {
    val balance = report?.netBalance ?: 0.0
    val isSurplus = balance >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = if (isSurplus) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                )
                Text(if (isSurplus) "Surplus Reimbursement" else "Mess Settlement QR")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "QR Code",
                            tint = Color.Black,
                            modifier = Modifier.size(100.dp)
                        )
                        Text(
                            text = "SCAN TO PAY / REIMBURSE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = if (isSurplus) {
                        "You are due +$${String.format("%.2f", balance)}. Have the hostel manager scan this QR to transfer your surplus."
                    } else {
                        "Amount to Transfer: $${String.format("%.2f", -balance)}\nUPI / Bank: pinecrest304@mess.bank"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        }
    )
}
