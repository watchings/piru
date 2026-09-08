package app.piru.android

import android.os.Bundle
import android.Manifest
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.room.Room
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "piru-reminders", "Piru reminders", NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
        setContent {
            MaterialTheme {
                val database = remember {
                    Room.databaseBuilder(
                        applicationContext,
                        PiruRoomDatabase::class.java,
                        "piru-room.db"
                    ).addMigrations(ROOM_MIGRATION_1_2).build()
                }
                val entries = remember { mutableStateOf(emptyList<DoseEntry>()) }
                val substance = remember { mutableStateOf("") }
                val amount = remember { mutableStateOf("") }
                val scope = rememberCoroutineScope()
                suspend fun refresh() {
                    entries.value = database.journal().list().map(RoomDoseEntry::toDoseEntry)
                }
                LaunchedEffect(Unit) { refresh() }
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
                            scope.launch {
                                database.journal().save(
                                    RoomDoseEntry(
                                        id = java.util.UUID.randomUUID().toString(),
                                        substance = substance.value,
                                        amount = parsed,
                                        unit = "mg",
                                        route = "oral",
                                        timestamp = System.currentTimeMillis(),
                                        notes = null
                                    )
                                )
                                refresh()
                                substance.value = ""
                                amount.value = ""
                            }
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
                                    scope.launch {
                                        database.journal().delete(dose.id)
                                        refresh()
                                    }
                                }) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                    val summary = UsageInsights.summarize(entries.value)
                    Text("Last ${summary.windowDays} days: ${summary.doseCount} doses, " +
                        "${summary.substanceCount} substances, ${summary.totalAmount} total amount")
                }
            }
        }
    }
}

private fun RoomDoseEntry.toDoseEntry() = DoseEntry(
    id, substance, amount, unit, route, timestamp, notes
)
