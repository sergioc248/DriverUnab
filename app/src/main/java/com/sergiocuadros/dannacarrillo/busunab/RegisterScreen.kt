package com.sergiocuadros.dannacarrillo.busunab

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onClickBack: () -> Unit = {}, onSuccessfulRegister: () -> Unit = {}) {
    val auth = Firebase.auth
    val db = Firebase.firestore
    val activity = LocalView.current.context as Activity

    // ESTADOS
    var inputName by remember { mutableStateOf("") }
    var inputEmail by remember { mutableStateOf("") }
    var inputPassword by remember { mutableStateOf("") }
    var inputPasswordConfirmation by remember { mutableStateOf("") }

    var registerError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var passwordConfirmationError by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick =
                            onClickBack
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                imageVector = Icons.Default.Person,
                contentDescription = "Usuario",
                modifier = Modifier.size(150.dp),
                colorFilter = ColorFilter.tint(Color(0xFFFF9900))
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Registrarse",
                fontSize = 24.sp,
                color = Color(0xFFFF9900),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = inputName,
                onValueChange = { inputName = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Email",
                        tint = Color(0xFFFF9900)
                    )
                },
                label = {
                    Text(text = "Nombre Completo")
                },
                shape = RoundedCornerShape(12.dp),
                supportingText = {
                    if (nameError.isNotEmpty()) {
                        Text(
                            text = nameError,
                            color = Color.Red
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    autoCorrectEnabled = false,
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = inputEmail,
                onValueChange = { inputEmail = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email",
                        tint = Color(0xFFFF9900)
                    )
                },
                label = {
                    Text(text = "Correo Electrónico")
                },
                shape = RoundedCornerShape(12.dp),
                supportingText = {
                    if (emailError.isNotEmpty()) {
                        Text(
                            text = emailError,
                            color = Color.Red
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Email
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = inputPassword,
                onValueChange = { inputPassword = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Email",
                        tint = Color(0xFFFF9900)
                    )
                },
                label = {
                    Text(text = "Contraseña")
                },
                shape = RoundedCornerShape(12.dp),
                supportingText = {
                    if (passwordError.isNotEmpty()) {
                        Text(
                            text = passwordError,
                            color = Color.Red
                        )
                    }
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = inputPasswordConfirmation,
                onValueChange = { inputPasswordConfirmation = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Email",
                        tint = Color(0xFFFF9900)
                    )
                },
                label = {
                    Text(text = "Confirmar Contraseña")
                },
                shape = RoundedCornerShape(12.dp),
                supportingText = {
                    if (passwordConfirmationError.isNotEmpty()) {
                        Text(
                            text = passwordConfirmationError,
                            color = Color.Red
                        )
                    }
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (registerError.isNotEmpty()) {
                Text(
                    registerError,
                    color = Color.Red,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }
            Button(
                onClick = {
                    val isValidName: Boolean = validateName(inputName).first
                    val isValidEmail: Boolean = validateEmail(inputEmail).first
                    val isValidPassword = validatePassword(inputPassword).first
                    val isValidConfirmPassword: Boolean =
                        validatePasswordConfirmation(inputPassword, inputPasswordConfirmation).first

                    nameError = validateName(inputName).second
                    emailError = validateEmail(inputEmail).second
                    passwordError = validatePassword(inputPassword).second
                    passwordConfirmationError = validatePasswordConfirmation(
                        inputPassword,
                        inputPasswordConfirmation
                    ).second

                    if (isValidName && isValidEmail && isValidPassword && isValidConfirmPassword) {
                        auth.createUserWithEmailAndPassword(inputEmail, inputPassword)
                            .addOnCompleteListener(activity) { task ->
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    user?.let {
                                        val userDoc = hashMapOf(
                                            "name" to inputName,
                                            "email" to inputEmail,
                                            "role" to "driver"
                                        )
                                        db.collection("users").document(it.uid).set(userDoc)
                                    }
                                    onSuccessfulRegister()
                                } else {
                                    registerError = when (task.exception) {
                                        is FirebaseAuthInvalidCredentialsException -> "Correo inválido"
                                        is FirebaseAuthUserCollisionException -> "Correo ya registrado"
                                        else -> "Error al registrarse. Intenta de nuevo"
                                    }
                                }
                            }
                    } else {
                        registerError = "Hubo un error en el registro"
                    }

                }, modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9900))
            ) {
                Text("Registrarse")
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = {}) {
                Text(
                    text = "¿Ya tienes una cuenta? Inicia Sesión",
                    color = Color(0xFFFF9900)
                )
            }
        }
    }
}

@Preview
@Composable
fun RegisterScreenPreview() {
    //RegisterScreen()
}