package com.srishti.pantrypulse.view

import PantryDao
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srishti.pantrypulse.db.PantryItem
import kotlinx.coroutines.launch

class AddItemViewModel: ViewModel() {

    fun addPantryItem(pantryItem: PantryItem, dao: PantryDao) {
       viewModelScope.launch {
           dao.insertItem(
               pantryItem
           )
       }
    }

}