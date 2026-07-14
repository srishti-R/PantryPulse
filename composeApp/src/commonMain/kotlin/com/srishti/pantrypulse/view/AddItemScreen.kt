
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.srishti.pantrypulse.CameraScanner
import com.srishti.pantrypulse.Utilities
import com.srishti.pantrypulse.VoiceCommandOverlay
import com.srishti.pantrypulse.db.PantryItem
import com.srishti.pantrypulse.view.AddItemViewModel
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    pantryDao: PantryDao,
    viewModel: AddItemViewModel = viewModel()
) {
    val scrollState = rememberScrollState()

    var productName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.NA) }
    var expiryDate by remember { mutableStateOf<LocalDate?>(null) }
    var buyDate by remember { mutableStateOf<LocalDate?>(null) }
    var showScanner by remember { mutableStateOf(false) }
    var scannerMode by remember { mutableStateOf("productName") }

    var showPicker by remember { mutableStateOf(false) }
    var pickingForExpiry by remember { mutableStateOf(true) }

    // Geofencing refilling state
    var isGeofenceAlertEnabled by remember { mutableStateOf(true) }

    var activeVoiceField by remember { mutableStateOf<String?>(null) }
    var showVoiceOverlay by remember { mutableStateOf(false) }

    // Platform date picker dialog
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
            .verticalScroll(scrollState)
            .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // PRODUCT NAME INPUT
        OutlinedTextField(
            value = productName,
            onValueChange = { productName = it },
            label = { Text("Product Name") },
            placeholder = { Text("Enter milk, cereal, fruit...") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Row(
                    modifier = Modifier.padding(end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        activeVoiceField = "productName"
                        showVoiceOverlay = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input"
                        )
                    }
                    IconButton(onClick = {
                        scannerMode = "productName"
                        showScanner = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Scan with Camera"
                        )
                    }
                }
            }
        )

        // CATEGORY CHIPS
        Text("Category", style = MaterialTheme.typography.labelLarge)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Category.entries.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category.displayName) }
                )
            }
        }

        // EXPIRY DATE PICKER FIELD
        DateField(
            label = "Expiry Date",
            date = expiryDate,
            onClick = {
                pickingForExpiry = true
                showPicker = true
            },
            onCameraClick = {
                scannerMode = "expiryDate"
                showScanner = true
            },
            onMicClick = {
                activeVoiceField = "expiryDate"
                showVoiceOverlay = true
            }
        )

        // BUY DATE PICKER FIELD
        DateField(
            label = "Buy Date",
            date = buyDate,
            onClick = {
                pickingForExpiry = false
                showPicker = true
            },
            onMicClick = {
                activeVoiceField = "buyDate"
                showVoiceOverlay = true
            },
            onCameraClick = {
                scannerMode = "buyDate"
                showScanner = true
            }
        )

        // REMIND ME ON LOCATION (GEOFENCE CHECKBOX)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isGeofenceAlertEnabled,
                onCheckedChange = { isGeofenceAlertEnabled = it }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Simulate GPS Geofence Reminders", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Alert me to buy this item when passing supermarkets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SAVE BUTTON
        Button(
            onClick = {
                if (productName.isNotBlank()) {
                    val resolvedBuyDate = buyDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault())

                    val finalExpiryDate = expiryDate ?: resolvedBuyDate.plus(DatePeriod(days = Utilities.getDefaultExpiryDays(selectedCategory)))
                    val itemToSave = PantryItem(
                        name = productName,
                        category = selectedCategory,
                        expiryDate = finalExpiryDate,
                        buyDate = resolvedBuyDate,
                        isRemindEnabled = isGeofenceAlertEnabled
                    )

                    viewModel.addPantryItem(itemToSave, pantryDao) {
                        productName = ""
                        expiryDate = null
                        buyDate = null
                        selectedCategory = Category.NA
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Pantry Item", fontWeight = FontWeight.SemiBold)
        }
    }

    if (showScanner) {
        ScannerOverlay(
            mode = scannerMode,
            onDateDetected = { detectedExpiryDate, detectedBuyDate, detectedName, detectedCategory ->
                if (scannerMode == "productName") {
                    if (detectedName.isNotBlank()) productName = detectedName
                    if (detectedCategory != null) selectedCategory = detectedCategory
                } else if (scannerMode == "expiryDate") {
                    expiryDate = detectedExpiryDate
                } else if (scannerMode == "buyDate") {
                    buyDate = detectedBuyDate
                }
                showScanner = false
            },
            onClose = { showScanner = false }
        )
    }

    if (showVoiceOverlay && activeVoiceField != null) {
        VoiceCommandOverlay(
            mode = activeVoiceField ?: "",
            onProductNameParsed = { name ->
                if (activeVoiceField == "productName") {
                    productName = name
                }
                showVoiceOverlay = false
                activeVoiceField = null
            },
            onDateParsed = { detectedDate ->
                if (activeVoiceField == "expiryDate") {
                    expiryDate = detectedDate
                } else if (activeVoiceField == "buyDate") {
                    buyDate = detectedDate
                }
                showVoiceOverlay = false
                activeVoiceField = null
            },
            onClose = {
                showVoiceOverlay = false
                activeVoiceField = null
            }
        )
    }
}

@Composable
fun DateField(
    label: String,
    date: LocalDate?,
    onClick: () -> Unit,
    onCameraClick: () -> Unit,
    onMicClick: () -> Unit,
) {
    OutlinedTextField(
        value = date?.formatToDeviceLocale() ?: "",
        onValueChange = {},
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        label = { Text(label) },
        readOnly = true,
        trailingIcon = {
            DateFieldTrailingIcons(
                onMicClick = { onMicClick() },
                onCalendarClick = { onClick() },
                onCameraClick = { onCameraClick() }
            )
        }
    )
}

@Composable
fun DateFieldTrailingIcons(
    onMicClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onCameraClick: () -> Unit = {}
) {
    Row {
        IconButton(onClick = onCalendarClick) {
            Icon(Icons.Default.DateRange, contentDescription = "Pick Date")
        }
        IconButton(onClick = onMicClick) {
            Icon(Icons.Default.Mic, contentDescription = "Voice Command Add")
        }
        IconButton(onClick = onCameraClick) {
            Icon(Icons.Default.Camera, contentDescription = "Scan Label Date")
        }
    }
}


@Composable
fun ScannerOverlay(
    mode: String = "productName",
    onDateDetected: (LocalDate?, LocalDate?, String, Category?) -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Live Camera Scanner Engine (Actual use-case in background)
            CameraScanner(
                onDateDetected = onDateDetected,
                onClose = onClose,
                modifier = Modifier.fillMaxSize(),
                mode = mode
            )

            // Semi-transparent scrim to dim the background camera feed for UI readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )

            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
                Text(
                    text = when (mode) {
                        "productName" -> "Product Name OCR Scanner"
                        "expiryDate" -> "Expiration Date OCR Scanner"
                        else -> "Purchase/MFG Date OCR Scanner"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Viewfinder Target Frame perfectly center aligned
            Box(
                modifier = Modifier
                    .size(width = 280.dp, height = 200.dp)
                    .align(Alignment.Center)
                    .drawWithContent {
                        drawContent()
                        val cornerLength = 24.dp.toPx()
                        val strokeWidth = 3.dp.toPx()
                        val color = Color(0xFF6366F1)

                        // Top Left corners
                        drawRect(color = color, topLeft = Offset(0f, 0f), size = Size(cornerLength, strokeWidth))
                        drawRect(color = color, topLeft = Offset(0f, 0f), size = Size(strokeWidth, cornerLength))
                        // Top Right corners
                        drawRect(color = color, topLeft = Offset(size.width - cornerLength, 0f), size = Size(cornerLength, strokeWidth))
                        drawRect(color = color, topLeft = Offset(size.width - strokeWidth, 0f), size = Size(strokeWidth, cornerLength))
                        // Bottom Left corners
                        drawRect(color = color, topLeft = Offset(0f, size.height - strokeWidth), size = Size(cornerLength, strokeWidth))
                        drawRect(color = color, topLeft = Offset(0f, size.height - cornerLength), size = Size(strokeWidth, cornerLength))
                        // Bottom Right corners
                        drawRect(color = color, topLeft = Offset(size.width - cornerLength, size.height - strokeWidth), size = Size(cornerLength, strokeWidth))
                        drawRect(color = color, topLeft = Offset(size.width - strokeWidth, size.height - cornerLength), size = Size(strokeWidth, cornerLength))
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(2.dp)
                        .background(Color.Red)
                )
                Text(
                    text = when (mode) {
                        "productName" -> "Align product label within frame"
                        "expiryDate" -> "Align best-before stamp within frame"
                        else -> "Align mfg or purchase date within frame"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }

            // Bottom UI: Dynamic capture info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Analyzing camera feed in real-time...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when (mode) {
                        "productName" -> "Hold your device steady with good lighting to parse product names"
                        "expiryDate" -> "Hold your device steady with good lighting to parse expiry dates (preceded by EXP, BB)"
                        else -> "Hold your device steady with good lighting to parse buy/mfg dates"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialKmpDatePicker(
    show: Boolean,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    if (!show) return

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Clock.System.now().toEpochMilliseconds()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { mills ->
                        val instant = Instant.fromEpochMilliseconds(mills)
                        val tz = TimeZone.UTC
                        val localDateTime = instant.toLocalDateTime(tz)
                        onDateSelected(localDateTime.date)
                    }
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun VoiceWaveform(isProcessing: Boolean, volumeIntensity: Float = 0f) {
    val animatedIntensity by animateFloatAsState(
        targetValue = if (isProcessing) {
            if (volumeIntensity > 0f) volumeIntensity.coerceIn(0f, 1f) else 1f
        } else {
            0f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
    )

    val infiniteTransition = rememberInfiniteTransition()
    @Composable
    fun animateFallback(duration: Int, delay: Int): Float {
        val value by if (isProcessing && volumeIntensity == 0f) {
            infiniteTransition.animateFloat(
                initialValue = 0.15f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = duration, delayMillis = delay),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            remember { mutableStateOf(0.15f) }
        }
        return value
    }

    val baseMultipliers = listOf(0.15f, 0.35f, 0.6f, 0.85f, 0.7f, 0.5f, 0.8f, 1.0f, 0.75f, 0.55f, 0.4f, 0.25f, 0.1f)
    val fallbackHeights = listOf(
        animateFallback(450, 50),
        animateFallback(600, 150),
        animateFallback(500, 100),
        animateFallback(700, 0),
        animateFallback(400, 200),
        animateFallback(550, 80),
        animateFallback(650, 120),
        animateFallback(480, 60),
        animateFallback(580, 180),
        animateFallback(420, 110),
        animateFallback(610, 140),
        animateFallback(520, 90),
        animateFallback(680, 40)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        baseMultipliers.forEachIndexed { index, multiplier ->
            val scale = if (isProcessing) {
                if (volumeIntensity > 0f) {
                    // Drive heights dynamically by the real-time audio volume!
                    0.15f + (multiplier * animatedIntensity * 0.85f)
                } else {
                    // Smooth organic fallback wave
                    fallbackHeights[index]
                }
            } else {
                0.15f
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(4.dp)
                    .fillMaxHeight(scale)
                    .background(
                        color = if (isProcessing) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}