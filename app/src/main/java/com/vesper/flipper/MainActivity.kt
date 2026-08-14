package com.vesper.flipper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vesper.flipper.ble.FlipperBleService
import com.vesper.flipper.data.SettingsStore
import com.vesper.flipper.ui.components.AppDrawerContent
import com.vesper.flipper.ui.components.DrawerItem
import com.vesper.flipper.ui.components.DrawerSection
import com.vesper.flipper.ui.components.LocalOpenDrawer
import com.vesper.flipper.ui.screen.*
import com.vesper.flipper.ui.theme.GlassStroke
import com.vesper.flipper.ui.theme.TextTertiary
import com.vesper.flipper.ui.theme.VesperAccent
import com.vesper.flipper.ui.theme.VesperBackdropBrush
import com.vesper.flipper.ui.theme.VesperSurface
import com.vesper.flipper.ui.theme.VesperTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsStore: SettingsStore

    /**
     * Without these the BLE service cannot scan or connect, so it is not started.
     *
     * ACCESS_FINE_LOCATION is essential only below Android 12, where the platform
     * refused a BLE scan without it. From Android 12 the neverForLocation flag on
     * BLUETOOTH_SCAN replaces that requirement, and FlipperBleService.hasBluetoothPermissions()
     * already checks only BLUETOOTH_SCAN and BLUETOOTH_CONNECT — so asking for location
     * on a modern phone gated the app on something it does not use.
     */
    private val essentialPermissions = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }.toTypedArray()

    /**
     * Asked for, but denying them must not stop the Flipper connection. A foreground
     * service still runs with POST_NOTIFICATIONS denied; the user simply does not see
     * its notification.
     */
    private val optionalPermissions = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Start on the essential set alone. The previous version required every
        // requested permission, so one declined optional prompt left the app with no
        // Bluetooth for the rest of its life.
        if (hasEssentialPermissions()) {
            startBleService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            val darkMode by settingsStore.darkMode.collectAsState(initial = true)
            VesperTheme(darkTheme = darkMode) {
                VesperApp()
            }
        }
    }

    private fun hasEssentialPermissions(): Boolean = essentialPermissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = (essentialPermissions + optionalPermissions).filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            startBleService()
        } else {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun startBleService() {
        FlipperBleService.startService(this)
    }
}

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Chat : Screen("chat", "Chat", Icons.Filled.Chat, Icons.Outlined.Chat)
    object Oracle : Screen("oracle", "Oracle", Icons.Filled.Visibility, Icons.Outlined.Visibility)
    object Arsenal : Screen("arsenal", "Arsenal", Icons.Filled.Sensors, Icons.Outlined.Sensors)
    object Alchemy : Screen("alchemy", "Alchemy", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome)
    object OpsCenter : Screen("ops_center", "Ops", Icons.Filled.BluetoothSearching, Icons.Outlined.BluetoothSearching)
    object PayloadLab : Screen("payload_lab", "Payloads", Icons.Filled.Code, Icons.Outlined.Code)
    object Files : Screen("files", "Files", Icons.Filled.Folder, Icons.Outlined.Folder)
    object Audit : Screen("audit", "Audit", Icons.Filled.History, Icons.Outlined.History)
    object Device : Screen("device", "Device", Icons.Filled.Bluetooth, Icons.Outlined.Bluetooth)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

val screens = listOf(
    Screen.Chat,
    Screen.Alchemy,
    Screen.Device,
    Screen.Settings
)

@Composable
fun VesperApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    fun go(screen: Screen) {
        scope.launch { drawerState.close() }
        if (currentRoute == screen.route) return
        navController.navigate(screen.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Navigation moved out of a bottom bar and into a drawer. A bar holds four
    // items before labels truncate and this app has ten destinations — five of
    // which could not be reached at all because they did not fit. On the chat
    // screen the bar also sat directly under the composer, stacking two pieces of
    // chrome at the bottom of the screen competing for the same thumb.
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent {
                DrawerItem("Chat", Icons.Filled.Chat, currentRoute == Screen.Chat.route) { go(Screen.Chat) }
                DrawerItem("Device", Icons.Filled.Bluetooth, currentRoute == Screen.Device.route) { go(Screen.Device) }
                DrawerItem("Files", Icons.Filled.Folder, currentRoute == Screen.Files.route) { go(Screen.Files) }

                DrawerSection("Build")
                DrawerItem("Alchemy Lab", Icons.Filled.AutoAwesome, currentRoute == Screen.Alchemy.route) { go(Screen.Alchemy) }
                DrawerItem("Payload Lab", Icons.Filled.Code, currentRoute == Screen.PayloadLab.route) { go(Screen.PayloadLab) }
                DrawerItem("Signal Arsenal", Icons.Filled.Sensors, currentRoute == Screen.Arsenal.route) { go(Screen.Arsenal) }
                DrawerItem("Spectral Oracle", Icons.Filled.Visibility, currentRoute == Screen.Oracle.route) { go(Screen.Oracle) }

                DrawerSection("Diagnostics")
                DrawerItem("Ops Center", Icons.Filled.BluetoothSearching, currentRoute == Screen.OpsCenter.route) { go(Screen.OpsCenter) }
                DrawerItem("Audit log", Icons.Filled.Receipt, currentRoute == Screen.Audit.route) { go(Screen.Audit) }

                DrawerSection("App")
                DrawerItem("Settings", Icons.Filled.Settings, currentRoute == Screen.Settings.route) { go(Screen.Settings) }
            }
        }
    ) {
    CompositionLocalProvider(LocalOpenDrawer provides { scope.launch { drawerState.open() } }) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VesperBackdropBrush)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            // The host Scaffold claims no insets of its own. Destinations that carry a
            // Scaffold + TopAppBar already apply the status-bar inset themselves, and
            // letting this one apply it too put a band of empty space above every header.
            // AlchemyLab is the one reachable destination that draws its own header with
            // no TopAppBar; it applies statusBarsPadding() at its own root instead. Any
            // new screen without a TopAppBar must do the same.
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Chat.route,
                // innerPadding now carries only the bottom bar, because this Scaffold
                // claims no window insets — see contentWindowInsets above.
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Chat.route) {
                    ChatScreen(
                        onNavigateToAudit = {
                            navController.navigate(Screen.Audit.route)
                        }
                    )
                }
                composable(Screen.Alchemy.route) {
                    AlchemyLabScreen()
                }
                composable(Screen.OpsCenter.route) {
                    OpsCenterScreen()
                }
                composable(Screen.Files.route) {
                    FileBrowserScreen()
                }
                composable(Screen.Audit.route) {
                    AuditScreen()
                }
                composable(Screen.Device.route) {
                    DeviceScreen(
                        onNavigateToFiles = {
                            navController.navigate(Screen.Files.route)
                        }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
                composable(Screen.PayloadLab.route) {
                    PayloadLabScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable(Screen.Arsenal.route) {
                    SignalArsenalScreen()
                }
                composable(Screen.Oracle.route) {
                    SpectralOracleScreen()
                }
            }
        }
    }
    }
    }
}

/**
 * One tab inside the floating pill.
 *
 * The label is always present rather than appearing only on the selected tab.
 * Icon-only tabs look tidier in a mockup and cost real time in use: this app's
 * icons (a spark, a chip, a bluetooth glyph) do not name their destinations
 * well enough to stand alone.
 */
@Composable
private fun NavPill(
    screen: Screen,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) VesperAccent.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
            contentDescription = screen.title,
            tint = if (selected) VesperAccent else TextTertiary,
            modifier = Modifier.size(21.dp)
        )
        Text(
            text = screen.title,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) VesperAccent else TextTertiary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
