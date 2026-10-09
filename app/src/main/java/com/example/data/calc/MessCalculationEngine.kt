package com.example.data.calc

import com.example.data.model.ExpenseSplitType
import com.example.data.model.MealLog
import com.example.data.model.MemberDeposit
import com.example.data.model.MessExpense
import com.example.data.model.MessMember
import kotlin.math.round

data class UserSettlementReport(
    val memberId: Long,
    val memberName: String,
    val isCurrentUser: Boolean,
    val totalMeals: Double,
    val mealCost: Double,
    val fixedCost: Double,
    val totalBill: Double,
    val expensesPaidDirectly: Double,
    val totalDeposited: Double,
    val totalContributed: Double,
    val netBalance: Double, // Positive = Surplus (gets back), Negative = Deficit (owes)
    val statusText: String
)

data class MessSummaryReport(
    val totalMealsConsumed: Double,
    val totalFoodExpenses: Double,
    val totalFixedExpenses: Double,
    val totalExpenses: Double,
    val totalDeposits: Double,
    val currentMealRate: Double,
    val memberReports: List<UserSettlementReport>,
    val cashOnHand: Double
)

object MessCalculationEngine {

    /**
     * Computes the complete financial settlement report for the mess group.
     */
    fun calculateSettlement(
        members: List<MessMember>,
        mealLogs: List<MealLog>,
        expenses: List<MessExpense>,
        deposits: List<MemberDeposit>
    ): MessSummaryReport {
        val totalMeals = mealLogs.sumOf { it.breakfastCount + it.lunchCount + it.dinnerCount }
        
        // Split expenses into Meal-Proportional (food/groceries) and Equal-Split (rent/wifi/maid)
        val foodExpenses = expenses
            .filter { it.splitType == ExpenseSplitType.MEAL_PROPORTIONAL }
            .sumOf { it.amount }
        
        val fixedExpenses = expenses
            .filter { it.splitType == ExpenseSplitType.EQUAL_SPLIT }
            .sumOf { it.amount }
            
        val totalExpenses = foodExpenses + fixedExpenses
        val totalDeposits = deposits.sumOf { it.amount }

        // Dynamic Meal Rate: if meals > 0, foodExpenses / totalMeals, else 0.0
        val currentMealRate = if (totalMeals > 0.0) {
            roundToTwoDecimals(foodExpenses / totalMeals)
        } else {
            0.0
        }

        val activeMemberCount = if (members.isNotEmpty()) members.size else 1
        val fixedCostPerMember = roundToTwoDecimals(fixedExpenses / activeMemberCount)

        // Generate individual reports
        val memberReports = members.map { member ->
            val memberLogs = mealLogs.filter { it.memberId == member.id }
            val memberMeals = memberLogs.sumOf { it.breakfastCount + it.lunchCount + it.dinnerCount }
            val memberMealCost = roundToTwoDecimals(memberMeals * currentMealRate)
            val memberBill = roundToTwoDecimals(memberMealCost + fixedCostPerMember)

            val memberExpensesPaid = roundToTwoDecimals(
                expenses.filter { it.paidByMemberId == member.id }.sumOf { it.amount }
            )
            val memberDeposited = roundToTwoDecimals(
                deposits.filter { it.memberId == member.id }.sumOf { it.amount }
            )
            val totalContributed = roundToTwoDecimals(memberExpensesPaid + memberDeposited)
            val netBalance = roundToTwoDecimals(totalContributed - memberBill)

            val statusText = when {
                netBalance > 0.05 -> "Surplus: Due +$${String.format("%.2f", netBalance)}"
                netBalance < -0.05 -> "Deficit: Owes $${String.format("%.2f", -netBalance)}"
                else -> "All Settled"
            }

            UserSettlementReport(
                memberId = member.id,
                memberName = member.memberName,
                isCurrentUser = member.isCurrentUser,
                totalMeals = memberMeals,
                mealCost = memberMealCost,
                fixedCost = fixedCostPerMember,
                totalBill = memberBill,
                expensesPaidDirectly = memberExpensesPaid,
                totalDeposited = memberDeposited,
                totalContributed = totalContributed,
                netBalance = netBalance,
                statusText = statusText
            )
        }

        // Cash on hand in the mess treasury = Total cash deposits - actual reimbursement payouts/expenses
        val cashOnHand = roundToTwoDecimals(totalDeposits - totalExpenses)

        return MessSummaryReport(
            totalMealsConsumed = totalMeals,
            totalFoodExpenses = foodExpenses,
            totalFixedExpenses = fixedExpenses,
            totalExpenses = totalExpenses,
            totalDeposits = totalDeposits,
            currentMealRate = currentMealRate,
            memberReports = memberReports,
            cashOnHand = cashOnHand
        )
    }

    private fun roundToTwoDecimals(value: Double): Double {
        return round(value * 100.0) / 100.0
    }
}
