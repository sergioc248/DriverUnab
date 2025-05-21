package com.sergiocuadros.dannacarrillo.busunab.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sergiocuadros.dannacarrillo.busunab.models.User
import com.sergiocuadros.dannacarrillo.busunab.models.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState

    fun registerUser(name: String, email: String, pass: String, isAdmin: Boolean) {
        viewModelScope.launch {
            _registrationState.value = RegistrationState.Loading
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    val userRole = if (isAdmin) UserRole.ADMIN else UserRole.DRIVER
                    val user = User(
                        id = firebaseUser.uid,
                        email = email,
                        name = name,
                        role = userRole
                    )
                    db.collection("users").document(firebaseUser.uid).set(user).await()
                    _registrationState.value = RegistrationState.Success(userRole)
                } else {
                    _registrationState.value = RegistrationState.Error("Firebase user is null after registration.")
                }
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error(e.message ?: "Unknown registration error")
            }
        }
    }
}

sealed class RegistrationState {
    object Idle : RegistrationState()
    object Loading : RegistrationState()
    data class Success(val role: UserRole) : RegistrationState()
    data class Error(val message: String) : RegistrationState()
} 