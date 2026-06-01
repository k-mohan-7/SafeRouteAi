package com.saferoute.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.saferoute.ai.data.local.SessionDataStore
import com.saferoute.ai.service.LocationService
import com.saferoute.ai.ui.navigation.SafeRouteNavHost
import com.saferoute.ai.ui.theme.SafeRouteAITheme
import com.saferoute.ai.util.ConnectivityObserver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionDataStore: SessionDataStore
    @Inject lateinit var connectivityObserver: ConnectivityObserver

    private val backgroundPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        startLocationServiceIfAllowed()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val fineGranted = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val bgGranted = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (!bgGranted) {
                    backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    startLocationServiceIfAllowed()
                }
            } else {
                startLocationServiceIfAllowed()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermissions()
        setContent {
            SafeRouteAITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SafeRouteNavHost(sessionDataStore, connectivityObserver)
                }
            }
        }
    }

    private fun requestPermissions() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    private fun startLocationServiceIfAllowed() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED) {
            startForegroundService(Intent(this, LocationService::class.java))
        }
    }
}
