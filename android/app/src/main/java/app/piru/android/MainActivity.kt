package app.piru.android

import android.os.Bundle
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "piru-reminders", "Piru reminders", NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        setContent {
            MaterialTheme {
                val database = remember { JournalDatabase(this) }
                val entries = remember { mutableStateOf(database.listDoses()) }
                val substance = remember { mutableStateOf("") }
                val amount = remember { mutableStateOf("") }
                LaunchedEffect(Unit) { entries.value = database.listDoses() }
                Column {
                    Text("Piru — local-first journal")
                    OutlinedTextField(
                        value = substance.value,
                        onValueChange = { substance.value = it },
                        label = { Text("Substance") }
                    )
                    OutlinedTextField(
                        value = amount.value,
                        onValueChange = { amount.value = it },
                        label = { Text("Amount") }
                    )
                    Button(onClick = {
                        val parsed = amount.value.toDoubleOrNull()
                        if (substance.value.isNotBlank() && parsed != null && parsed >= 0) {
                            database.addDose(substance.value, parsed, "mg", "oral", null)
                            entries.value = database.listDoses()
                            substance.value = ""
                            amount.value = ""
                        }
                    }) {
                        Text("Save dose")
                    }
                    Spacer(Modifier.height(12.dp))
                    entries.value.take(20).forEach { dose ->
                        val elapsed = ((System.currentTimeMillis() - dose.timestamp) / 60000.0).coerceAtLeast(0.0)
                        val remaining = (PkModel.remainingFraction(elapsed, 300.0) * 100).toInt()
                        Column {
                            Text("${dose.substance} — ${dose.amount} ${dose.unit} (${dose.route}), " +
                                "estimated $remaining% remaining")
                            Row {
                                Button(onClick = {
                                    database.deleteDose(dose.id)
                                    entries.value = database.listDoses()
                                }) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
