package com.sergiocuadros.dannacarrillo.busunab.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
import com.sergiocuadros.dannacarrillo.busunab.repository.UserRepository

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore
    private val userRepository = UserRepository()

    private val _currentUserData = MutableStateFlow<CurrentUserState>(CurrentUserState.Loading)
    val currentUserData: StateFlow<CurrentUserState> = _currentUserData

    private val _driverList = MutableStateFlow<List<User>>(emptyList())
    val driverList: StateFlow<List<User>> = _driverList

    init {
        observeAuthState()
        fetchAllDrivers()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            auth.addAuthStateListener { firebaseAuth ->
                val firebaseUser = firebaseAuth.currentUser
                if (firebaseUser != null) {
                    fetchUserData(firebaseUser)
                } else {
                    _currentUserData.value = CurrentUserState.NotAuthenticated
                }
            }
        }
    }

    private fun fetchUserData(firebaseUser: FirebaseUser) {
        viewModelScope.launch {
            _currentUserData.value = CurrentUserState.Loading
            try {
                val documentSnapshot = db.collection("users").document(firebaseUser.uid).get().await()
                Log.d("AuthViewModel", "Fetching user data for UID: ${firebaseUser.uid}")
                Log.d("AuthViewModel", "Document exists: ${documentSnapshot.exists()}")
                if (documentSnapshot.exists()) {
                    Log.d("AuthViewModel", "Document data: ${documentSnapshot.data}")
                }
                val user = documentSnapshot.toObject(User::class.java)
                if (user != null) {
                    _currentUserData.value = CurrentUserState.Authenticated(user)
                    Log.d("AuthViewModel", "User data successfully parsed: $user")
                } else {
                    Log.e("AuthViewModel", "User data not found in Firestore or failed to parse for UID: ${firebaseUser.uid}. Document exists: ${documentSnapshot.exists()}")
                    _currentUserData.value = CurrentUserState.Error("User data not found or unparsable in Firestore.")
                     // auth.signOut() // Optionally sign out if user record is crucial
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error fetching user data for UID: ${firebaseUser.uid}", e)
                _currentUserData.value = CurrentUserState.Error(e.message ?: "Error fetching user data.")
            }
        }
    }

    fun fetchAllDrivers() {
        viewModelScope.launch {
            try {
                val result = userRepository.getAllDrivers()
                if (result.isSuccess) {
                    _driverList.value = result.getOrNull() ?: emptyList()
                } else {
                    Log.e("AuthViewModel", "Error fetching drivers: ${result.exceptionOrNull()?.message}")
                    _driverList.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Exception fetching drivers: ${e.message}")
                _driverList.value = emptyList()
            }
        }
    }

    fun refreshUserData() {
        Log.d("AuthViewModel", "refreshUserData called.")
        val firebaseUser = auth.currentUser
        Log.d("AuthViewModel", "Current Firebase user in refreshUserData: ${firebaseUser?.uid ?: "null"}")
        if (firebaseUser != null) {
            fetchUserData(firebaseUser)
        } else {
            _currentUserData.value = CurrentUserState.NotAuthenticated
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUserData.value = CurrentUserState.NotAuthenticated
    }
}

sealed class CurrentUserState {
    object Loading : CurrentUserState()
    data class Authenticated(val user: User) : CurrentUserState()
    object NotAuthenticated : CurrentUserState()
    data class Error(val message: String) : CurrentUserState()
} 