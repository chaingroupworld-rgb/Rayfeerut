package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfile::class,
        AcademicTask::class,
        ClassSchedule::class,
        MessGroup::class,
        MessMember::class,
        MealLog::class,
        MessExpense::class,
        MemberDeposit::class,
        Habit::class,
        PomodoroSession::class,
        StudyNote::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RayfeerutDatabase : RoomDatabase() {

    abstract fun rayfeerutDao(): RayfeerutDao

    companion object {
        @Volatile
        private var INSTANCE: RayfeerutDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): RayfeerutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RayfeerutDatabase::class.java,
                    "rayfeerut_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.rayfeerutDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: RayfeerutDao) {
            // Seed User Profile
            dao.insertOrUpdateUserProfile(
                UserProfile(
                    id = "user_default",
                    fullName = "Alex Chen",
                    institution = "Tech University of Science",
                    major = "Computer Science",
                    semester = "Semester 5",
                    role = UserRole.STUDENT,
                    aiTokens = 8,
                    targetCgpa = 3.85,
                    currentCgpa = 3.68
                )
            )

            // Seed Tasks
            val now = System.currentTimeMillis()
            val oneDay = 24 * 60 * 60 * 1000L
            dao.insertTask(
                AcademicTask(
                    title = "Submit Distributed Systems Lab 3",
                    courseCode = "CS401",
                    dueDate = now + (oneDay * 0.75).toLong(), // due in 18 hrs (amber glow)
                    isCompleted = false,
                    priority = TaskPriority.HIGH,
                    notes = "Implement Raft consensus election algorithm in Go or Kotlin."
                )
            )
            dao.insertTask(
                AcademicTask(
                    title = "Database Systems Midterm Preparation",
                    courseCode = "CS310",
                    dueDate = now + (oneDay * 2).toLong(),
                    isCompleted = false,
                    priority = TaskPriority.HIGH,
                    notes = "Review B+ Tree indexing, normalization up to BCNF, and 2PL transaction locking."
                )
            )
            dao.insertTask(
                AcademicTask(
                    title = "Computer Networks Problem Set 4",
                    courseCode = "EE320",
                    dueDate = now + (oneDay * 4).toLong(),
                    isCompleted = false,
                    priority = TaskPriority.MEDIUM,
                    notes = "TCP congestion control window sliding exercises."
                )
            )
            dao.insertTask(
                AcademicTask(
                    title = "Technical Writing Ethics Essay",
                    courseCode = "HUM201",
                    dueDate = now + (oneDay * 6).toLong(),
                    isCompleted = true,
                    priority = TaskPriority.LOW,
                    notes = "Completed draft on AI copyright and fair use."
                )
            )

            // Seed Class Schedule
            dao.insertSchedules(
                listOf(
                    ClassSchedule(
                        courseName = "Distributed Systems",
                        courseCode = "CS401",
                        dayOfWeek = "Monday",
                        timeSlot = "09:00 AM - 10:30 AM",
                        roomNumber = "Lab 4B",
                        instructor = "Dr. Valerie Vance"
                    ),
                    ClassSchedule(
                        courseName = "Database Engineering",
                        courseCode = "CS310",
                        dayOfWeek = "Monday",
                        timeSlot = "11:00 AM - 12:30 PM",
                        roomNumber = "Auditorium 2",
                        instructor = "Prof. Marcus Brody"
                    ),
                    ClassSchedule(
                        courseName = "Computer Networks",
                        courseCode = "EE320",
                        dayOfWeek = "Tuesday",
                        timeSlot = "02:00 PM - 03:30 PM",
                        roomNumber = "Hall 105",
                        instructor = "Dr. S. Raman"
                    ),
                    ClassSchedule(
                        courseName = "Machine Learning Foundations",
                        courseCode = "CS450",
                        dayOfWeek = "Wednesday",
                        timeSlot = "10:00 AM - 11:30 AM",
                        roomNumber = "CS Seminar Room",
                        instructor = "Dr. Elena Rostova"
                    ),
                    ClassSchedule(
                        courseName = "Operating Systems Practicum",
                        courseCode = "CS305",
                        dayOfWeek = "Thursday",
                        timeSlot = "01:30 PM - 03:30 PM",
                        roomNumber = "Unix Systems Lab",
                        instructor = "Prof. David Miller"
                    )
                )
            )

            // Seed Mess Group
            dao.insertOrUpdateMessGroup(
                MessGroup(
                    id = 1,
                    groupName = "Pine Crest Hostel Flat 304",
                    inviteCode = "RAY304",
                    managerName = "Alex Chen",
                    managerId = "user_default",
                    monthlyBudget = 1400.0,
                    cutoffHour = 22
                )
            )

            // Seed Mess Members
            val members = listOf(
                MessMember(id = 1, messId = 1, memberName = "Alex Chen (You)", role = "Manager", isCurrentUser = true, roomNumber = "304-A"),
                MessMember(id = 2, messId = 1, memberName = "David Kim", role = "Member", isCurrentUser = false, roomNumber = "304-B"),
                MessMember(id = 3, messId = 1, memberName = "Rohan Sharma", role = "Member", isCurrentUser = false, roomNumber = "304-C"),
                MessMember(id = 4, messId = 1, memberName = "Liam O'Connor", role = "Member", isCurrentUser = false, roomNumber = "304-D")
            )
            dao.insertMembers(members)

            // Seed Meal Logs for recent days
            dao.insertMealLogs(
                listOf(
                    MealLog(messId = 1, memberId = 1, memberName = "Alex Chen (You)", dateString = "2026-10-08", breakfastCount = 1.0, lunchCount = 1.0, dinnerCount = 1.0),
                    MealLog(messId = 1, memberId = 2, memberName = "David Kim", dateString = "2026-10-08", breakfastCount = 0.5, lunchCount = 1.0, dinnerCount = 1.0),
                    MealLog(messId = 1, memberId = 3, memberName = "Rohan Sharma", dateString = "2026-10-08", breakfastCount = 1.0, lunchCount = 0.0, dinnerCount = 1.0),
                    MealLog(messId = 1, memberId = 4, memberName = "Liam O'Connor", dateString = "2026-10-08", breakfastCount = 1.0, lunchCount = 1.0, dinnerCount = 1.0),

                    MealLog(messId = 1, memberId = 1, memberName = "Alex Chen (You)", dateString = "2026-10-07", breakfastCount = 1.0, lunchCount = 1.0, dinnerCount = 1.0),
                    MealLog(messId = 1, memberId = 2, memberName = "David Kim", dateString = "2026-10-07", breakfastCount = 1.0, lunchCount = 1.0, dinnerCount = 1.0),
                    MealLog(messId = 1, memberId = 3, memberName = "Rohan Sharma", dateString = "2026-10-07", breakfastCount = 0.0, lunchCount = 1.0, dinnerCount = 1.0),
                    MealLog(messId = 1, memberId = 4, memberName = "Liam O'Connor", dateString = "2026-10-07", breakfastCount = 1.0, lunchCount = 1.0, dinnerCount = 0.5)
                )
            )

            // Seed Expenses
            dao.insertExpense(
                MessExpense(
                    messId = 1,
                    paidByMemberId = 1,
                    paidByName = "Alex Chen (You)",
                    amount = 124.50,
                    description = "Weekly Vegetables & Chicken Groceries",
                    category = "Groceries",
                    splitType = ExpenseSplitType.MEAL_PROPORTIONAL,
                    expenseDate = "2026-10-06"
                )
            )
            dao.insertExpense(
                MessExpense(
                    messId = 1,
                    paidByMemberId = 2,
                    paidByName = "David Kim",
                    amount = 88.00,
                    description = "Basmati Rice 20kg & Cooking Oil Canister",
                    category = "Groceries",
                    splitType = ExpenseSplitType.MEAL_PROPORTIONAL,
                    expenseDate = "2026-10-04"
                )
            )
            dao.insertExpense(
                MessExpense(
                    messId = 1,
                    paidByMemberId = 3,
                    paidByName = "Rohan Sharma",
                    amount = 60.00,
                    description = "High-Speed Fiber Wifi Bill (October)",
                    category = "Wifi",
                    splitType = ExpenseSplitType.EQUAL_SPLIT,
                    expenseDate = "2026-10-01"
                )
            )

            // Seed Deposits
            dao.insertDeposit(MemberDeposit(messId = 1, memberId = 1, memberName = "Alex Chen (You)", amount = 150.0, depositDate = "2026-10-01"))
            dao.insertDeposit(MemberDeposit(messId = 1, memberId = 2, memberName = "David Kim", amount = 120.0, depositDate = "2026-10-01"))
            dao.insertDeposit(MemberDeposit(messId = 1, memberId = 3, memberName = "Rohan Sharma", amount = 100.0, depositDate = "2026-10-01"))
            dao.insertDeposit(MemberDeposit(messId = 1, memberId = 4, memberName = "Liam O'Connor", amount = 140.0, depositDate = "2026-10-01"))

            // Seed Habits
            dao.insertHabit(Habit(name = "Drink 2L Water", targetDaysPerWeek = 7, currentStreak = 8, streakFreezesAvailable = 1, isCompletedToday = true, lastCompletedDate = "2026-10-08"))
            dao.insertHabit(Habit(name = "Read 15 Pages Research Papers", targetDaysPerWeek = 5, currentStreak = 5, streakFreezesAvailable = 2, isCompletedToday = false))
            dao.insertHabit(Habit(name = "Review LeetCode Daily Challenge", targetDaysPerWeek = 6, currentStreak = 12, streakFreezesAvailable = 1, isCompletedToday = true, lastCompletedDate = "2026-10-08"))
            dao.insertHabit(Habit(name = "Sleep Before 12:30 AM", targetDaysPerWeek = 7, currentStreak = 3, streakFreezesAvailable = 0, isCompletedToday = false))

            // Seed Pomodoro Sessions
            dao.insertPomodoroSession(PomodoroSession(taskTitle = "Distributed Systems Consensus", courseCode = "CS401", durationMinutes = 50, timestamp = now - 3600000L * 4))
            dao.insertPomodoroSession(PomodoroSession(taskTitle = "SQL Index Optimization", courseCode = "CS310", durationMinutes = 25, timestamp = now - 3600000L * 24))
            dao.insertPomodoroSession(PomodoroSession(taskTitle = "Subnetting Practice", courseCode = "EE320", durationMinutes = 25, timestamp = now - 3600000L * 48))

            // Seed Study Notes
            dao.insertStudyNote(
                StudyNote(
                    title = "Distributed Systems: CAP Theorem & Consensus",
                    courseCode = "CS401",
                    markdownContent = "# CAP Theorem & Consensus\n\n## 1. Core Tradeoffs\nIn a distributed network, a system can provide at most two of three guarantees:\n- **Consistency (C)**: Every read receives the most recent write.\n- **Availability (A)**: Every non-failing node returns a response.\n- **Partition Tolerance (P)**: The system continues operating despite dropped messages.\n\nSince network partitions are inevitable in real-world hardware, systems must choose between **CP** and **AP**.",
                    lastEdited = now - 86400000L,
                    aiSummary = "Summary: Real networks always experience partitions (P), so distributed databases must trade off strict consistency (CP, e.g. Spanner/Raft) versus high availability (AP, e.g. Cassandra/Dynamo)."
                )
            )
            dao.insertStudyNote(
                StudyNote(
                    title = "Database Indexing: B+ Trees vs LSM Trees",
                    courseCode = "CS310",
                    markdownContent = "# Database Indexing Architecture\n\n## B+ Tree Characteristics\n- Balanced search tree with all data stored in leaf nodes.\n- Excellent for range queries (`BETWEEN a AND b`).\n- High read throughput, random writes can require disk page splits.\n\n## Log-Structured Merge (LSM) Trees\n- Append-only write path (`MemTable` in RAM -> `SSTables` on disk).\n- Blazing write speeds for write-heavy workloads (LevelDB, RocksDB, Cassandra).",
                    lastEdited = now - 43200000L,
                    aiSummary = "Summary: B+ Trees optimize for point reads and range scans on SSDs, whereas LSM trees optimize for high-frequency write operations by sequentially appending to MemTables."
                )
            )
        }
    }
}
