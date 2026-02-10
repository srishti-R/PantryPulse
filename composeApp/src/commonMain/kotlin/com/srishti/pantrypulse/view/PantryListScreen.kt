package com.srishti.pantrypulse.view

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.srishti.pantrypulse.db.PantryItem

@Composable
fun PantryListScreen(rootNavController: NavController) {
//    TODO fetch list of pantry items from room and display here
//    LazyColumn(
//        modifier = Modifier.weight(1f)) {
//        items(pantryItems) { pantryItem ->
//            PantryItem(pantryItem)
//        }
//    }
}

@Composable
fun PantryItem(
    pantryItem: PantryItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Text(
            text = pantryItem.name,
            modifier = Modifier.padding(16.dp)
        )
    }
}