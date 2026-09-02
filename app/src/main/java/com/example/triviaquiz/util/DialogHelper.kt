package com.example.triviaquiz.util

import android.app.AlertDialog
import android.content.Context

/**
 * Helper for showing consistent AlertDialogs throughout the app.
 */
object DialogHelper {

    fun showLeaveQuiz(
        context: Context,
        onContinueQuiz: () -> Unit,
        onExitQuiz: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle("Leave Quiz?")
            .setMessage("Your progress will be saved.")
            .setPositiveButton("Exit Quiz") { _, _ -> onExitQuiz() }
            .setNegativeButton("Continue Quiz") { _, _ -> onContinueQuiz() }
            .setCancelable(false)
            .show()
    }

    fun showResumeQuiz(
        context: Context,
        category: String,
        difficulty: String,
        completed: Int,
        total: Int,
        onResume: () -> Unit,
        onStartNew: () -> Unit
    ) {
        val message = "$category • $difficulty\n$completed / $total completed"
        AlertDialog.Builder(context)
            .setTitle("Quiz in Progress")
            .setMessage("You have an unfinished quiz.\n\n$message")
            .setPositiveButton("Resume Quiz") { _, _ -> onResume() }
            .setNegativeButton("Start New Quiz") { _, _ -> onStartNew() }
            .setCancelable(false)
            .show()
    }

    fun showConfirm(
        context: Context,
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Confirm") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
