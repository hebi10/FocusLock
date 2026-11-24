package com.haebi.focuslock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.haebi.focuslock.data.GoalAlertNavigation
import com.haebi.focuslock.ui.FocusLockApp

class MainActivity : ComponentActivity() {
    private var pendingGoalRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingGoalRoute = if (savedInstanceState == null) GoalAlertNavigation.target(intent)
            else savedInstanceState.getString("pending_goal_route")
        enableEdgeToEdge()
        setContent { FocusLockApp(pendingGoalRoute) { pendingGoalRoute = null } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        GoalAlertNavigation.target(intent)?.let { pendingGoalRoute = it }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("pending_goal_route", pendingGoalRoute)
        super.onSaveInstanceState(outState)
    }
}
