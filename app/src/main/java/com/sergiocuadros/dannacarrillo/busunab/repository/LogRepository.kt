package com.sergiocuadros.dannacarrillo.busunab.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.sergiocuadros.dannacarrillo.busunab.models.LogEntry
import kotlinx.coroutines.tasks.await

object LogRepository {
    private val logs = FirebaseFirestore.getInstance().collection("logs")

    suspend fun addLog(log: LogEntry) {
        logs.add(log).await()
    }

    suspend fun getLogsByBus(busId: String): List<LogEntry> {
        return logs.whereEqualTo("busId", busId).get().await().toObjects(LogEntry::class.java)
    }
}
