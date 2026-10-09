package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskPriority {
    LOW, MEDIUM, HIGH
}

enum class UserRole {
    STUDENT, MESS_MANAGER
}

enum class ExpenseSplitType {
    MEAL_PROPORTIONAL, EQUAL_SPLIT
}

@Entity(tableName = "users")
data class UserProfile(
    @PrimaryKey val id: String = "user_default",
    val fullName: String,
    val institution: String,
    val major: String,
    val semester: String,
    val role: UserRole = UserRole.STUDENT,
    val aiTokens: Int = 10,
    val targetCgpa: Double = 3.80,
    val currentCgpa: Double = 3.65
)

@Entity(tableName = "academic_tasks")
data class AcademicTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val courseCode: String? = null,
    val dueDate: Long, // timestamp
    val isCompleted: Boolean = false,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val notes: String? = null
)

@Entity(tableName = "class_schedules")
data class ClassSchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseName: String,
    val courseCode: String,
    val dayOfWeek: String, // Monday, Tuesday, etc.
    val timeSlot: String,  // e.g. "10:00 AM - 11:30 AM"
    val roomNumber: String,
    val instructor: String
)

@Entity(tableName = "mess_groups")
data class MessGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 1,
    val groupName: String,
    val inviteCode: String,
    val managerName: String,
    val managerId: String,
    val monthlyBudget: Double = 1200.0,
    val cutoffHour: Int = 22 // 10 PM cutoff
)

@Entity(tableName = "mess_members")
data class MessMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messId: Long = 1,
    val memberName: String,
    val role: String = "Member",
    val isCurrentUser: Boolean = false,
    val roomNumber: String = "Room 304"
)

@Entity(tableName = "meal_logs")
data class MealLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messId: Long = 1,
    val memberId: Long,
    val memberName: String,
    val dateString: String, // "YYYY-MM-DD"
    val breakfastCount: Double = 1.0, // 0, 0.5, 1.0
    val lunchCount: Double = 1.0,
    val dinnerCount: Double = 1.0
) {
    val totalMeals: Double get() = breakfastCount + lunchCount + dinnerCount
}

@Entity(tableName = "mess_expenses")
data class MessExpense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messId: Long = 1,
    val paidByMemberId: Long,
    val paidByName: String,
    val amount: Double,
    val description: String,
    val category: String, // "Groceries", "Rent", "Utilities", "Wifi", "Cook Maid"
    val splitType: ExpenseSplitType = ExpenseSplitType.MEAL_PROPORTIONAL,
    val expenseDate: String, // "YYYY-MM-DD"
    val receiptUrl: String? = null
)

@Entity(tableName = "member_deposits")
data class MemberDeposit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messId: Long = 1,
    val memberId: Long,
    val memberName: String,
    val amount: Double,
    val depositDate: String
)

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetDaysPerWeek: Int = 7,
    val currentStreak: Int = 0,
    val streakFreezesAvailable: Int = 1,
    val isCompletedToday: Boolean = false,
    val lastCompletedDate: String? = null
)

@Entity(tableName = "pomodoro_sessions")
data class PomodoroSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskTitle: String,
    val courseCode: String? = null,
    val durationMinutes: Int = 25,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_notes")
data class StudyNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val courseCode: String,
    val markdownContent: String,
    val lastEdited: Long = System.currentTimeMillis(),
    val aiSummary: String? = null
)

data class CourseResource(
    val id: Long,
    val title: String,
    val courseCode: String,
    val category: String, // "Past Exam Paper", "Lecture Slides", "Lab Manual", "Formula Sheet"
    val size: String,
    val downloadCount: Int
)
