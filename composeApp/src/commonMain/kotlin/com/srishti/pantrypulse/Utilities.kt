package com.srishti.pantrypulse

import Category

object Utilities {
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
}
