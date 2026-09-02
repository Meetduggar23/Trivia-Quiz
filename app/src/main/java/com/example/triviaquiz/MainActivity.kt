package com.example.triviaquiz

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.triviaquiz.data.QuizRepository
import com.example.triviaquiz.databinding.ActivityMainBinding
import com.example.triviaquiz.model.Question
import com.example.triviaquiz.network.NetworkResult
import com.example.triviaquiz.network.NetworkUtils
import kotlinx.coroutines.launch

/**
 * Main Activity — the single screen for the Trivia Quiz app.
 * Handles four UI states: Loading, Error, Quiz, and Result.
 *
 * Demonstrates:
 * - View Binding (ActivityMainBinding)
 * - Kotlin Coroutines (lifecycleScope.launch)
 * - Network connectivity check before fetching
 * - Loading state with ProgressBar
 * - Error state with Retry
 * - Quiz state with answer validation and score tracking
 * - Result state with Play Again
 */
class MainActivity : AppCompatActivity() {

    // View Binding — replaces findViewById
    private lateinit var binding: ActivityMainBinding

    // Repository for fetching questions (single source of truth)
    private val repository = QuizRepository()

    // Quiz state variables
    private var questions = listOf<Question>()
    private var currentIndex = 0
    private var score = 0

    // Handler for delayed transitions (e.g., showing feedback before next question)
    private val handler = Handler(Looper.getMainLooper())

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

        // Set up button click listeners
        setupAnswerButtons()
        setupRetryButton()
        setupPlayAgainButton()

        // Load questions automatically when the app launches
        loadQuestions()
    }

    /**
     * Sets up the four answer buttons with click listeners.
     * Each button validates the selected answer against the correct answer.
     */
    private fun setupAnswerButtons() {
        val answerButtons = listOf(
            binding.answerButton1,
            binding.answerButton2,
            binding.answerButton3,
            binding.answerButton4
        )

        answerButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                handleAnswer(index)
            }
        }
    }

    /**
     * Sets up the Retry button to re-attempt loading questions.
     */
    private fun setupRetryButton() {
        binding.retryButton.setOnClickListener {
            loadQuestions()
        }
    }

    /**
     * Sets up the Play Again button to fetch fresh questions.
     */
    private fun setupPlayAgainButton() {
        binding.playAgainButton.setOnClickListener {
            loadQuestions()
        }
    }

    /**
     * Loads trivia questions from the API.
     * Checks Internet connectivity first, then fetches via repository.
     *
     * Demonstrates:
     * - Network connectivity check using NetworkUtils
     * - lifecycleScope.launch for coroutines
     * - Loading state management
     * - Error state management
     */
    private fun loadQuestions() {
        // Step 1: Show loading state
        showLoadingState()

        // Step 2: Check Internet connectivity BEFORE attempting network request
        if (!NetworkUtils.isInternetAvailable(this)) {
            showErrorState("No internet connection. Please check your network settings.")
            return
        }

        // Step 3: Fetch questions using Kotlin Coroutines
        // lifecycleScope.launch runs on the Main thread by default
        lifecycleScope.launch {
            when (val result = repository.fetchQuestions()) {
                is NetworkResult.Success -> {
                    // Questions loaded successfully — start the quiz
                    questions = result.data
                    currentIndex = 0
                    score = 0
                    showQuizState()
                }
                is NetworkResult.Error -> {
                    // Network or parsing error — show error screen
                    showErrorState(result.message)
                }
            }
        }
    }

    /**
     * Shows the loading state with a progress spinner.
     */
    private fun showLoadingState() {
        binding.loadingGroup.visibility = View.VISIBLE
        binding.errorGroup.visibility = View.GONE
        binding.quizGroup.visibility = View.GONE
        binding.resultGroup.visibility = View.GONE
    }

    /**
     * Shows the error state with a friendly message and Retry button.
     *
     * @param message The error message to display
     */
    private fun showErrorState(message: String) {
        binding.loadingGroup.visibility = View.GONE
        binding.errorGroup.visibility = View.VISIBLE
        binding.quizGroup.visibility = View.GONE
        binding.resultGroup.visibility = View.GONE
        binding.errorMessage.text = message
    }

    /**
     * Shows the quiz state with the current question and answer buttons.
     */
    private fun showQuizState() {
        binding.loadingGroup.visibility = View.GONE
        binding.errorGroup.visibility = View.GONE
        binding.quizGroup.visibility = View.VISIBLE
        binding.resultGroup.visibility = View.GONE

        displayQuestion()
    }

    /**
     * Shows the result state with the final score and Play Again button.
     */
    private fun showResultState() {
        binding.loadingGroup.visibility = View.GONE
        binding.errorGroup.visibility = View.GONE
        binding.quizGroup.visibility = View.GONE
        binding.resultGroup.visibility = View.VISIBLE

        binding.finalScoreText.text = "You scored $score out of ${questions.size}"
    }

    /**
     * Displays the current question and its four answer options.
     */
    private fun displayQuestion() {
        val question = questions[currentIndex]

        // Update progress text: "Question 1 of 10"
        binding.questionProgress.text = "QUESTION ${currentIndex + 1} OF ${questions.size}"

        // Set the question text
        binding.questionText.text = question.text

        // Set the four answer button labels
        binding.answerButton1.text = question.answers[0]
        binding.answerButton2.text = question.answers[1]
        binding.answerButton3.text = question.answers[2]
        binding.answerButton4.text = question.answers[3]

        // Update score display
        binding.scoreText.text = "Score: $score"

        // Reset feedback text
        binding.feedbackText.visibility = View.GONE

        // Re-enable all answer buttons
        setAnswerButtonsEnabled(true)

        // Reset button colors to default outlined style
        resetButtonColors()
    }

    /**
     * Handles when the user selects an answer.
     * Validates the answer, updates score, shows feedback, then advances.
     *
     * @param selectedIndex The index of the selected answer (0-3)
     */
    private fun handleAnswer(selectedIndex: Int) {
        val question = questions[currentIndex]
        val selectedAnswer = question.answers[selectedIndex]
        val isCorrect = selectedAnswer == question.correctAnswer

        // Disable buttons to prevent double-tapping
        setAnswerButtonsEnabled(false)

        if (isCorrect) {
            // Correct answer — increment score and show feedback
            score++
            binding.scoreText.text = "Score: $score"
            binding.feedbackText.text = "Correct! ✓"
            binding.feedbackText.setTextColor(getColor(R.color.correct_green))

            // Highlight the correct button in green
            highlightButton(selectedIndex, isCorrect = true)
        } else {
            // Wrong answer — show feedback with the correct answer
            binding.feedbackText.text = "Wrong! ✗\nThe answer was: ${question.correctAnswer}"
            binding.feedbackText.setTextColor(getColor(R.color.wrong_red))

            // Highlight selected button in red, correct button in green
            highlightButton(selectedIndex, isCorrect = false)
            highlightCorrectButton(question.correctAnswer)
        }

        binding.feedbackText.visibility = View.VISIBLE

        // Delay before moving to the next question (so user can see feedback)
        handler.postDelayed({
            currentIndex++
            if (currentIndex < questions.size) {
                // More questions — show next one
                displayQuestion()
            } else {
                // All questions answered — show result
                showResultState()
            }
        }, 1500)
    }

    /**
     * Highlights an answer button as correct (green) or wrong (red).
     */
    private fun highlightButton(index: Int, isCorrect: Boolean) {
        val button = when (index) {
            0 -> binding.answerButton1
            1 -> binding.answerButton2
            2 -> binding.answerButton3
            3 -> binding.answerButton4
            else -> return
        }

        if (isCorrect) {
            button.setBackgroundColor(getColor(R.color.correct_green))
            button.setTextColor(getColor(android.R.color.white))
        } else {
            button.setBackgroundColor(getColor(R.color.wrong_red))
            button.setTextColor(getColor(android.R.color.white))
        }
    }

    /**
     * Finds and highlights the button containing the correct answer text.
     */
    private fun highlightCorrectButton(correctAnswer: String) {
        val buttons = listOf(
            binding.answerButton1,
            binding.answerButton2,
            binding.answerButton3,
            binding.answerButton4
        )
        buttons.forEach { button ->
            if (button.text.toString() == correctAnswer) {
                button.setBackgroundColor(getColor(R.color.correct_green))
                button.setTextColor(getColor(android.R.color.white))
            }
        }
    }

    /**
     * Resets all answer buttons to the default outlined style.
     */
    private fun resetButtonColors() {
        val buttons = listOf(
            binding.answerButton1,
            binding.answerButton2,
            binding.answerButton3,
            binding.answerButton4
        )
        buttons.forEach { button ->
            button.setBackgroundColor(getColor(android.R.color.transparent))
            button.setTextColor(getColor(R.color.chocolate_brown))
        }
    }

    /**
     * Enables or disables all answer buttons.
     */
    private fun setAnswerButtonsEnabled(enabled: Boolean) {
        binding.answerButton1.isEnabled = enabled
        binding.answerButton2.isEnabled = enabled
        binding.answerButton3.isEnabled = enabled
        binding.answerButton4.isEnabled = enabled
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up handler callbacks to prevent memory leaks
        handler.removeCallbacksAndMessages(null)
    }
}
