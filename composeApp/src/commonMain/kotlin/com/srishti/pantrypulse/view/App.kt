
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.room.RoomDatabase
import com.srishti.pantrypulse.db.PantryDatabase
import com.srishti.pantrypulse.model.Graph
import com.srishti.pantrypulse.model.NavigationItem
import com.srishti.pantrypulse.model.Routes
import com.srishti.pantrypulse.model.navigationItemsLists
import com.srishti.pantrypulse.view.BottomNavigationBar

@Composable
@Preview
fun App(
    databaseBuilder: RoomDatabase.Builder<PantryDatabase>
) {
    MaterialTheme {
        val database = remember { databaseBuilder.build() }

        val pantryDao =  database.getDao()

        MainScreen(pantryDao = pantryDao)
    }
}

@Composable
fun MainScreen(pantryDao: PantryDao) {
    val rootNavController = rememberNavController()
    val navBackStackEntry by rootNavController.currentBackStackEntryAsState()
    val currentRoute by remember(navBackStackEntry) {
        derivedStateOf {
            navBackStackEntry?.destination?.route
        }
    }
    val navigationItem by remember {
        derivedStateOf {
            navigationItemsLists.find { it.route == currentRoute }
        }
    }
    val isBottomBarVisible by remember {
        derivedStateOf {
            navigationItem != null
        }
    }

    MainScaffold(
        rootNavController = rootNavController,
        currentRoute = currentRoute,
        isBottomBarVisible = isBottomBarVisible,
        onItemClick = { currentNavigationItem ->
            rootNavController.navigate(currentNavigationItem.route) {
                popUpTo(rootNavController.graph.startDestinationRoute ?: "") {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        pantryDao = pantryDao
    )
}

@Composable
fun MainScaffold(
    rootNavController: NavHostController,
    currentRoute: String?,
    isBottomBarVisible: Boolean,
    onItemClick: (NavigationItem) -> Unit,
    pantryDao: PantryDao
) {
    Row {
        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visible = isBottomBarVisible,
                    enter = slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight }
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight }
                    )
                ) {
                    BottomNavigationBar(
                        items = navigationItemsLists,
                        currentRoute = currentRoute,
                        onItemClick = { currentNavigationItem ->
                            onItemClick(currentNavigationItem)
                        }
                    )
                }
            }
        ) { innerPadding ->
            RootNavGraph(
                rootNavController = rootNavController,
                innerPadding = innerPadding,
                pantryDao = pantryDao
            )
        }
    }
}

@Composable
fun RootNavGraph(
    rootNavController: NavHostController,
    innerPadding: PaddingValues,
    pantryDao: PantryDao
) {
    NavHost(
        navController = rootNavController,
        startDestination = Graph.NAVIGATION_BAR_SCREEN_GRAPH,
    ) {
        mainNavGraph(rootNavController = rootNavController, pantryDao = pantryDao, innerPadding = innerPadding)
    }
}

fun NavGraphBuilder.mainNavGraph(
    rootNavController: NavHostController,
    pantryDao: PantryDao,
    innerPadding: PaddingValues
) {
    navigation(
        startDestination = Routes.Add.route,
        route = Graph.NAVIGATION_BAR_SCREEN_GRAPH
    ) {
        composable(route = Routes.Add.route) {
            AddItemScreen(
                pantryDao = pantryDao
            )
        }
        composable(route = Routes.List.route) {
            PantryListScreen(
                rootNavController = rootNavController,
                pantryDao = pantryDao
            )
        }
    }
}