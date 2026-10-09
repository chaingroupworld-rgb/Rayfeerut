package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RayfeerutDao {

    // User Profile
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserProfile(userId: String = "user_default"): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserProfile(user: UserProfile)

    // Academic Tasks
    @Query("SELECT * FROM academic_tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<AcademicTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: AcademicTask): Long

    @Update
    suspend fun updateTask(task: AcademicTask)

    @Delete
    suspend fun deleteTask(task: AcademicTask)

    // Class Schedules
    @Query("SELECT * FROM class_schedules ORDER BY id ASC")
    fun getAllSchedules(): Flow<List<ClassSchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<ClassSchedule>)

    @Query("DELETE FROM class_schedules")
    suspend fun clearSchedules()

    // Mess Group
    @Query("SELECT * FROM mess_groups WHERE id = 1 LIMIT 1")
    fun getMessGroup(): Flow<MessGroup?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMessGroup(group: MessGroup)

    // Mess Members
    @Query("SELECT * FROM mess_members WHERE messId = :messId")
    fun getMembers(messId: Long = 1): Flow<List<MessMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MessMember>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MessMember): Long

    // Meal Logs
    @Query("SELECT * FROM meal_logs WHERE messId = :messId ORDER BY dateString DESC")
    fun getMealLogs(messId: Long = 1): Flow<List<MealLog>>

    @Query("SELECT * FROM meal_logs WHERE messId = :messId AND dateString = :date")
    fun getMealLogsByDate(messId: Long = 1, date: String): Flow<List<MealLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealLog(log: MealLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealLogs(logs: List<MealLog>)

    @Update
    suspend fun updateMealLog(log: MealLog)

    // Mess Expenses
    @Query("SELECT * FROM mess_expenses WHERE messId = :messId ORDER BY id DESC")
    fun getExpenses(messId: Long = 1): Flow<List<MessExpense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: MessExpense): Long

    @Delete
    suspend fun deleteExpense(expense: MessExpense)

    // Member Deposits
    @Query("SELECT * FROM member_deposits WHERE messId = :messId ORDER BY id DESC")
    fun getDeposits(messId: Long = 1): Flow<List<MemberDeposit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeposit(deposit: MemberDeposit): Long

    // Habits
    @Query("SELECT * FROM habits ORDER BY id ASC")
    fun getAllHabits(): Flow<List<Habit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    // Pomodoro
    @Query("SELECT * FROM pomodoro_sessions ORDER BY timestamp DESC")
    fun getAllPomodoroSessions(): Flow<List<PomodoroSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPomodoroSession(session: PomodoroSession): Long

    // Study Notes
    @Query("SELECT * FROM study_notes ORDER BY lastEdited DESC")
    fun getAllStudyNotes(): Flow<List<StudyNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyNote(note: StudyNote): Long

    @Update
    suspend fun updateStudyNote(note: StudyNote)

    @Delete
    suspend fun deleteStudyNote(note: StudyNote)
}
