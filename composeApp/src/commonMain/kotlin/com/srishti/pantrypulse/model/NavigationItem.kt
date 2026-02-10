package com.srishti.pantrypulse.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.ui.graphics.vector.ImageVector

data class NavigationItem(val unSelectedIcon: ImageVector /* or  DrawableResource*/,
                          val selectedIcon: ImageVector /* or  DrawableResource*/,
                          val title: String /* or  StringResource  */,
                          val route: String)
object Graph {
    const val NAVIGATION_BAR_SCREEN_GRAPH = "navigationBarScreenGraph"
}

sealed class Routes(var route: String) {
    data object Add : Routes("Add")
    data object List : Routes("List")
}

val navigationItemsLists = listOf(
    NavigationItem(
        unSelectedIcon = Icons.Outlined.Add,
        selectedIcon = Icons.Filled.Add,
        title = "Add",
        route = Routes.Add.route,
    ),
    NavigationItem(
        unSelectedIcon = Icons.Outlined.Checklist,
        selectedIcon = Icons.Filled.Checklist,
        title = "List",
        route = Routes.List.route,
    ),
)