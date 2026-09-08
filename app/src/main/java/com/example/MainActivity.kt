package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.RafiqahViewModel
import com.example.ui.navigation.RafiqahNavGraph
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
          ) {
            val viewModel: RafiqahViewModel = viewModel()
            val rawDestination = intent?.getStringExtra("com.example.reminder.EXTRA_DESTINATION")
            val startDest = when (rawDestination) {
              "reading" -> com.example.ui.navigation.RafiqahDestinations.READING
              "learning" -> com.example.ui.navigation.RafiqahDestinations.LEARNING
              "focus" -> com.example.ui.navigation.RafiqahDestinations.FOCUS_SESSION
              "health" -> com.example.ui.navigation.RafiqahDestinations.HEALTH
              "planner" -> com.example.ui.navigation.RafiqahDestinations.PLANNER
              "french" -> com.example.ui.navigation.RafiqahDestinations.FRENCH
              else -> com.example.ui.navigation.RafiqahDestinations.HOME
            }
            RafiqahNavGraph(viewModel = viewModel, startDestination = startDest)
          }
        }
      }
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "مرحباً $name! رفيقة معك 🌸", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { Greeting("أمي") }
}
