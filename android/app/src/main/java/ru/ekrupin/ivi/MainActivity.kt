package ru.ekrupin.ivi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import ru.ekrupin.ivi.app.ui.IviAppRoot
import ru.ekrupin.ivi.app.ui.theme.IviTheme
import ru.ekrupin.ivi.data.sync.AppSyncRunner

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var appSyncRunner: AppSyncRunner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IviTheme {
                IviAppRoot()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appSyncRunner.triggerForegroundSync()
    }
}
