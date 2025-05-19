package com.sergiocuadros.dannacarrillo.busunab.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sergiocuadros.dannacarrillo.busunab.models.User
import com.sergiocuadros.dannacarrillo.busunab.models.UserRole
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class UserRepository {
    private val db: FirebaseFirestore = Firebase.firestore
    private val usersCollection = db.collection("users")

    suspend fun createUser(user: User): Result<User> = suspendCoroutine { continuation ->
        usersCollection.document(user.id)
            .set(user)
            .addOnSuccessListener {
                continuation.resume(Result.success(user))
            }
            .addOnFailureListener { e ->
                continuation.resume(Result.failure(e))
            }
    }

    suspend fun getUser(userId: String): Result<User> = suspendCoroutine { continuation ->
        usersCollection.document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        continuation.resume(Result.success(user))
                    } else {
                        continuation.resumeWithException(Exception("Failed to parse user data"))
                    }
                } else {
                    continuation.resumeWithException(Exception("User not found"))
                }
            }
            .addOnFailureListener { e ->
                continuation.resume(Result.failure(e))
            }
    }

    suspend fun updateUser(user: User): Result<User> = suspendCoroutine { continuation ->
        usersCollection.document(user.id)
            .set(user)
            .addOnSuccessListener {
                continuation.resume(Result.success(user))
            }
            .addOnFailureListener { e ->
                continuation.resume(Result.failure(e))
            }
    }

    suspend fun deleteUser(userId: String): Result<Unit> = suspendCoroutine { continuation ->
        usersCollection.document(userId)
            .delete()
            .addOnSuccessListener {
                continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                continuation.resume(Result.failure(e))
            }
    }

    suspend fun getAllDrivers(): Result<List<User>> = suspendCoroutine { continuation ->
        usersCollection
            .whereEqualTo("role", UserRole.DRIVER)
            .get()
            .addOnSuccessListener { documents ->
                val users = documents.mapNotNull { it.toObject(User::class.java) }
                continuation.resume(Result.success(users))
            }
            .addOnFailureListener { e ->
                continuation.resume(Result.failure(e))
            }
    }

    suspend fun getDriverByBusPlate(busPlate: String): Result<User?> = suspendCoroutine { continuation ->
        usersCollection
            .whereEqualTo("busPlate", busPlate)
            .whereEqualTo("role", UserRole.DRIVER)
            .get()
            .addOnSuccessListener { documents ->
                val user = documents.firstOrNull()?.toObject(User::class.java)
                continuation.resume(Result.success(user))
            }
            .addOnFailureListener { e ->
                continuation.resume(Result.failure(e))
            }
    }
} 