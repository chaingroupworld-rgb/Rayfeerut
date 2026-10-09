package com.example

import com.example.data.calc.MessCalculationEngine
import com.example.data.model.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testMessCalculationEngine_accurateSettlementAndMealRate() {
        val members = listOf(
            MessMember(id = 1, memberName = "Alice", isCurrentUser = true),
            MessMember(id = 2, memberName = "Bob", isCurrentUser = false)
        )

        val mealLogs = listOf(
            MealLog(id = 1, messId = 1, memberId = 1, memberName = "Alice", dateString = "2026-10-01", breakfastCount = 1.0, lunchCount = 1.0, dinnerCount = 1.0), // 3 meals
            MealLog(id = 2, messId = 1, memberId = 2, memberName = "Bob", dateString = "2026-10-01", breakfastCount = 0.5, lunchCount = 1.0, dinnerCount = 0.5)   // 2 meals
        )
        // Total meals = 5.0

        val expenses = listOf(
            // Food expense: $50.00
            MessExpense(id = 1, messId = 1, paidByMemberId = 1, paidByName = "Alice", amount = 50.0, description = "Groceries", category = "Groceries", splitType = ExpenseSplitType.MEAL_PROPORTIONAL, expenseDate = "2026-10-01"),
            // Fixed expense: $20.00 (Split equally: $10 each)
            MessExpense(id = 2, messId = 1, paidByMemberId = 2, paidByName = "Bob", amount = 20.0, description = "Wifi", category = "Wifi", splitType = ExpenseSplitType.EQUAL_SPLIT, expenseDate = "2026-10-01")
        )

        val deposits = listOf(
            MemberDeposit(id = 1, messId = 1, memberId = 1, memberName = "Alice", amount = 10.0, depositDate = "2026-10-01"),
            MemberDeposit(id = 2, messId = 1, memberId = 2, memberName = "Bob", amount = 30.0, depositDate = "2026-10-01")
        )

        val report = MessCalculationEngine.calculateSettlement(members, mealLogs, expenses, deposits)

        // 1. Total meals consumed
        assertEquals(5.0, report.totalMealsConsumed, 0.001)

        // 2. Meal rate = $50.00 / 5.0 = $10.00 / meal
        assertEquals(10.0, report.currentMealRate, 0.001)

        // 3. Fixed cost per member = $20 / 2 = $10.00
        assertEquals(20.0, report.totalFixedExpenses, 0.001)

        // 4. Alice report:
        // Meals: 3 -> Meal cost: $30.00
        // Fixed: $10.00 -> Total bill: $40.00
        // Paid directly: $50.00 + Deposited: $10.00 = Contributed: $60.00
        // Net balance: $60.00 - $40.00 = +$20.00 (Surplus)
        val alice = report.memberReports.first { it.memberId == 1L }
        assertEquals(3.0, alice.totalMeals, 0.001)
        assertEquals(30.0, alice.mealCost, 0.001)
        assertEquals(40.0, alice.totalBill, 0.001)
        assertEquals(60.0, alice.totalContributed, 0.001)
        assertEquals(20.0, alice.netBalance, 0.001)
        assertTrue(alice.netBalance > 0)

        // 5. Bob report:
        // Meals: 2 -> Meal cost: $20.00
        // Fixed: $10.00 -> Total bill: $30.00
        // Paid directly: $20.00 + Deposited: $30.00 = Contributed: $50.00
        // Net balance: $50.00 - $30.00 = +$20.00 (Surplus)
        val bob = report.memberReports.first { it.memberId == 2L }
        assertEquals(2.0, bob.totalMeals, 0.001)
        assertEquals(20.0, bob.mealCost, 0.001)
        assertEquals(30.0, bob.totalBill, 0.001)
    }

    @Test
    fun testMessCalculationEngine_zeroMealsHandledSafely() {
        val members = listOf(MessMember(id = 1, memberName = "Alice"))
        val report = MessCalculationEngine.calculateSettlement(members, emptyList(), emptyList(), emptyList())
        assertEquals(0.0, report.totalMealsConsumed, 0.001)
        assertEquals(0.0, report.currentMealRate, 0.001)
    }
}
