package com.example.triviaquiz

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.triviaquiz.data.QuizPreferences
import com.example.triviaquiz.data.QuizRepository
import com.example.triviaquiz.databinding.ActivityMainBinding
import com.example.triviaquiz.model.Question
import com.example.triviaquiz.model.QuizState
import com.example.triviaquiz.network.NetworkResult
import com.example.triviaquiz.network.NetworkUtils
import com.example.triviaquiz.util.SoundManager
import kotlinx.coroutines.launch

/**
 * Professional Trivia Quiz Activity — single-screen architecture with multiple logical states.
 * Handles: Home, Loading, Error, Quiz, Flagged, Result, Review, History, Achievements, Settings.
 *
 * Demonstrates all assignment requirements:
 * - View Binding, Kotlin Coroutines, lifecycleScope.launch
 * - Connectivity check, HttpURLConnection (via Repository/NetworkClient)
 * - JSONObject/JSONArray parsing (via JsonParser)
 * - Loading/Error/Retry states
 * - Answer validation, score tracking, final result
 * - Previous/Next navigation without double-counting
 */
class MainActivity : AppCompatActivity() {

    // View Binding
    private lateinit var binding: ActivityMainBinding

    // Core quiz components
    private val repository = QuizRepository()
    private lateinit var prefs: QuizPreferences
    private lateinit var soundManager: SoundManager

    // API configuration
    private var selectedCategory: Int? = null
    private var selectedDifficulty: String? = null
    private var selectedCount: Int = 10

    // Category data: name to Open Trivia DB ID mapping
    private val categoryNames = listOf(
        "Any Category", "General Knowledge", "Books", "Film", "Music",
        "Television", "Video Games", "Science", "Computers", "Mathematics",
        "Mythology", "Sports", "Geography", "History", "Politics",
        "Art", "Celebrities", "Animals"
    )
    private val categoryIds = listOf(
        null, 9, 10, 11, 12, 14, 15, 17, 18, 19,
        20, 21, 22, 23, 24, 25, 26, 27
    )

    // Difficulty data
    private val difficultyNames = listOf("Any Difficulty", "Easy", "Medium", "Hard")
    private val difficultyValues = listOf(null, "easy", "medium", "hard")

    // Quiz session state
    private var questions = listOf<Question>()
    private var quizStates = mutableListOf<QuizState>()
    private var currentIndex = 0
    private var currentStreak = 0
    private var bestStreak = 0

    // Lifeline availability (one use per quiz)
    private var isFiftyFiftyAvailable = true
    private var isSkipAvailable = true
    private var isAddTimeAvailable = true

    // Timer
    private var countDownTimer: CountDownTimer? = null
    private var timeRemaining = 20000L // milliseconds
    private var isTimerRunning = false

    // Track first answer time for "Fast Thinker" achievement
    private var firstAnswerTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize helpers
        prefs = QuizPreferences(this)
        soundManager = SoundManager(this)

        // Setup all listeners
        setupHomeScreen()
        setupQuizScreen()
        setupResultScreen()
        setupSecondaryScreens()

        // Show home screen
        showScreen("home")
    }

    // ==================== HOME SCREEN ====================

    private fun setupHomeScreen() {
        // Category Spinner
        val catAdapter = android.widget.ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, categoryNames
        )
        binding.categorySpinner.adapter = catAdapter
        binding.categorySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategory = categoryIds[position]
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Difficulty Spinner
        val diffAdapter = android.widget.ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, difficultyNames
        )
        binding.difficultySpinner.adapter = diffAdapter
        binding.difficultySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedDifficulty = difficultyValues[position]
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Question count buttons
        val countButtons = listOf(binding.countBtn5, binding.countBtn10, binding.countBtn15, binding.countBtn20)
        val counts = listOf(5, 10, 15, 20)

        countButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                selectedCount = counts[index]
                updateCountButtonSelection(countButtons, index)
            }
        }

        // Start Quiz button
        binding.startQuizButton.setOnClickListener {
            loadQuestions()
        }

        // Bottom navigation
        binding.historyButton.setOnClickListener { showScreen("history") }
        binding.achievementsButton.setOnClickListener { showScreen("achievements") }
        binding.settingsButton.setOnClickListener { showScreen("settings") }
    }

    private fun updateCountButtonSelection(buttons: List<com.google.android.material.button.MaterialButton>, selectedIndex: Int) {
        buttons.forEachIndexed { index, button ->
            if (index == selectedIndex) {
                button.setBackgroundColor(getColor(R.color.primary_pink))
                button.setTextColor(Color.WHITE)
            } else {
                button.setBackgroundColor(Color.TRANSPARENT)
                button.setTextColor(getColor(R.color.chocolate_brown))
            }
        }
    }

    private fun refreshHomeStats() {
        val bestScore = prefs.getBestScore()
        val bestTotal = prefs.getBestTotal()
        binding.homeBestScore.text = if (bestTotal > 0) "$bestScore / $bestTotal" else "0 / 0"
        binding.homeQuizzesPlayed.text = prefs.getQuizzesPlayed().toString()
    }

    // ==================== QUIZ SCREEN ====================

    private fun setupQuizScreen() {
        // Answer buttons
        val answerButtons = listOf(binding.answerButton1, binding.answerButton2, binding.answerButton3, binding.answerButton4)
        answerButtons.forEachIndexed { index, button ->
            button.setOnClickListener { handleAnswer(index) }
        }

        // Navigation
        binding.previousButton.setOnClickListener { navigatePrevious() }
        binding.nextButton.setOnClickListener { navigateNext() }

        // Flag button
        binding.flagButton.setOnClickListener { toggleFlag() }

        // Back from quiz (go home)
        binding.quizBackButton.setOnClickListener {
            cancelTimer()
            showScreen("home")
        }

        // Lifelines
        binding.lifelineFiftyFifty.setOnClickListener { useFiftyFifty() }
        binding.lifelineSkip.setOnClickListener { useSkip() }
        binding.lifelineAddTime.setOnClickListener { useAddTime() }
    }

    // ==================== RESULT SCREEN ====================

    private fun setupResultScreen() {
        binding.reviewButton.setOnClickListener { showScreen("review") }
        binding.resultFlaggedButton.setOnClickListener { showScreen("flagged") }
        binding.playAgainButton.setOnClickListener { loadQuestions() }
        binding.homeButton.setOnClickListener {
            cancelTimer()
            showScreen("home")
        }
    }

    // ==================== SECONDARY SCREENS ====================

    private fun setupSecondaryScreens() {
        // History
        binding.historyBackButton.setOnClickListener { showScreen("home") }
        binding.clearHistoryButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage(getString(R.string.confirm_clear))
                .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                    prefs.clearHistory()
                    populateHistory()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        // Achievements
        binding.achievementsBackButton.setOnClickListener { showScreen("home") }

        // Settings
        binding.settingsBackButton.setOnClickListener { showScreen("home") }
        setupSettings()

        // Review
        binding.reviewBackButton.setOnClickListener { showScreen("result") }

        // Flagged questions
        binding.flaggedBackButton.setOnClickListener { showScreen("quiz") }
    }

    private fun setupSettings() {
        // Load current settings
        binding.soundSwitch.isChecked = prefs.isSoundEnabled()
        binding.vibrationSwitch.isChecked = prefs.isVibrationEnabled()
        binding.timerSwitch.isChecked = prefs.isTimerEnabled()

        // Timer duration buttons
        val durationButtons = listOf(binding.durationBtn10, binding.durationBtn15, binding.durationBtn20, binding.durationBtn30)
        val durations = listOf(10, 15, 20, 30)
        updateDurationSelection(durationButtons, durations.indexOf(prefs.getTimerDuration()).coerceAtLeast(0))

        durationButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                prefs.setTimerDuration(durations[index])
                updateDurationSelection(durationButtons, index)
            }
        }

        // Toggle listeners
        binding.soundSwitch.setOnCheckedChangeListener { _, isChecked -> prefs.setSoundEnabled(isChecked) }
        binding.vibrationSwitch.setOnCheckedChangeListener { _, isChecked -> prefs.setVibrationEnabled(isChecked) }
        binding.timerSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.setTimerEnabled(isChecked)
            binding.timerDurationCard.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Show/hide duration card based on timer setting
        binding.timerDurationCard.visibility = if (prefs.isTimerEnabled()) View.VISIBLE else View.GONE
    }

    private fun updateDurationSelection(buttons: List<com.google.android.material.button.MaterialButton>, selectedIndex: Int) {
        buttons.forEachIndexed { index, button ->
            if (index == selectedIndex) {
                button.setBackgroundColor(getColor(R.color.primary_pink))
                button.setTextColor(Color.WHITE)
            } else {
                button.setBackgroundColor(Color.TRANSPARENT)
                button.setTextColor(getColor(R.color.chocolate_brown))
            }
        }
    }

    // ==================== SCREEN NAVIGATION ====================

    private fun showScreen(screen: String) {
        // Hide all groups
        val allGroups = listOf(
            binding.homeGroup, binding.loadingGroup, binding.errorGroup,
            binding.quizGroup, binding.resultGroup, binding.reviewGroup,
            binding.historyGroup, binding.achievementsGroup, binding.settingsGroup,
            binding.flaggedGroup
        )
        allGroups.forEach { it.visibility = View.GONE }

        // Show requested group
        when (screen) {
            "home" -> {
                binding.homeGroup.visibility = View.VISIBLE
                refreshHomeStats()
            }
            "loading" -> binding.loadingGroup.visibility = View.VISIBLE
            "error" -> binding.errorGroup.visibility = View.VISIBLE
            "quiz" -> {
                binding.quizGroup.visibility = View.VISIBLE
                displayQuestion()
            }
            "result" -> {
                binding.resultGroup.visibility = View.VISIBLE
                displayResult()
            }
            "review" -> {
                binding.reviewGroup.visibility = View.VISIBLE
                populateReview()
            }
            "history" -> {
                binding.historyGroup.visibility = View.VISIBLE
                populateHistory()
            }
            "achievements" -> {
                binding.achievementsGroup.visibility = View.VISIBLE
                populateAchievements()
            }
            "settings" -> binding.settingsGroup.visibility = View.VISIBLE
            "flagged" -> {
                binding.flaggedGroup.visibility = View.VISIBLE
                populateFlagged()
            }
        }
    }

    // ==================== LOAD QUESTIONS ====================

    /**
     * Loads trivia questions from the API.
     * Checks Internet connectivity first, then fetches via repository.
     *
     * Demonstrates:
     * - Network connectivity check using NetworkUtils
     * - lifecycleScope.launch for coroutines
     * - Loading and Error state management
     */
    private fun loadQuestions() {
        showScreen("loading")

        // Check Internet connectivity BEFORE attempting network request
        if (!NetworkUtils.isInternetAvailable(this)) {
            showErrorState(getString(R.string.no_internet))
            return
        }

        // Fetch questions using Kotlin Coroutines
        // lifecycleScope.launch runs on the Main thread by default
        lifecycleScope.launch {
            when (val result = repository.fetchQuestions(selectedCount, selectedCategory, selectedDifficulty)) {
                is NetworkResult.Success -> {
                    // Questions loaded successfully — initialize quiz
                    initializeQuiz(result.data)
                    showScreen("quiz")
                }
                is NetworkResult.Error -> {
                    showErrorState(result.message)
                }
            }
        }
    }

    /**
     * Initializes quiz state for a new set of questions.
     */
    private fun initializeQuiz(questionList: List<Question>) {
        questions = questionList
        currentIndex = 0
        currentStreak = 0
        bestStreak = 0

        // Create quiz state for each question
        quizStates = questions.mapIndexed { index, _ ->
            QuizState(questionIndex = index)
        }.toMutableList()

        // Reset lifelines
        isFiftyFiftyAvailable = true
        isSkipAvailable = true
        isAddTimeAvailable = true

        // Reset timer
        timeRemaining = prefs.getTimerDuration() * 1000L
        firstAnswerTime = System.currentTimeMillis()
    }

    private fun showErrorState(message: String) {
        showScreen("error")
        binding.errorMessage.text = message
    }

    // ==================== QUIZ DISPLAY ====================

    /**
     * Displays the current question with all its state.
     * Restores previous answer/flag state when navigating.
     */
    private fun displayQuestion() {
        if (questions.isEmpty() || currentIndex !in questions.indices) return

        val question = questions[currentIndex]
        val state = quizStates[currentIndex]

        // Update progress
        binding.questionProgress.text = getString(R.string.question_format, currentIndex + 1, questions.size)

        // Update progress bar
        val progress = ((currentIndex + 1).toFloat() / questions.size * 100).toInt()
        binding.quizProgressBar.max = 100
        binding.quizProgressBar.progress = progress

        // Update stats
        recalculateScore()
        binding.scoreText.text = getString(R.string.score_format, calculateScore())
        binding.streakText.text = getString(R.string.streak_format, currentStreak)
        binding.flaggedCountText.text = getString(R.string.flagged_format, quizStates.count { it.isFlagged })

        // Update flag button visual
        updateFlagButtonVisual(state.isFlagged)

        // Set question text
        binding.questionText.text = question.text

        // Set answer buttons
        val buttons = listOf(binding.answerButton1, binding.answerButton2, binding.answerButton3, binding.answerButton4)
        val answerButtonMap = mapOf(
            0 to binding.answerButton1,
            1 to binding.answerButton2,
            2 to binding.answerButton3,
            3 to binding.answerButton4
        )

        // Find which index maps to which answer
        buttons.forEachIndexed { idx, button ->
            val answerText = question.answers[idx]
            button.text = answerText

            // Check if this answer was eliminated by 50/50
            if (state.eliminatedAnswers.contains(answerText)) {
                button.visibility = View.INVISIBLE
                button.isEnabled = false
            } else {
                button.visibility = View.VISIBLE
                button.isEnabled = !state.isAnswered
            }

            // Restore visual state
            when {
                state.isAnswered && answerText == state.selectedAnswer -> {
                    if (state.isCorrect) {
                        button.setBackgroundColor(getColor(R.color.correct_green))
                        button.setTextColor(Color.WHITE)
                    } else {
                        button.setBackgroundColor(getColor(R.color.wrong_red))
                        button.setTextColor(Color.WHITE)
                    }
                }
                state.isAnswered && answerText == question.correctAnswer -> {
                    button.setBackgroundColor(getColor(R.color.correct_green))
                    button.setTextColor(Color.WHITE)
                }
                state.isSkipped -> {
                    button.setBackgroundColor(Color.TRANSPARENT)
                    button.setTextColor(getColor(R.color.chocolate_brown))
                }
                else -> {
                    button.setBackgroundColor(Color.TRANSPARENT)
                    button.setTextColor(getColor(R.color.chocolate_brown))
                }
            }
        }

        // Show feedback if already answered
        if (state.isAnswered) {
            binding.feedbackText.visibility = View.VISIBLE
            when {
                state.isCorrect -> {
                    binding.feedbackText.text = getString(R.string.correct_feedback)
                    binding.feedbackText.setTextColor(getColor(R.color.correct_green))
                }
                state.isSkipped -> {
                    binding.feedbackText.text = getString(R.string.skipped_label)
                    binding.feedbackText.setTextColor(getColor(R.color.skipped_gray))
                }
                else -> {
                    binding.feedbackText.text = getString(R.string.wrong_feedback) + "\nAnswer: ${question.correctAnswer}"
                    binding.feedbackText.setTextColor(getColor(R.color.wrong_red))
                }
            }
            buttons.forEach { it.isEnabled = false }
        } else {
            binding.feedbackText.visibility = View.GONE
            buttons.forEach { it.isEnabled = true }
        }

        // Update navigation buttons
        binding.previousButton.isEnabled = currentIndex > 0
        binding.previousButton.alpha = if (currentIndex > 0) 1.0f else 0.5f

        if (currentIndex == questions.size - 1) {
            binding.nextButton.text = "Finish"
        } else {
            binding.nextButton.text = getString(R.string.next)
        }

        // Update lifeline button states
        updateLifelineButtons()

        // Start timer if enabled and question not yet answered
        if (!state.isAnswered) {
            startTimer()
        } else {
            cancelTimer()
            binding.timerText.text = "⏱ --"
        }
    }

    // ==================== ANSWER HANDLING ====================

    /**
     * Handles answer selection.
     * Uses QuizState to prevent double-counting when navigating Previous/Next.
     */
    private fun handleAnswer(selectedIndex: Int) {
        val question = questions[currentIndex]
        val state = quizStates[currentIndex]

        // Don't allow re-answering
        if (state.isAnswered) return

        val selectedAnswer = question.answers[selectedIndex]
        val isCorrect = selectedAnswer == question.correctAnswer

        // Record the answer in state
        state.selectedAnswer = selectedAnswer
        state.isAnswered = true
        state.isCorrect = isCorrect
        state.isWrong = !isCorrect

        // Cancel timer
        cancelTimer()

        // Track first answer time for Fast Thinker achievement
        if (firstAnswerTime == 0L) {
            firstAnswerTime = System.currentTimeMillis()
        }

        // Play feedback
        if (isCorrect) {
            currentStreak++
            if (currentStreak > bestStreak) bestStreak = currentStreak
            soundManager.playCorrectSound()
            soundManager.vibrate(true)
        } else {
            currentStreak = 0
            soundManager.playWrongSound()
            soundManager.vibrate(false)
        }

        // Update UI
        displayQuestion()
    }

    /**
     * Handles skip action — marks question as skipped.
     */
    private fun useSkip() {
        if (!isSkipAvailable) return
        val state = quizStates[currentIndex]
        if (state.isAnswered) return

        isSkipAvailable = false
        state.isAnswered = true
        state.isSkipped = true
        state.selectedAnswer = null
        currentStreak = 0

        cancelTimer()
        displayQuestion()
        Toast.makeText(this, getString(R.string.skipped_label), Toast.LENGTH_SHORT).show()
    }

    /**
     * Handles timer expiry — auto-marks question as wrong.
     */
    private fun handleTimeUp() {
        val state = quizStates[currentIndex]
        if (state.isAnswered) return

        state.isAnswered = true
        state.isWrong = true
        state.selectedAnswer = null
        currentStreak = 0

        binding.feedbackText.text = getString(R.string.time_up) + "\nAnswer: ${questions[currentIndex].correctAnswer}"
        binding.feedbackText.setTextColor(getColor(R.color.wrong_red))
        binding.feedbackText.visibility = View.VISIBLE

        displayQuestion()
    }

    // ==================== NAVIGATION ====================

    /**
     * Navigate to the previous question without making a new API request.
     * Restores the previous question's state (answer, flag, 50/50).
     */
    private fun navigatePrevious() {
        if (currentIndex > 0) {
            cancelTimer()
            currentIndex--
            displayQuestion()
        }
    }

    /**
     * Navigate to the next question.
     * On the last question, moves to the result screen.
     */
    private fun navigateNext() {
        if (currentIndex < questions.size - 1) {
            cancelTimer()
            currentIndex++
            displayQuestion()
        } else {
            // Last question — show result
            cancelTimer()
            finishQuiz()
        }
    }

    // ==================== FLAG ====================

    private fun toggleFlag() {
        val state = quizStates[currentIndex]
        state.isFlagged = !state.isFlagged
        updateFlagButtonVisual(state.isFlagged)
        binding.flaggedCountText.text = getString(R.string.flagged_format, quizStates.count { it.isFlagged })
    }

    private fun updateFlagButtonVisual(isFlagged: Boolean) {
        if (isFlagged) {
            binding.flagButton.text = "🚩"
            binding.flagButton.setBackgroundColor(getColor(R.color.flag_orange))
            binding.flagButton.setTextColor(Color.WHITE)
        } else {
            binding.flagButton.text = "🚩"
            binding.flagButton.setBackgroundColor(Color.TRANSPARENT)
            binding.flagButton.setTextColor(getColor(R.color.chocolate_brown))
        }
    }

    // ==================== LIFELINES ====================

    private fun updateLifelineButtons() {
        binding.lifelineFiftyFifty.isEnabled = isFiftyFiftyAvailable
        binding.lifelineFiftyFifty.alpha = if (isFiftyFiftyAvailable) 1.0f else 0.4f

        binding.lifelineSkip.isEnabled = isSkipAvailable
        binding.lifelineSkip.alpha = if (isSkipAvailable) 1.0f else 0.4f

        binding.lifelineAddTime.isEnabled = isAddTimeAvailable
        binding.lifelineAddTime.alpha = if (isAddTimeAvailable) 1.0f else 0.4f
    }

    /**
     * 50/50 Lifeline — removes two incorrect answers, leaving correct + one wrong.
     */
    private fun useFiftyFifty() {
        if (!isFiftyFiftyAvailable) return
        val state = quizStates[currentIndex]
        if (state.isAnswered || state.isFiftyFiftyUsed) return

        isFiftyFiftyAvailable = false
        state.isFiftyFiftyUsed = true

        val question = questions[currentIndex]
        val incorrectAnswers = question.answers.filter { it != question.correctAnswer }

        // Randomly remove 2 of the 3 incorrect answers
        val toRemove = incorrectAnswers.shuffled().take(2)
        state.eliminatedAnswers = toRemove

        displayQuestion()
    }

    /**
     * +10s Lifeline — adds 10 seconds to the current timer.
     */
    private fun useAddTime() {
        if (!isAddTimeAvailable || !prefs.isTimerEnabled()) return
        isAddTimeAvailable = false

        // Add 10 seconds
        timeRemaining += 10000L
        cancelTimer()
        startTimerWithRemaining()

        displayQuestion()
        updateLifelineButtons()
    }

    // ==================== TIMER ====================

    private fun startTimer() {
        cancelTimer()
        if (!prefs.isTimerEnabled()) {
            binding.timerText.text = "⏱ --"
            return
        }

        timeRemaining = prefs.getTimerDuration() * 1000L
        startTimerWithRemaining()
    }

    private fun startTimerWithRemaining() {
        binding.timerText.text = "⏱ ${timeRemaining / 1000}s"

        countDownTimer = object : CountDownTimer(timeRemaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining = millisUntilFinished
                binding.timerText.text = "⏱ ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                timeRemaining = 0
                isTimerRunning = false
                binding.timerText.text = "⏱ 0s"
                handleTimeUp()
            }
        }.start()
        isTimerRunning = true
    }

    private fun cancelTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        isTimerRunning = false
    }

    // ==================== SCORE CALCULATION ====================

    /**
     * Recalculates score from quiz states — prevents double-counting.
     * Score is always calculated from the stored question states, not incremented on click.
     */
    private fun calculateScore(): Int {
        return quizStates.count { it.isCorrect }
    }

    private fun recalculateScore() {
        // Recalculate current streak from states
        currentStreak = 0
        for (i in 0..currentIndex) {
            if (quizStates[i].isCorrect) {
                currentStreak++
            } else {
                currentStreak = 0
            }
        }
    }

    // ==================== FINISH QUIZ ====================

    private fun finishQuiz() {
        val correct = quizStates.count { it.isCorrect }
        val wrong = quizStates.count { it.isWrong }
        val skipped = quizStates.count { it.isSkipped }
        val flagged = quizStates.count { it.isFlagged }

        // Save to history
        val catName = binding.categorySpinner.selectedItem?.toString() ?: "Any Category"
        val diffName = binding.difficultySpinner.selectedItem?.toString() ?: "Any Difficulty"
        prefs.saveQuizResult(
            category = catName,
            difficulty = diffName,
            totalQuestions = questions.size,
            correct = correct,
            wrong = wrong,
            skipped = skipped,
            bestStreak = bestStreak,
            flagged = flagged
        )

        // Unlock achievements
        unlockAchievements(correct, questions.size, bestStreak)

        // Show result
        showScreen("result")
    }

    private fun displayResult() {
        val correct = quizStates.count { it.isCorrect }
        val wrong = quizStates.count { it.isWrong }
        val skipped = quizStates.count { it.isSkipped }
        val flagged = quizStates.count { it.isFlagged }
        val total = questions.size
        val percentage = if (total > 0) (correct * 100) / total else 0

        binding.resultScoreLarge.text = getString(R.string.score_out_of, correct, total)
        binding.resultPercentage.text = getString(R.string.percentage_format, percentage)
        binding.resultCorrectCount.text = correct.toString()
        binding.resultWrongCount.text = wrong.toString()
        binding.resultSkippedCount.text = skipped.toString()
        binding.resultAccuracy.text = getString(R.string.percentage_format, percentage)
        binding.resultBestStreak.text = bestStreak.toString()
        binding.resultFlaggedCount.text = flagged.toString()
    }

    // ==================== REVIEW SCREEN ====================

    private fun populateReview() {
        binding.reviewListContainer.removeAllViews()

        questions.forEachIndexed { index, question ->
            val state = quizStates[index]

            val card = CardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dpToPx(10) }
                radius = dpToPx(12).toFloat()
                cardElevation = dpToPx(2).toFloat()
                setCardBackgroundColor(Color.WHITE)
            }

            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            }

            // Status header
            val statusText = TextView(this).apply {
                text = when {
                    state.isCorrect -> "✓ CORRECT"
                    state.isSkipped -> "— SKIPPED"
                    else -> "✕ WRONG"
                }
                setTextColor(when {
                    state.isCorrect -> getColor(R.color.correct_green)
                    state.isSkipped -> getColor(R.color.skipped_gray)
                    else -> getColor(R.color.wrong_red)
                })
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
            }
            container.addView(statusText)

            // Question text
            val questionLabel = TextView(this).apply {
                text = question.text
                setTextColor(getColor(R.color.chocolate_brown))
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setPadding(0, dpToPx(8), 0, dpToPx(4))
            }
            container.addView(questionLabel)

            // User's answer
            val userAnswer = TextView(this).apply {
                text = "Your answer: ${state.selectedAnswer ?: getString(R.string.not_answered)}"
                setTextColor(getColor(R.color.warm_brown))
                textSize = 14f
                setPadding(0, dpToPx(4), 0, dpToPx(2))
            }
            container.addView(userAnswer)

            // Correct answer (show if wrong)
            if (!state.isCorrect) {
                val correctLabel = TextView(this).apply {
                    text = "Correct answer: ${question.correctAnswer}"
                    setTextColor(getColor(R.color.correct_green))
                    textSize = 14f
                    setTypeface(null, Typeface.BOLD)
                }
                container.addView(correctLabel)
            }

            card.addView(container)
            binding.reviewListContainer.addView(card)
        }
    }

    // ==================== FLAGGED QUESTIONS ====================

    private fun populateFlagged() {
        binding.flaggedListContainer.removeAllViews()
        val flaggedStates = quizStates.filter { it.isFlagged }

        if (flaggedStates.isEmpty()) {
            binding.flaggedEmptyText.visibility = View.VISIBLE
            return
        }

        binding.flaggedEmptyText.visibility = View.GONE

        flaggedStates.forEach { state ->
            val question = questions[state.questionIndex]

            val card = CardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dpToPx(8) }
                radius = dpToPx(12).toFloat()
                cardElevation = dpToPx(2).toFloat()
                setCardBackgroundColor(Color.WHITE)
                isClickable = true
                isFocusable = true
            }

            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            }

            val title = TextView(this).apply {
                text = "🚩 ${getString(R.string.question_number, state.questionIndex + 1)}"
                setTextColor(getColor(R.color.flag_orange))
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
            }
            container.addView(title)

            val qText = TextView(this).apply {
                text = question.text
                setTextColor(getColor(R.color.chocolate_brown))
                textSize = 15f
                maxLines = 2
                setPadding(0, dpToPx(4), 0, 0)
            }
            container.addView(qText)

            card.addView(container)
            card.setOnClickListener {
                currentIndex = state.questionIndex
                showScreen("quiz")
            }

            binding.flaggedListContainer.addView(card)
        }
    }

    // ==================== HISTORY ====================

    private fun populateHistory() {
        binding.historyListContainer.removeAllViews()
        val history = prefs.getQuizHistory()

        if (history.isEmpty()) {
            binding.historyEmptyText.visibility = View.VISIBLE
            return
        }

        binding.historyEmptyText.visibility = View.GONE

        history.forEach { entry ->
            try {
                val obj = org.json.JSONObject(entry)
                val card = CardView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = dpToPx(8) }
                    radius = dpToPx(12).toFloat()
                    cardElevation = dpToPx(2).toFloat()
                    setCardBackgroundColor(Color.WHITE)
                }

                val container = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
                }

                val catText = TextView(this).apply {
                    text = obj.optString("category", "Unknown")
                    setTextColor(getColor(R.color.chocolate_brown))
                    textSize = 15f
                    setTypeface(null, Typeface.BOLD)
                }
                container.addView(catText)

                val diffText = TextView(this).apply {
                    text = obj.optString("difficulty", "Any").replaceFirstChar { it.uppercase() }
                    setTextColor(getColor(R.color.warm_brown))
                    textSize = 13f
                }
                container.addView(diffText)

                val scoreText = TextView(this).apply {
                    val correct = obj.optInt("correct", 0)
                    val total = obj.optInt("totalQuestions", 0)
                    val pct = if (total > 0) (correct * 100) / total else 0
                    text = "$correct / $total  •  $pct%"
                    setTextColor(getColor(R.color.primary_pink))
                    textSize = 16f
                    setTypeface(null, Typeface.BOLD)
                    setPadding(0, dpToPx(4), 0, 0)
                }
                container.addView(scoreText)

                card.addView(container)
                binding.historyListContainer.addView(card)
            } catch (_: Exception) {}
        }
    }

    // ==================== ACHIEVEMENTS ====================

    private fun populateAchievements() {
        binding.achievementsListContainer.removeAllViews()

        val achievements = listOf(
            Triple("first_quiz", getString(R.string.first_quiz_title), getString(R.string.first_quiz_desc)),
            Triple("hot_streak", getString(R.string.hot_streak_title), getString(R.string.hot_streak_desc)),
            Triple("perfect_score", getString(R.string.perfect_score_title), getString(R.string.perfect_score_desc)),
            Triple("quiz_master", getString(R.string.quiz_master_title), getString(R.string.quiz_master_desc)),
            Triple("fast_thinker", getString(R.string.fast_thinker_title), getString(R.string.fast_thinker_desc))
        )

        achievements.forEach { (key, title, desc) ->
            val unlocked = prefs.isAchievementUnlocked(key)

            val card = CardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dpToPx(8) }
                radius = dpToPx(12).toFloat()
                cardElevation = dpToPx(2).toFloat()
                setCardBackgroundColor(Color.WHITE)
            }

            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            }

            val titleView = TextView(this).apply {
                text = "${if (unlocked) "✓" else "🔒"} $title"
                setTextColor(if (unlocked) getColor(R.color.correct_green) else getColor(R.color.text_secondary))
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
            }
            container.addView(titleView)

            val descView = TextView(this).apply {
                text = desc
                setTextColor(getColor(R.color.warm_brown))
                textSize = 13f
                setPadding(0, dpToPx(2), 0, 0)
            }
            container.addView(descView)

            val statusView = TextView(this).apply {
                text = if (unlocked) getString(R.string.unlocked) else getString(R.string.locked)
                setTextColor(if (unlocked) getColor(R.color.correct_green) else getColor(R.color.text_secondary))
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setPadding(0, dpToPx(4), 0, 0)
            }
            container.addView(statusView)

            card.addView(container)
            binding.achievementsListContainer.addView(card)
        }
    }

    private fun unlockAchievements(correct: Int, total: Int, streak: Int) {
        // First Quiz
        prefs.unlockAchievement("first_quiz")

        // Hot Streak (5 in a row)
        if (streak >= 5) prefs.unlockAchievement("hot_streak")

        // Perfect Score
        if (correct == total && total > 0) prefs.unlockAchievement("perfect_score")

        // Quiz Master (10 quizzes)
        if (prefs.getQuizzesPlayed() >= 10) prefs.unlockAchievement("quiz_master")

        // Fast Thinker (answered first question in under 5 seconds)
        val elapsed = System.currentTimeMillis() - firstAnswerTime
        if (elapsed in 1..5000) prefs.unlockAchievement("fast_thinker")
    }

    // ==================== HELPERS ====================

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelTimer()
        soundManager.release()
    }
}
