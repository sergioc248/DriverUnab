package com.sergiocuadros.dannacarrillo.busunab.viewmodels
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.sergiocuadros.dannacarrillo.busunab.models.LogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LoginViewModel : ViewModel() {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs

    fun loadLogs() {
        FirebaseFirestore.getInstance().collection("logs")
            .get()
            .addOnSuccessListener { result ->
                val entries = result.documents.mapNotNull { it.toObject(LogEntry::class.java) }
                _logs.value = entries
            }
    }
}
