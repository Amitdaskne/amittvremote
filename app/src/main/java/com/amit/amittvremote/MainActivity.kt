package com.amit.amittvremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.amit.amittvremote.ui.screen.RemoteScreen
import com.amit.amittvremote.ui.theme.AmitTVRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AmitTVRemoteTheme {
                RemoteScreen()
            }
        }
    }
}
