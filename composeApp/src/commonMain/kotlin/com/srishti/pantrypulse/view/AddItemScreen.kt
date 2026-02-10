package com.srishti.pantrypulse.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.srishti.pantrypulse.model.Category
import com.srishti.pantrypulse.db.PantryItem
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import pantrypulse.composeapp.generated.resources.Res
import pantrypulse.composeapp.generated.resources.checkbox_selected_text
import pantrypulse.composeapp.generated.resources.checkbox_unselected_text
import kotlin.time.Instant


@Composable
fun AddItemScreenStateful(rootNavController: NavController) {
//    TODO add and observe for viewmodel states here
//    TODO refer https://proandroiddev.com/room-in-kotlin-multiplatform-kmp-with-koin-d7716bdd8783
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddItemScreenStateless(
    categories: List<Category>,
    onSave: (PantryItem) -> Unit
) {

    var productName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.NA) }
    var expiryDate by remember { mutableStateOf<LocalDate?>(null) }
    var buyDate by remember { mutableStateOf<LocalDate?>(null) }

    var showPicker by remember { mutableStateOf(false) }
    var pickingForExpiry by remember { mutableStateOf(true) }

    // The Single Date Picker Instance
    MaterialKmpDatePicker(
        show = showPicker,
        onDismiss = { showPicker = false },
        onDateSelected = { date ->
            if (pickingForExpiry) expiryDate = date else buyDate = date
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

//        TODO add method to scan a product and figure out name, category and expiry date from camera pic,
//        TODO buy date being current date, with the option to change it
        OutlinedTextField(
            value = productName,
            onValueChange = { productName = it },
            label = { Text("Product Name") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.DocumentScanner, contentDescription = null) }
        )

        // CATEGORY CHIPS
        Text("Category", style = MaterialTheme.typography.labelLarge)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category.name) }
                )
            }
        }

        // EXPIRY DATE
        DateField(
            label = "Expiry Date",
            date = expiryDate,
            onClick = {
                pickingForExpiry = true
                showPicker = true
            }
        )

        // BUY DATE
        DateField(
            label = "Buy Date",
            date = buyDate,
            onClick = {
                pickingForExpiry = false
                showPicker = true
            }
        )

        // SAVE BUTTON
        Button(
            onClick = {
                if (productName.isNotBlank()) {
                    onSave(
                        PantryItem(
                            name = productName,
                            category = selectedCategory,
                            expiryDate = expiryDate,
                            buyDate = buyDate
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Item")
        }
    }
}

@Composable
fun DateField(label: String, date: LocalDate?, onClick: () -> Unit) {
    // Use readOnly = true and a Box to ensure the whole area is clickable
    OutlinedTextField(
        value = date?.toString() ?: "",
        onValueChange = {},
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, // This works better with readOnly
        label = { Text(label) },
        readOnly = true,
        trailingIcon = {
            DateFieldTrailingIcons(
                onMicClick = {
//                    TODO add functionality to process voice commands for date
                },
                onCalendarClick = { onClick() })
        }
    )
}

@Composable
fun DateFieldTrailingIcons(onMicClick: () -> Unit = {}, onCalendarClick: () -> Unit = {}) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .padding(8.dp)
    ) {
        Icon(
            Icons.Default.DateRange,
            modifier = Modifier
                .clickable { onCalendarClick() },
            contentDescription = null)
        Spacer(modifier = Modifier.padding(2.dp))
        Icon(
            Icons.Default.Mic, modifier = Modifier
                .clickable { onMicClick() },
            contentDescription = null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialKmpDatePicker(
    show: Boolean,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val state = rememberDatePickerState()

    if (show) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            // Convert millis to LocalDate
                            val date = kotlinx.datetime.Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.UTC).date
                            onDateSelected(date)
                        }
                        onDismiss()
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun RemindMeCheckbox() {
//    TODO add geofences in subsequent versions to remind to refill when I am near a shop selling the category
    var checked by remember { mutableStateOf(true) }
    Checkbox(
        checked = checked,
        onCheckedChange = { checked = it }
    )
    Text(
        if (checked) {
            stringResource(Res.string.checkbox_selected_text)
        } else {
            stringResource(Res.string.checkbox_unselected_text)
        }
    )
}


@Preview
@Composable
fun AddItemScreenStatelessPreview() {
    AddItemScreenStateless(
        categories = listOf(Category.DRY_GOODS, Category.DAIRY, Category.FAST_CONSUMABLE),
        onSave = {}
    )
}