package com.example.cokeinventory

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cokeinventory.ui.DashboardScreen
import com.example.cokeinventory.ui.LoginScreen
import com.example.cokeinventory.ui.OnboardingTutorialScreen
import com.example.cokeinventory.ui.theme.SoftdrinksInventoryTheme
import com.example.cokeinventory.util.AdMobManager
import com.example.cokeinventory.viewmodel.InventoryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize AdMob early on startup
        AdMobManager.initialize(this)

        setContent {
            SoftdrinksInventoryTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SoftdrinksInventoryApp()
                }
            }
        }
    }
}

@Composable
fun SoftdrinksInventoryApp(
    viewModel: InventoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val prefs = remember { context.getSharedPreferences("drinkstock_prefs", Context.MODE_PRIVATE) }
    
    var hasSeenTutorial by remember {
        mutableStateOf(prefs.getBoolean("has_seen_tutorial", false))
    }
    
    var adminPin by remember {
        mutableStateOf(prefs.getString("admin_pin", "admin123") ?: "admin123")
    }

    if (!hasSeenTutorial) {
        OnboardingTutorialScreen(
            onFinishTutorial = {
                prefs.edit().putBoolean("has_seen_tutorial", true).apply()
                hasSeenTutorial = true
            }
        )
    } else if (uiState.isLoggedIn) {
        DashboardScreen(
            viewModel = viewModel,
            uiState = uiState,
            adminPin = adminPin,
            onUpdateAdminPin = { newPin ->
                prefs.edit().putString("admin_pin", newPin).apply()
                adminPin = newPin
            },
            onReplayTutorial = {
                prefs.edit().putBoolean("has_seen_tutorial", false).apply()
                hasSeenTutorial = false
            }
        )
    } else {
        LoginScreen(
            viewModel = viewModel,
            uiState = uiState,
            onRegisterSetup = { newAdminPin, storeName ->
                val chosenPin = if (newAdminPin.isBlank()) "admin123" else newAdminPin
                val chosenStore = if (storeName.isBlank()) "DrinkStock Store" else storeName
                
                prefs.edit()
                    .putString("admin_pin", chosenPin)
                    .putString("store_name", chosenStore)
                    .putBoolean("has_seen_tutorial", false)
                    .apply()

                adminPin = chosenPin
                hasSeenTutorial = false
            }
        )
    }
}
