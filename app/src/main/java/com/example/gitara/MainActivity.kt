package com.example.gitara

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.gitara.data.DataStoreManager
import com.example.gitara.data.GuitarSong
import com.example.gitara.data.SongsManager
import com.example.gitara.data.sampleSongs
import com.example.gitara.metronome.MetronomeManager
import com.example.gitara.ui.screens.*
import com.example.gitara.ui.theme.GitaraTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Инициализируем DataStore до UI
        val dataStoreManager = DataStoreManager(applicationContext)

        // 2. Блокирующе читаем стартовое значение темы
        val initialIsDarkTheme = runBlocking {
            dataStoreManager.isDarkTheme.first()
        }

        enableEdgeToEdge()

        setContent {
            // 3. Подписываемся на изменения темы
            val isDarkTheme by dataStoreManager.isDarkTheme.collectAsState(initial = initialIsDarkTheme)

            GitaraTheme(
                darkTheme = isDarkTheme,
                dynamicColor = false
            ) {
                MainScreen()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        MetronomeManager.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        MetronomeManager.stop()
    }
}

@Composable
fun MainScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }

    // Данные песен
    val customSongs by dataStoreManager.getCustomSongsFlow().collectAsState(initial = emptyList())
    val allSongs = remember(customSongs) { sampleSongs + customSongs }

    val navController = rememberNavController()
    var editingSong by remember { mutableStateOf<GuitarSong?>(null) }

    // Сохранение скролла списка
    val songsListState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    val items = listOf(
        BottomNavItem.Metronome,
        BottomNavItem.Songs,
        BottomNavItem.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Скрываем нижнюю панель только на деталях/редактировании, но показываем в списке и метрономе
    val showBottomBar = currentDestination?.route?.startsWith("songDetail/") != true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.height(85.dp)
                ) {
                    items.forEach { screen ->
                        // Проверяем выделение
                        val isRouteInTab = when (screen.route) {
                            "songs" -> currentDestination?.route in listOf("songs", "add_song") ||
                                    currentDestination?.route?.startsWith("songDetail") == true
                            else -> currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        }

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            selected = isRouteInTab,
                            onClick = {
                                navController.navigate(screen.route) {
                                    // Сбрасываем стек до начального экрана при переключении
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true

                                    // 🔥 ИСПРАВЛЕНИЕ БАГА:
                                    // Если мы переходим на вкладку "Песни" (screen.route == "songs"),
                                    // мы принудительно ставим restoreState = false.
                                    // Это заставляет навигатор открыть список песен заново,
                                    // вместо того чтобы восстанавливать "зависший" экран метронома.
                                    restoreState = (screen.route != "songs")
                                }
                            },
                            alwaysShowLabel = false,
                            label = null
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "songs",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("metronome") {
                MetronomeScreen(onBackClick = { navController.popBackStack() })
            }

            composable("songs") {
                GuitarSongsScreen(
                    customSongs = customSongs,
                    listState = songsListState,
                    onSongClick = { song ->
                        navController.navigate("songDetail/${song.id}")
                    },
                    onAddSongClick = {
                        editingSong = null
                        navController.navigate("add_song")
                    },
                    onEditSong = { song ->
                        editingSong = song
                        navController.navigate("add_song")
                    },
                    onDeleteSong = { song ->
                        scope.launch {
                            SongsManager.deleteSongFiles(context, song)
                            dataStoreManager.deleteCustomSong(song.id)
                        }
                    }
                )
            }

            composable(
                route = "songDetail/{songId}",
                arguments = listOf(navArgument("songId") { type = NavType.StringType })
            ) { backStackEntry ->
                val songId = backStackEntry.arguments?.getString("songId")
                val song = remember(songId, allSongs) { allSongs.find { it.id == songId } }
                song?.let {
                    GuitarSongDetailScreen(
                        song = it,
                        onBackClick = { navController.popBackStack() },
                        // 🔥 При клике на метроном из песни мы просто переходим на экран метронома
                        // А благодаря фиксу выше, кнопка "Назад" (в нижнем баре) вернет нас в список.
                        onMetronomeClick = { navController.navigate("metronome") }
                    )
                }
            }

            composable("settings") { SettingsScreen() }

            composable("add_song") {
                AddSongScreen(
                    songToEdit = editingSong,
                    onBackClick = { navController.popBackStack() },
                    onSongSaved = {
                        navController.popBackStack()
                        editingSong = null
                    }
                )
            }
        }
    }
}

sealed class BottomNavItem(val route: String, val icon: @Composable () -> ImageVector) {
    object Metronome : BottomNavItem("metronome", { ImageVector.vectorResource(R.drawable.ic_metronome) })
    object Songs : BottomNavItem("songs", { Icons.Default.Home })
    object Settings : BottomNavItem("settings", { Icons.Default.Settings })
}
