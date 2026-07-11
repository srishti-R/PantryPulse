
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.srishti.pantrypulse.db.PantryItem
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PantryListScreen(
    pantryDao: PantryDao
) {
    val coroutineScope = rememberCoroutineScope()

    // UI states
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<Category?>(null) }
    var sortByExpiry by remember { mutableStateOf(true) }

    // Fetch live list of pantry items from Room DB flow
    val pantryItems by pantryDao.getAllItemsFlow().collectAsState(initial = emptyList())

    // Filtering & Sorting calculations
    val filteredItems = remember(pantryItems, searchQuery, selectedCategoryFilter, sortByExpiry) {
        pantryItems
            .filter { item ->
                val matchesSearch = item.name.contains(searchQuery, ignoreCase = true)
                val matchesCategory = selectedCategoryFilter == null || item.category == selectedCategoryFilter
                matchesSearch && matchesCategory
            }
            .sortedWith(
                if (sortByExpiry) {
                    compareBy<PantryItem> { it.expiryDate == null }.thenBy { it.expiryDate }
                } else {
                    compareBy { it.name.lowercase() }
                }
            )
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // SEARCH & SORT ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search ingredients...") },
                    placeholder = { Text("Milk, Eggs, Broccoli etc...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                IconButton(
                    onClick = { sortByExpiry = !sortByExpiry },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = if (sortByExpiry) Icons.Default.SortByAlpha else Icons.Default.CalendarToday,
                        contentDescription = "Change Sorting"
                    )
                }
            }

            // CATEGORY CHIP CAROUSEL
            Text(
                "Filter by Category",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "All" Filter chip
                FilterChip(
                    selected = selectedCategoryFilter == null,
                    onClick = { selectedCategoryFilter = null },
                    label = { Text("All") }
                )

                Category.entries.forEach { category ->
                    FilterChip(
                        selected = selectedCategoryFilter == category,
                        onClick = { selectedCategoryFilter = category },
                        label = { Text(category.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // LIST DISPLAY
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            "No pantry items found",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        SwipeToDismissItem(
                            item = item,
                            onDismiss = {
                                coroutineScope.launch {
                                    pantryDao.deleteItem(item)
                                }
                            },
                            onToggleConsumed = {
                                coroutineScope.launch {
                                    pantryDao.updateItem(item.copy(consumed = !item.consumed))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissItem(
    item: PantryItem,
    onDismiss: () -> Unit,
    onToggleConsumed: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { positionalValue ->
            if (positionalValue == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else if (positionalValue == SwipeToDismissBoxValue.StartToEnd) {
                onToggleConsumed()
                false // Don't dismiss, just check toggle
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction == SwipeToDismissBoxValue.StartToEnd || direction == SwipeToDismissBoxValue.EndToStart) {
                val color = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    else -> Color.Transparent
                }

                val alignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }

                val icon = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Check
                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.DeleteSweep
                    else -> Icons.Default.Delete
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(color)
                        .padding(horizontal = 20.dp),
                    contentAlignment = alignment
                ) {
                    Icon(icon, contentDescription = null, tint = contentColorFor(color))
                }
            }
        },
        content = {
            PantryItemCard(pantryItem = item, onToggleConsumed = onToggleConsumed)
        }
    )
}

@Composable
fun PantryItemCard(
    pantryItem: PantryItem,
    onToggleConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (pantryItem.category) {
        Category.DAIRY -> Color(0xFFD97706)
        Category.FRUITS_VEG -> Color(0xFF16A34A)
        Category.BAKERY -> Color(0xFFB45309)
        Category.MEAT_SEAFOOD -> Color(0xFFDC2626)
        Category.PANTRY -> Color(0xFFCA8A04)
        Category.BEVERAGES -> Color(0xFF0369A1)
        Category.FROZEN -> Color(0xFF00838F)
        Category.SNACKS -> Color(0xFF7B1FA2)
        else -> Color(0xFF455A64)
    }

    val cardBgColor =  when (pantryItem.category) {
        Category.DAIRY -> Color(0xFFFFFDF9)
        Category.FRUITS_VEG -> Color(0xFFF7FDF8)
        Category.BAKERY -> Color(0xFFFCF9F5)
        Category.MEAT_SEAFOOD -> Color(0xFFFFF6F6)
        Category.PANTRY -> Color(0xFFFFFDF3)
        Category.BEVERAGES -> Color(0xFFF5FAFF)
        Category.FROZEN -> Color(0xFFF4FEFF)
        Category.SNACKS -> Color(0xFFFCF9FF)
        else -> MaterialTheme.colorScheme.surface
    }

    if (pantryItem.consumed) {
        cardBgColor.copy(alpha = 0.4f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant category colored thin left stripe accent
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(5.dp)
                    .background(categoryColor)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onToggleConsumed) {
                    Icon(
                        imageVector = if (pantryItem.consumed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        tint = if (pantryItem.consumed) Color(0xFF10B981) else MaterialTheme.colorScheme.outline,
                        contentDescription = "Toggle Consumed"
                    )
                }

                val (badgeContainerColor, textColor) = when (pantryItem.category) {
                    Category.DAIRY -> Color(0xFFFFF2E6) to Color(0xFFD97706)
                    Category.FRUITS_VEG -> Color(0xFFEAF9EB) to Color(0xFF16A34A)
                    Category.BAKERY -> Color(0xFFFAF1E6) to Color(0xFFB45309)
                    Category.MEAT_SEAFOOD -> Color(0xFFFCE8E6) to Color(0xFFDC2626)
                    Category.PANTRY -> Color(0xFFFEF9E7) to Color(0xFFCA8A04)
                    Category.BEVERAGES -> Color(0xFFE0F2FE) to Color(0xFF0369A1)
                    Category.FROZEN -> Color(0xFFE0F7FA) to Color(0xFF00838F)
                    Category.SNACKS -> Color(0xFFF3E5F5) to Color(0xFF7B1FA2)
                    else -> Color(0xFFECEFF1) to Color(0xFF455A64)
                }

                Column(modifier = Modifier.weight(1f)) {
                    val capitalizedName =
                        pantryItem.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    Text(
                        text = capitalizedName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (pantryItem.consumed) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (pantryItem.consumed) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        val categoryDisplay = pantryItem.category.displayName

                        Box(
                            modifier = Modifier
                                .background(
                                    badgeContainerColor,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = categoryDisplay,
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Dynamic Expiry Indicator Badge at right edge
                pantryItem.expiryDate?.let { date ->
                    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    val isExpired = date < today
                    val isExpiringSoon = date >= today && today.daysUntil(date) <= 3

                    val dotColor = if (pantryItem.consumed) Color.LightGray
                    else if (isExpired) Color(0xFFDC2626)
                    else if (isExpiringSoon) Color(0xFFF59E0B)
                    else Color(0xFF10B981)

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(dotColor)
                    )
                }
            }
        }
    }
}

fun getCategoryIcon(category: Category): ImageVector {
    return when (category) {
        Category.DAIRY -> Icons.Filled.WaterDrop       // Milk / Liquid representation
        Category.FRUITS_VEG -> Icons.Filled.Eco         // Leaf representation
        Category.BAKERY -> Icons.Filled.BakeryDining   // Bread / Bakery icon
        Category.MEAT_SEAFOOD -> Icons.Filled.Restaurant // Dining cutlery representation
        Category.PANTRY -> Icons.Filled.ShoppingBag    // Package / Essential bag
        Category.BEVERAGES -> Icons.Filled.LocalCafe    // Coffee Cup representing Drinks
        Category.FROZEN -> Icons.Filled.AcUnit         // Snowflake representing refrigeration
        Category.SNACKS -> Icons.Filled.Cookie         // Cookie/Snack icon
        else -> Icons.Filled.Inventory                 // Default bundle package icon
    }
}