package com.srishti.pantrypulse.util

import Category
import kotlinx.datetime.LocalDate

data class OcrResult(
    val expiryDate: LocalDate?,
    val buyDate: LocalDate?,
    val productName: String,
    val category: Category
)

object OcrDateParser {
    private val monthsMap = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
        "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
    )

    private fun findDateInText(text: String): LocalDate? {
        // Try YYYY-MM-DD or YYYY/MM/DD first
        val yyyymmdd = """(?:^|\D)(\d{4})[-\./](\d{1,2})[-\./](\d{1,2})(?:$|\D)""".toRegex().find(text)
        if (yyyymmdd != null) {
            return try {
                LocalDate(yyyymmdd.groupValues[1].toInt(), yyyymmdd.groupValues[2].toInt(), yyyymmdd.groupValues[3].toInt())
            } catch (e: Exception) { null }
        }

        // Try DD-MM-YYYY or DD/MM/YYYY
        val ddmmyyyy = """(?:^|\D)(\d{1,2})[-\./](\d{1,2})[-\./](\d{4})(?:$|\D)""".toRegex().find(text)
        if (ddmmyyyy != null) {
            return try {
                val day = ddmmyyyy.groupValues[1].toInt()
                val month = ddmmyyyy.groupValues[2].toInt()
                val year = ddmmyyyy.groupValues[3].toInt()
                if (month in 1..12 && day in 1..31) {
                    LocalDate(year, month, day)
                } else null
            } catch (e: Exception) { null }
        }

        // Try DD-MM-YY or DD/MM/YY (2-digit year)
        val ddmmyy = """(?:^|\D)(\d{1,2})[-\./](\d{1,2})[-\./](\d{2})(?:$|\D)""".toRegex().find(text)
        if (ddmmyy != null) {
            try {
                val day = ddmmyy.groupValues[1].toInt()
                val month = ddmmyy.groupValues[2].toInt()
                val year2Digit = ddmmyy.groupValues[3].toInt()
                if (month in 1..12 && day in 1..31 && year2Digit in 20..40) {
                    val year = 2000 + year2Digit
                    return LocalDate(year, month, day)
                }
            } catch (e: Exception) { /* ignore */ }
        }

        // Try Month Name patterns: DD Month YYYY or Month DD YYYY
        val ddMonthYyyy = """(?:^|\D)(\d{1,2})\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+(\d{4}|\d{2})(?:$|\D)""".toRegex(RegexOption.IGNORE_CASE).find(text)
        if (ddMonthYyyy != null) {
            try {
                val day = ddMonthYyyy.groupValues[1].toInt()
                val monthStr = ddMonthYyyy.groupValues[2].lowercase().take(3)
                val month = monthsMap[monthStr] ?: 1
                var year = ddMonthYyyy.groupValues[3].toInt()
                if (year < 100) year += 2000
                return LocalDate(year, month, day)
            } catch (e: Exception) { /* ignore */ }
        }

        val monthDdYyyy = """(?:^|\D)(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+(\d{1,2})\s*,?\s*(\d{4}|\d{2})(?:$|\D)""".toRegex(RegexOption.IGNORE_CASE).find(text)
        if (monthDdYyyy != null) {
            try {
                val monthStr = monthDdYyyy.groupValues[1].lowercase().take(3)
                val month = monthsMap[monthStr] ?: 1
                val day = monthDdYyyy.groupValues[2].toInt()
                var year = monthDdYyyy.groupValues[3].toInt()
                if (year < 100) year += 2000
                return LocalDate(year, month, day)
            } catch (e: Exception) { /* ignore */ }
        }

        // Try MM/YY or MM-YY or MM.YY (e.g. 12/26 or 08-25)
        val mmyy = """(?:^|\D)(\d{1,2})[-\./](\d{2})(?:$|\D)""".toRegex().find(text)
        if (mmyy != null) {
            try {
                val month = mmyy.groupValues[1].toInt()
                val year2Digit = mmyy.groupValues[2].toInt()
                if (month in 1..12 && year2Digit in 20..40) {
                    val year = 2000 + year2Digit
                    return LocalDate(year, month, 1)
                }
            } catch (e: Exception) { /* ignore */ }
        }

        return null
    }

    private fun scoreLineForProductName(line: String): Int {
        val lineLower = line.lowercase().trim()
        if (lineLower.isEmpty()) return -100

        // Reject lines that match explicit date patterns or are primarily numbers/dates
        val hasDatePattern = """\b\d{4}[-/.]\d{1,2}[-/.]\d{1,2}\b""".toRegex().containsMatchIn(line) ||
                """\b\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4}\b""".toRegex().containsMatchIn(line) ||
                """\b\d{1,2}\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\b""".toRegex(RegexOption.IGNORE_CASE).containsMatchIn(line) ||
                """\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{1,2}\b""".toRegex(RegexOption.IGNORE_CASE).containsMatchIn(line)
        if (hasDatePattern) {
            return -100 // Strongly disqualified
        }

        // Explicitly disqualify lines containing date keywords or markers
        val forbiddenSubstrings = listOf(
            "best before", "use by", "packaged on", "packaged", "expiry", "expiration",
            "manufactured", "invoice", "receipt", "billing", "nutrition", "facts", "serving",
            "calories", "ingredients", "sodium", "protein", "cholesterol", "keep refrigerated",
            "distributor", "product of", "net wt", "weight"
        )
        val forbiddenWords = listOf("exp", "bb", "bbd", "mfg", "mfd", "prod", "date", "before", "pkg")

        val words = lineLower.split(Regex("[^a-zA-Z]+")).filter { it.isNotEmpty() }
        val hasForbiddenWord = words.any { it in forbiddenWords }
        val hasForbiddenSubstring = forbiddenSubstrings.any { lineLower.contains(it) }

        if (hasForbiddenWord || hasForbiddenSubstring) {
            return -100 // Strongly disqualified
        }

        // Must have at least 3 letters
        val lettersCount = line.count { it.isLetter() }
        if (lettersCount < 3) return -100

        // Must have vowels (must be a pronounceable/real word representation)
        val hasVowels = lineLower.any { it in "aeiouy" }
        if (!hasVowels) return -50

        // Disqualify if it has too many digits (like barcodes or serial codes)
        val digitsCount = line.count { it.isDigit() }
        if (digitsCount > lettersCount) return -100

        var score = 0
        if (digitsCount > 0) {
            score -= (digitsCount * 3) // Penalize digits but allow things like "1L" or "500g"
        }

        // Word count sweet spot (product names are typically 2 to 4 words)
        val wordCount = words.size
        when {
            wordCount in 2..4 -> score += 35
            wordCount == 1 -> score += 5  // Single words are okay but less ideal
            wordCount > 4 -> score -= 15  // Likely a sentence or disclaimer text
        }

        // Capitalization bonus (Brands / Products are usually capitalized)
        val originalWords = line.split(Regex("[^a-zA-Z]+")).filter { it.isNotEmpty() }
        val capitalizedWords = originalWords.count { it.firstOrNull()?.isUpperCase() == true }
        if (capitalizedWords >= 1) {
            score += 15
        }

        // Length guidelines
        if (line.length in 8..25) {
            score += 15
        } else if (line.length < 5 || line.length > 35) {
            score -= 15
        }

        // High priority food keywords
        val foodKeywords = listOf(
            "milk", "juice", "soup", "bread", "cheese", "yogurt", "cereal", "water", "sauce", "pasta", "rice",
            "beef", "chicken", "pork", "fish", "apple", "banana", "tomato", "potato", "soda", "coffee", "tea",
            "butter", "cream", "bacon", "egg", "chips", "cookies", "snack", "chocolate", "candy", "oil", "salt",
            "sugar", "flour", "honey", "jam", "organic", "fresh", "classic", "natural", "original", "premium"
        )
        val hasFoodKeyword = foodKeywords.any { lineLower.contains(it) }
        if (hasFoodKeyword) {
            score += 40
        }

        return score
    }

    fun parse(ocrText: String, mode: String = "productName"): OcrResult? {
        if (ocrText.isBlank()) return null

        val lowerText = ocrText.lowercase()

        when (mode) {
            "productName" -> {
                val lines = ocrText.lines().map { it.trim() }.filter { it.isNotEmpty() }

                var bestLine: String? = null
                var highestScore = -999

                for (line in lines) {
                    val score = scoreLineForProductName(line)
                    if (score > highestScore) {
                        highestScore = score
                        bestLine = line
                    }
                }

                // Require a solid minimum score threshold to consider it a valid product name.
                // This stops the UI from jumping around with random single-letter OCR noise.
                if (bestLine == null || highestScore < 15) {
                    return null
                }

                var category = Category.OTHER
                when {
                    lowerText.contains("milk") || lowerText.contains("cheese") || lowerText.contains("butter") || lowerText.contains("dairy") || lowerText.contains("yogurt") -> {
                        category = Category.DAIRY
                    }
                    lowerText.contains("soup") || lowerText.contains("can") || lowerText.contains("sauce") || lowerText.contains("pasta") || lowerText.contains("rice") -> {
                        category = Category.PANTRY
                    }
                    lowerText.contains("bread") || lowerText.contains("bun") || lowerText.contains("bakery") || lowerText.contains("loaf") || lowerText.contains("croissant") -> {
                        category = Category.BAKERY
                    }
                    lowerText.contains("beef") || lowerText.contains("chicken") || lowerText.contains("meat") || lowerText.contains("pork") || lowerText.contains("fish") || lowerText.contains("seafood") -> {
                        category = Category.MEAT_SEAFOOD
                    }
                    lowerText.contains("apple") || lowerText.contains("banana") || lowerText.contains("tomato") || lowerText.contains("potato") || lowerText.contains("vegetable") || lowerText.contains("fruit") -> {
                        category = Category.FRUITS_VEG
                    }
                    lowerText.contains("juice") || lowerText.contains("soda") || lowerText.contains("beverage") || lowerText.contains("water") || lowerText.contains("coffee") || lowerText.contains("tea") -> {
                        category = Category.BEVERAGES
                    }
                    lowerText.contains("frozen") || lowerText.contains("ice cream") || lowerText.contains("pizza") -> {
                        category = Category.FROZEN
                    }
                    lowerText.contains("chip") || lowerText.contains("cookie") || lowerText.contains("snack") || lowerText.contains("chocolate") || lowerText.contains("candy") -> {
                        category = Category.SNACKS
                    }
                }

                return OcrResult(expiryDate = null, buyDate = null, productName = bestLine, category = category)
            }
            "expiryDate" -> {
                val expWords = listOf("exp", "expiry", "bb", "bbd", "useby", "before")
                val expSubstrings = listOf("best before", "use by", "expiry date", "expiration date", "expiration")
                var detectedDate: LocalDate? = null

                val lines = ocrText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                for (line in lines) {
                    val hasExpWord = expWords.any { line.contains(it, ignoreCase = true) }
                    val hasExpSubstring = expSubstrings.any { line.contains(it, ignoreCase = true) }

                    if (hasExpWord || hasExpSubstring) {
                        val possibleDate = findDateInText(line)
                        if (possibleDate != null) {
                            detectedDate = possibleDate
                            break
                        }
                    }
                }

                if (detectedDate == null) {
                    val hasAnyIndicator = expSubstrings.any { ocrText.contains(it, ignoreCase = true) } ||
                            expWords.any { ocrText.contains(it, ignoreCase = true) }
                    if (hasAnyIndicator) {
                        detectedDate = findDateInText(ocrText)
                    }
                }

                if (detectedDate != null) {
                    return OcrResult(expiryDate = detectedDate, buyDate = null, productName = "", category = Category.NA)
                }
                return null
            }
            "buyDate" -> {
                val mfgWords = listOf("mfg", "mfd", "prod", "date", "mfgdate", "pkg", "bill", "receipt")
                val mfgSubstrings = listOf("packaged on", "packaged", "manufactured on", "manufacturing date", "manufactured", "invoice", "billing")
                var detectedDate: LocalDate? = null

                val lines = ocrText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                for (line in lines) {
                    val hasMfgWord = mfgWords.any { line.contains(it, ignoreCase = true) }
                    val hasMfgSubstring = mfgSubstrings.any { line.contains(it, ignoreCase = true) }

                    if (hasMfgWord || hasMfgSubstring) {
                        val possibleDate = findDateInText(line)
                        if (possibleDate != null) {
                            detectedDate = possibleDate
                            break
                        }
                    }
                }

                if (detectedDate == null) {
                    val hasAnyIndicator = mfgSubstrings.any { ocrText.contains(it, ignoreCase = true) } ||
                            mfgWords.any { ocrText.contains(it, ignoreCase = true) }
                    if (hasAnyIndicator) {
                        detectedDate = findDateInText(ocrText)
                    }
                }

                if (detectedDate != null) {
                    return OcrResult(expiryDate = null, buyDate = detectedDate, productName = "", category = Category.NA)
                }
                return null
            }
            else -> return null
        }
    }
}