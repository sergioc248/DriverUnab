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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.auth
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.interaction.MutableInteractionSource

@Composable
fun LoginScreen(onClickRegister: () -> Unit = {}, onSuccesfulLogin: () -> Unit = {}) {

    val auth = Firebase.auth
    val activity = LocalView.current.context as Activity
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // ESTADOS
    var inputEmail by remember { mutableStateOf("") }
    var inputPassword by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

    fun handleLogin() {
        keyboardController?.hide()
        focusManager.clearFocus()
        Log.d("LoginScreen_DEBUG", "Login Button Clicked - Top of onClick")
        val isValidEmail: Boolean = validateEmail(inputEmail).first
        val isValidPassword = validatePassword(inputPassword).first
        emailError = validateEmail(inputEmail).second
        passwordError = validatePassword(inputPassword).second

        if (isValidEmail && isValidPassword) {
            Log.d("LoginScreen", "Attempting to sign in with email: $inputEmail")
            auth.signInWithEmailAndPassword(inputEmail, inputPassword)
                .addOnCompleteListener(activity) { task ->
                    Log.d("LoginScreen", "signInWithEmailAndPassword onComplete. Successful: ${task.isSuccessful}")
                    if (task.isSuccessful) {
                        Log.d("LoginScreen", "Login successful. Calling onSuccesfulLogin().")
                        onSuccesfulLogin()
                    } else {
                        Log.e("LoginScreen", "Login failed.", task.exception)
                        loginError = when (task.exception) {
                            is FirebaseAuthInvalidCredentialsException -> "Correo o Contraseña incorrecta"
                            is FirebaseAuthInvalidUserException -> "No existe una cuenta con este correo"
                            else -> "Error al iniciar sesión. Intenta de nuevo"
                        }
                    }
                }
        } else {
            if (!isValidEmail && !isValidPassword) {
                loginError = "Correo y contraseña inválidos."
            } else if (!isValidEmail) {
                loginError = "Formato de correo inválido."
            } else {
                loginError = "Formato de contraseña inválido."
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF00B2F3) // Fondo azul brillante
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                )
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            Image(
                painter = painterResource(R.drawable.logo_unab_blanco),
                contentDescription = "Logo Unab",
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "INICIO DE SESIÓN",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "¡Bienvenido de vuelta!",
                fontSize = 14.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(26.dp))

            OutlinedTextField(
                value = inputEmail,
                onValueChange = { inputEmail = it; loginError = "" },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                placeholder = { Text("Email", color = Color(0xFF666666)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = inputPassword,
                onValueChange = { inputPassword = it; loginError = "" },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                placeholder = { Text("Password", color = Color(0xFF666666)) },
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { handleLogin() }
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (loginError.isNotEmpty()) {
                Text(
                    loginError,
                    color = Color.Red.copy(alpha = 0.8f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            Button(
                onClick = { handleLogin() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0076A5))
            ) {
                Text("Iniciar Sesión", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onClickRegister) {
                Text(
                    text = "¿No tienes cuenta? Regístrate",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
