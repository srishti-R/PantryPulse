package com.srishti.pantrypulse

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.srishti.pantrypulse.view.App

fun MainViewController() = ComposeUIViewController {
    val database = remember { getDatabaseBuilder() }
    App(database)
}