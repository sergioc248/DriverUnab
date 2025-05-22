package com.sergiocuadros.dannacarrillo.busunab

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.sergiocuadros.dannacarrillo.busunab.validateEmail
import com.sergiocuadros.dannacarrillo.busunab.validateName
import com.sergiocuadros.dannacarrillo.busunab.validatePassword
import com.sergiocuadros.dannacarrillo.busunab.validatePasswordConfirmation
import com.sergiocuadros.dannacarrillo.busunab.models.UserRole
import com.sergiocuadros.dannacarrillo.busunab.viewmodels.RegisterViewModel
import com.sergiocuadros.dannacarrillo.busunab.viewmodels.RegistrationState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.interaction.MutableInteractionSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onClickBack: () -> Unit = {},
    onSuccessfulRegister: (UserRole) -> Unit = {}
) {
    val registerViewModel: RegisterViewModel = viewModel()
    val registrationState by registerViewModel.registrationState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // ESTADOS
    var inputName by remember { mutableStateOf("") }
    var inputEmail by remember { mutableStateOf("") }
    var inputPassword by remember { mutableStateOf("") }
    var inputPasswordConfirmation by remember { mutableStateOf("") }
    var isAdminUser by remember { mutableStateOf(false) }

    var generalError by remember { mutableStateOf("") } // Consolidated error

    // Individual field errors are now primarily for highlighting fields if needed,
    // but generalError will show the main message.
    var nameFieldError by remember { mutableStateOf("") }
    var emailFieldError by remember { mutableStateOf("") }
    var passwordFieldError by remember { mutableStateOf("") }
    var passwordConfirmationFieldError by remember { mutableStateOf("") }


    LaunchedEffect(registrationState) {
        when (val state = registrationState) {
            is RegistrationState.Success -> {
                onSuccessfulRegister(state.role)
            }
            is RegistrationState.Error -> {
                generalError = state.message // Use generalError for ViewModel errors
            }
            else -> {
                generalError = ""
            }
        }
    }

    fun handleRegistration() {
        keyboardController?.hide()
        focusManager.clearFocus()

        // Reset previous errors
        nameFieldError = ""
        emailFieldError = ""
        passwordFieldError = ""
        passwordConfirmationFieldError = ""
        generalError = ""

        val nameValidation = validateName(inputName)
        val emailValidation = validateEmail(inputEmail)
        val passwordValidation = validatePassword(inputPassword)
        val passwordConfirmationValidation = validatePasswordConfirmation(inputPassword, inputPasswordConfirmation)

        var isValid = true

        if (!nameValidation.first) {
            nameFieldError = nameValidation.second
            isValid = false
        }
        if (!emailValidation.first) {
            emailFieldError = emailValidation.second
            isValid = false
        }
        if (!passwordValidation.first) {
            passwordFieldError = passwordValidation.second
            isValid = false
        }
        if (!passwordConfirmationValidation.first) {
            passwordConfirmationFieldError = passwordConfirmationValidation.second
            isValid = false
        }

        if (isValid) {
            registerViewModel.registerUser(inputName, inputEmail, inputPassword, isAdminUser)
        } else {
            generalError = "Por favor, corrige los errores en el formulario."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = onClickBack
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
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
            verticalArrangement = Arrangement.Center // Keep centered if content is short
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
                onValueChange = { inputName = it; generalError = ""; nameFieldError = "" },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Nombre",
                        tint = Color(0xFFFF9900)
                    )
                },
                label = { Text("Nombre Completo") },
                shape = RoundedCornerShape(12.dp),
                isError = nameFieldError.isNotEmpty(),
                supportingText = {
                    if (nameFieldError.isNotEmpty()) {
                        Text(nameFieldError, color = MaterialTheme.colorScheme.error)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp)) // Reduced spacer

            OutlinedTextField(
                value = inputEmail,
                onValueChange = { inputEmail = it; generalError = ""; emailFieldError = "" },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email",
                        tint = Color(0xFFFF9900)
                    )
                },
                label = { Text("Correo Electrónico") },
                shape = RoundedCornerShape(12.dp),
                isError = emailFieldError.isNotEmpty(),
                supportingText = {
                    if (emailFieldError.isNotEmpty()) {
                        Text(emailFieldError, color = MaterialTheme.colorScheme.error)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp)) // Reduced spacer

            OutlinedTextField(
                value = inputPassword,
                onValueChange = { inputPassword = it; generalError = ""; passwordFieldError = "" },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Contraseña",
                        tint = Color(0xFFFF9900)
                    )
                },
                label = { Text("Contraseña") },
                shape = RoundedCornerShape(12.dp),
                isError = passwordFieldError.isNotEmpty(),
                supportingText = {
                    if (passwordFieldError.isNotEmpty()) {
                        Text(passwordFieldError, color = MaterialTheme.colorScheme.error)
                    }
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp)) // Reduced spacer

            OutlinedTextField(
                value = inputPasswordConfirmation,
                onValueChange = { inputPasswordConfirmation = it; generalError = ""; passwordConfirmationFieldError = "" },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Confirmar Contraseña",
                        tint = Color(0xFFFF9900)
                    )
                },
                label = { Text("Confirmar Contraseña") },
                shape = RoundedCornerShape(12.dp),
                isError = passwordConfirmationFieldError.isNotEmpty(),
                supportingText = {
                    if (passwordConfirmationFieldError.isNotEmpty()) {
                        Text(passwordConfirmationFieldError, color = MaterialTheme.colorScheme.error)
                    }
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done // Done for the last field
                ),
                keyboardActions = KeyboardActions(
                    onDone = { handleRegistration() }
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isAdminUser,
                    onCheckedChange = { isAdminUser = it }
                )
                Text(text = "Crear como Administrador")
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (generalError.isNotEmpty()) {
                Text(
                    generalError,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            } else if (registrationState != RegistrationState.Loading) {
                 // Keep space consistent if no error and not loading
                Spacer(modifier = Modifier.height(32.dp)) // Approx height of error text + padding
            }

            Button(
                onClick = { handleRegistration() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = registrationState != RegistrationState.Loading
            ) {
                if (registrationState == RegistrationState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Crear Cuenta")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onClickBack) { // Changed to onClickBack to avoid confusion
                Text(
                    text = "¿Ya tienes una cuenta? Inicia Sesión",
                    color = Color(0xFFFF9900)
                )
            }
        }
    }
}