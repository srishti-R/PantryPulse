package com.srishti.pantrypulse.view

import com.srishti.pantrypulse.model.getPlatform

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}