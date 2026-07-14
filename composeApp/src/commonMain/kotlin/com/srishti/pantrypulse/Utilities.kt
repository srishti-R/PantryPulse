package com.srishti.pantrypulse

import Category
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

object Utilities {

    private val CLEAN_PUNCTUATION_REGEX = Regex("[^a-z0-9\\s]")
    private val DIGIT_REGEX = Regex("\\d+")
    private val WHITESPACE_REGEX = Regex("\\s+")

    private val NUMBER_WORDS = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
        "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
        "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
        "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, "eighteen" to 18,
        "nineteen" to 19, "twenty" to 20, "thirty" to 30, "forty" to 40,
        "fifty" to 50, "sixty" to 60, "seventy" to 70, "eighty" to 80,
        "ninety" to 90, "hundred" to 100, "a" to 1, "an" to 1, "next" to 1, "last" to 1
    )

    private val PAST_KEYWORDS = listOf("ago", "yesterday", "last", "past", "prior", "back", "before")
    private val FUTURE_KEYWORDS = listOf("from", "in", "after", "tomorrow", "next", "later")
    fun getDefaultExpiryDays(selectedCategory: Category): Int {
        return  when (selectedCategory) {
            Category.DAIRY -> 3
            Category.FRUITS_VEG -> 7
            Category.BAKERY -> 5
            Category.MEAT_SEAFOOD -> 3
            Category.PANTRY -> 365
            Category.BEVERAGES -> 30
            Category.FROZEN -> 90
            Category.SNACKS -> 60
            else -> 14
        }
    }

    fun parseRelativeDateFromSpeech(text: String, mode: String): LocalDate {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return if (mode == "expiryDate") today.plus(DatePeriod(days = 14)) else today
        }

        val lowercase = trimmed.lowercase()

        // Fast-path: Common single-word commands that don't need tokenization or regex cleaning
        if (lowercase == "today" || lowercase == "now") {
            return today
        }
        if (lowercase == "tomorrow") {
            return today.plus(DatePeriod(days = 1))
        }
        if (lowercase == "yesterday") {
            return today.plus(DatePeriod(days = -1))
        }

        // Clean and normalize the text using our precompiled regex
        val cleanText = CLEAN_PUNCTUATION_REGEX.replace(lowercase, "")

        // Find any unit of time
        var daysMultiplier = 1
        var hasUnit = false
        if (cleanText.contains("day")) {
            daysMultiplier = 1
            hasUnit = true
        } else if (cleanText.contains("week")) {
            daysMultiplier = 7
            hasUnit = true
        } else if (cleanText.contains("month")) {
            daysMultiplier = 30
            hasUnit = true
        } else if (cleanText.contains("year")) {
            daysMultiplier = 365
            hasUnit = true
        }

        // If no explicit unit is mentioned but a number is parsed, default multiplier to 1
        if (!hasUnit) {
            daysMultiplier = 1
        }

        // Extract quantity
        var quantity = 1 // default to 1 if not specified (e.g. "a week", "next month")

        // Check if there is an explicit digit using precompiled digit regex
        val digitMatch = DIGIT_REGEX.find(cleanText)
        if (digitMatch != null) {
            quantity = digitMatch.value.toIntOrNull() ?: 1
        } else {
            // Look for written number words in the cleanText using precompiled whitespace regex
            val words = cleanText.split(WHITESPACE_REGEX)
            for (word in words) {
                val value = NUMBER_WORDS[word]
                if (value != null) {
                    if (value > 1 || quantity == 1) {
                        quantity = value
                    }
                }
            }
        }

        // Determine past vs future direction
        // Default direction based on mode
        var isFuture = mode == "expiryDate"

        // Check if cleanText contains explicit past/future indicator words using our static lists
        val hasPastIndicator = PAST_KEYWORDS.any { cleanText.contains(it) }
        val hasFutureIndicator = FUTURE_KEYWORDS.any { cleanText.contains(it) }

        if (hasPastIndicator && !hasFutureIndicator) {
            isFuture = false
        } else if (hasFutureIndicator && !hasPastIndicator) {
            isFuture = true
        }

        val finalOffset = quantity * daysMultiplier
        val daysToAdd = if (isFuture) finalOffset else -finalOffset

        return try {
            today.plus(DatePeriod(days = daysToAdd))
        } catch (e: Exception) {
            // Fallback to mode defaults
            if (mode == "expiryDate") today.plus(DatePeriod(days = 14)) else today
        }
    }
}
