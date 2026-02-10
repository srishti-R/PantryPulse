package com.srishti.pantrypulse.model

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform