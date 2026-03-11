package com.example.task07

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private var service: RandomNumberService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val local = binder as? RandomNumberService.LocalBinder ?: return
            service = local.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BindScreen(
                    onConnect = { connectService() },
                    onDisconnect = { disconnectService() },
                    isBoundProvider = { isBound },
                    serviceProvider = { service }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectService()
    }

    private fun connectService() {
        if (isBound) return
        bindService(Intent(this, RandomNumberService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    private fun disconnectService() {
        if (!isBound) return
        unbindService(connection)
        service = null
        isBound = false
    }
}

@Composable
private fun BindScreen(
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    isBoundProvider: () -> Boolean,
    serviceProvider: () -> RandomNumberService?
) {
    val fallbackFlow = remember { MutableStateFlow(0) }
    val stateFlow = serviceProvider()?.numbers ?: fallbackFlow
    val randomNumber by stateFlow.collectAsState()
    var connected by remember { mutableStateOf(isBoundProvider()) }

    connected = isBoundProvider()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Bind service random number", style = MaterialTheme.typography.headlineSmall)
            Text("Connected: $connected")
            Text("Current number: $randomNumber")

            Button(onClick = onConnect) { Text("Подключиться") }
            Button(onClick = onDisconnect) { Text("Отключиться") }
        }
    }
}
