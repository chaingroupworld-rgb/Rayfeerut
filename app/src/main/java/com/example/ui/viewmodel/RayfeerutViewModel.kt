package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiRepository
import com.example.data.calc.MessCalculationEngine
import com.example.data.calc.MessSummaryReport
import com.example.data.local.RayfeerutDatabase
import com.example.data.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class ActiveTab(val title: String) {
    DASHBOARD("Dashboard"),
    ACADEMICS("Academics"),
    MESS("Mess Ledger"),
    FOCUS("Focus & Habits"),
    VAULT("Vault & AI")
}

data class PomodoroTimerState(
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val isBreak: Boolean = false,
    val currentTaskTitle: String = "Deep Work Session",
    val completedSessionsCount: Int = 0
)

data class UiNotification(
    val message: String,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class RayfeerutViewModel(application: Application) : AndroidViewModel(application) {

    private val db = RayfeerutDatabase.getDatabase(application, viewModelScope)
    private val dao = db.rayfeerutDao()
    private val geminiRepo = GeminiRepository()

    // Navigation & UI States
    private val _currentTab = MutableStateFlow(ActiveTab.DASHBOARD)
    val currentTab: StateFlow<ActiveTab> = _currentTab.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _notification = MutableStateFlow<UiNotification?>(null)
    val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

    private val _showConfetti = MutableStateFlow(false)
    val showConfetti: StateFlow<Boolean> = _showConfetti.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _rewardedAdVisible = MutableStateFlow(false)
    val rewardedAdVisible: StateFlow<Boolean> = _rewardedAdVisible.asStateFlow()

    // Database Flows
    val userProfile: StateFlow<UserProfile?> = dao.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tasks: StateFlow<List<AcademicTask>> = dao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val schedules: StateFlow<List<ClassSchedule>> = dao.getAllSchedules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messGroup: StateFlow<MessGroup?> = dao.getMessGroup()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val messMembers: StateFlow<List<MessMember>> = dao.getMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mealLogs: StateFlow<List<MealLog>> = dao.getMealLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messExpenses: StateFlow<List<MessExpense>> = dao.getExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memberDeposits: StateFlow<List<MemberDeposit>> = dao.getDeposits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habits: StateFlow<List<Habit>> = dao.getAllHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pomodoroSessions: StateFlow<List<PomodoroSession>> = dao.getAllPomodoroSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyNotes: StateFlow<List<StudyNote>> = dao.getAllStudyNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real-Time Mess Calculation Report
    val messSummaryReport: StateFlow<MessSummaryReport> = combine(
        messMembers,
        mealLogs,
        messExpenses,
        memberDeposits
    ) { members, logs, expenses, deposits ->
        MessCalculationEngine.calculateSettlement(members, logs, expenses, deposits)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MessSummaryReport(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, emptyList(), 0.0)
    )

    // Pomodoro Timer Engine
    private val _timerState = MutableStateFlow(PomodoroTimerState())
    val timerState: StateFlow<PomodoroTimerState> = _timerState.asStateFlow()
    private var timerJob: Job? = null

    // Welcome Greeting
    private val _welcomeGreeting = MutableStateFlow("Ready to achieve your academic and living goals?")
    val welcomeGreeting: StateFlow<String> = _welcomeGreeting.asStateFlow()

    init {
        // Initial setup check
        viewModelScope.launch {
            userProfile.filterNotNull().first().let { profile ->
                refreshWelcomeGreeting(profile.major)
            }
        }
    }

    fun selectTab(tab: ActiveTab) {
        _currentTab.value = tab
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun showNotification(msg: String, isError: Boolean = false) {
        _notification.value = UiNotification(msg, isError)
    }

    fun dismissNotification() {
        _notification.value = null
    }

    fun setRewardedAdVisible(visible: Boolean) {
        _rewardedAdVisible.value = visible
    }

    fun rewardAiTokens(amount: Int = 3) {
        viewModelScope.launch {
            val current = userProfile.value ?: return@launch
            val updated = current.copy(aiTokens = current.aiTokens + amount)
            dao.insertOrUpdateUserProfile(updated)
            showNotification("Earned +$amount AI Tokens! Balance: ${updated.aiTokens}")
        }
    }

    private fun consumeAiToken(): Boolean {
        val current = userProfile.value ?: return true
        if (current.aiTokens <= 0) {
            setRewardedAdVisible(true)
            showNotification("AI tokens depleted! Watch an ad to earn +3 tokens.", isError = true)
            return false
        }
        viewModelScope.launch {
            dao.insertOrUpdateUserProfile(current.copy(aiTokens = current.aiTokens - 1))
        }
        return true
    }

    fun refreshWelcomeGreeting(major: String) {
        viewModelScope.launch {
            val greeting = geminiRepo.generateWelcomeGreeting(major)
            _welcomeGreeting.value = greeting
        }
    }

    fun updateUserProfile(name: String, institution: String, major: String, semester: String, role: UserRole) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfile(fullName = name, institution = institution, major = major, semester = semester)
            val updated = current.copy(fullName = name, institution = institution, major = major, semester = semester, role = role)
            dao.insertOrUpdateUserProfile(updated)
            refreshWelcomeGreeting(major)
            showNotification("Profile updated successfully")
        }
    }

    // ==========================================
    // MODULE B: ACADEMICS & GEMINI TASK MANAGER
    // ==========================================

    fun addTask(title: String, courseCode: String?, dueHoursFromNow: Long, priority: TaskPriority, notes: String? = null) {
        viewModelScope.launch {
            val due = System.currentTimeMillis() + (dueHoursFromNow * 3600000L)
            dao.insertTask(
                AcademicTask(
                    title = title,
                    courseCode = courseCode,
                    dueDate = due,
                    priority = priority,
                    notes = notes
                )
            )
            showNotification("Task created: $title")
        }
    }

    fun addNaturalLanguageTaskWithAi(prompt: String) {
        if (!consumeAiToken()) return
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val parsed = geminiRepo.parseNaturalLanguageTask(prompt)
                dao.insertTask(parsed)
                showNotification("AI parsed & added: ${parsed.title}")
            } catch (e: Exception) {
                showNotification("Failed to parse task: ${e.message}", isError = true)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun toggleTaskCompletion(task: AcademicTask) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            dao.updateTask(updated)
            if (updated.isCompleted) {
                triggerConfetti()
            }
        }
    }

    fun deleteTask(task: AcademicTask) {
        viewModelScope.launch {
            dao.deleteTask(task)
            showNotification("Task deleted")
        }
    }

    fun importSyllabusRoutine(syllabusText: String) {
        if (!consumeAiToken()) return
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val schedules = geminiRepo.parseSyllabusRoutine(syllabusText)
                if (schedules.isNotEmpty()) {
                    dao.clearSchedules()
                    dao.insertSchedules(schedules)
                    showNotification("Imported ${schedules.size} class routine slots!")
                } else {
                    showNotification("Could not parse schedule routines", isError = true)
                }
            } catch (e: Exception) {
                showNotification("Import failed: ${e.message}", isError = true)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    // ==========================================
    // MODULE C: MESS & SHARED LIVING ENGINE
    // ==========================================

    fun logDailyMeal(memberId: Long, memberName: String, breakfast: Double, lunch: Double, dinner: Double, dateString: String = todayDateString()) {
        viewModelScope.launch {
            val existing = mealLogs.value.firstOrNull { it.memberId == memberId && it.dateString == dateString }
            if (existing != null) {
                dao.updateMealLog(existing.copy(breakfastCount = breakfast, lunchCount = lunch, dinnerCount = dinner))
            } else {
                dao.insertMealLog(
                    MealLog(
                        messId = 1,
                        memberId = memberId,
                        memberName = memberName,
                        dateString = dateString,
                        breakfastCount = breakfast,
                        lunchCount = lunch,
                        dinnerCount = dinner
                    )
                )
            }
            showNotification("Updated meals for $memberName ($dateString)")
        }
    }

    fun addMessExpense(amount: Double, description: String, category: String, splitType: ExpenseSplitType, paidByMemberId: Long, paidByName: String) {
        viewModelScope.launch {
            dao.insertExpense(
                MessExpense(
                    messId = 1,
                    paidByMemberId = paidByMemberId,
                    paidByName = paidByName,
                    amount = amount,
                    description = description,
                    category = category,
                    splitType = splitType,
                    expenseDate = todayDateString()
                )
            )
            showNotification("Added $$amount expense for $category")
        }
    }

    fun addMemberDeposit(memberId: Long, memberName: String, amount: Double) {
        viewModelScope.launch {
            dao.insertDeposit(
                MemberDeposit(
                    messId = 1,
                    memberId = memberId,
                    memberName = memberName,
                    amount = amount,
                    depositDate = todayDateString()
                )
            )
            showNotification("Recorded $$amount deposit from $memberName")
        }
    }

    fun addMessMember(name: String, roomNumber: String) {
        viewModelScope.launch {
            dao.insertMember(
                MessMember(
                    messId = 1,
                    memberName = name,
                    role = "Member",
                    isCurrentUser = false,
                    roomNumber = roomNumber
                )
            )
            showNotification("Added new roommate: $name")
        }
    }

    // ==========================================
    // MODULE D: POMODORO & HABITS
    // ==========================================

    fun startTimer(taskTitle: String = "Deep Work Session") {
        if (_timerState.value.isRunning) return
        _timerState.value = _timerState.value.copy(
            isRunning = true,
            currentTaskTitle = taskTitle
        )
        timerJob = viewModelScope.launch {
            while (_timerState.value.remainingSeconds > 0 && _timerState.value.isRunning) {
                delay(1000L)
                _timerState.value = _timerState.value.copy(
                    remainingSeconds = _timerState.value.remainingSeconds - 1
                )
            }
            if (_timerState.value.remainingSeconds == 0) {
                onTimerFinished()
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _timerState.value = _timerState.value.copy(isRunning = false)
    }

    fun resetTimer(minutes: Int = 25, isBreak: Boolean = false) {
        timerJob?.cancel()
        _timerState.value = PomodoroTimerState(
            totalSeconds = minutes * 60,
            remainingSeconds = minutes * 60,
            isRunning = false,
            isBreak = isBreak,
            currentTaskTitle = _timerState.value.currentTaskTitle,
            completedSessionsCount = _timerState.value.completedSessionsCount
        )
    }

    private fun onTimerFinished() {
        val completedState = _timerState.value
        if (!completedState.isBreak) {
            viewModelScope.launch {
                dao.insertPomodoroSession(
                    PomodoroSession(
                        taskTitle = completedState.currentTaskTitle,
                        durationMinutes = completedState.totalSeconds / 60
                    )
                )
            }
            triggerConfetti()
            showNotification("Pomodoro Complete! Enjoy a 5-minute break.")
            resetTimer(minutes = 5, isBreak = true)
        } else {
            showNotification("Break ended. Ready for the next deep work session?")
            resetTimer(minutes = 25, isBreak = false)
        }
    }

    fun toggleHabit(habit: Habit) {
        viewModelScope.launch {
            val today = todayDateString()
            val wasCompleted = habit.isCompletedToday
            val newCompleted = !wasCompleted
            val newStreak = if (newCompleted) habit.currentStreak + 1 else maxOf(0, habit.currentStreak - 1)
            val updated = habit.copy(
                isCompletedToday = newCompleted,
                currentStreak = newStreak,
                lastCompletedDate = if (newCompleted) today else habit.lastCompletedDate
            )
            dao.updateHabit(updated)
            if (newCompleted) {
                if (newStreak >= 7) {
                    triggerConfetti()
                    showNotification("🔥 7+ Day Streak Milestone Reached!")
                } else {
                    showNotification("Habit checked: ${habit.name}")
                }
            }
        }
    }

    fun addHabit(name: String, targetDays: Int = 7) {
        viewModelScope.launch {
            dao.insertHabit(Habit(name = name, targetDaysPerWeek = targetDays))
            showNotification("New habit added: $name")
        }
    }

    fun useStreakFreeze(habit: Habit) {
        viewModelScope.launch {
            if (habit.streakFreezesAvailable > 0) {
                val updated = habit.copy(
                    streakFreezesAvailable = habit.streakFreezesAvailable - 1,
                    isCompletedToday = true
                )
                dao.updateHabit(updated)
                showNotification("Used 1 Streak Freeze to protect '${habit.name}'!")
            } else {
                showNotification("No streak freezes available", isError = true)
            }
        }
    }

    // ==========================================
    // MODULE E: STUDY NOTES & RESOURCE VAULT
    // ==========================================

    fun addStudyNote(title: String, courseCode: String, content: String) {
        viewModelScope.launch {
            dao.insertStudyNote(
                StudyNote(
                    title = title,
                    courseCode = courseCode,
                    markdownContent = content,
                    lastEdited = System.currentTimeMillis()
                )
            )
            showNotification("Saved note: $title")
        }
    }

    fun updateStudyNote(note: StudyNote, newContent: String) {
        viewModelScope.launch {
            dao.updateStudyNote(note.copy(markdownContent = newContent, lastEdited = System.currentTimeMillis()))
        }
    }

    fun deleteStudyNote(note: StudyNote) {
        viewModelScope.launch {
            dao.deleteStudyNote(note)
            showNotification("Note removed")
        }
    }

    suspend fun askGeminiStudyAssistant(concept: String, mode: String): String {
        if (!consumeAiToken()) return "AI tokens depleted. Watch a sponsor ad to earn more tokens."
        _isAiLoading.value = true
        return try {
            geminiRepo.explainStudyConcept(concept, mode)
        } finally {
            _isAiLoading.value = false
        }
    }

    private fun triggerConfetti() {
        viewModelScope.launch {
            _showConfetti.value = true
            delay(3500L)
            _showConfetti.value = false
        }
    }

    private fun todayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
