package app.piru.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val database = remember { JournalDatabase(this) }
                val entries = remember { mutableStateOf(database.listDoses()) }
                LaunchedEffect(Unit) { entries.value = database.listDoses() }
                Column {
                    Text("Piru — local-first journal")
                    entries.value.take(20).forEach { dose ->
                        Text("${dose.substance} — ${dose.amount} ${dose.unit} (${dose.route})")
                    }
                }
            }
        }
    }
}
