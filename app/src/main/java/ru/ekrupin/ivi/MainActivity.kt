package ru.ekrupin.ivi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import ru.ekrupin.ivi.app.ui.IviAppRoot
import ru.ekrupin.ivi.app.ui.theme.IviTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IviTheme {
                IviAppRoot()
            }
        }
    }
}
