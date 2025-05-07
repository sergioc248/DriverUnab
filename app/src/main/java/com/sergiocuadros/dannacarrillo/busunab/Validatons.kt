package com.sergiocuadros.dannacarrillo.busunab

import android.util.Patterns

// Retornar true si es válida y un false si no es valida
// Tambien retorna una cadena que me diga que paso, si no es valido
fun validateEmail(email: String): Pair<Boolean, String> {
    return when {
        email.isEmpty() -> Pair(false, "El correo es obligatorio")
        !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> Pair(false, "El correo no es válido")
        !email.endsWith("@unab.edu.co") -> Pair(false, "El correo debe ser unab")
        else -> Pair(true, "")
    }
}

fun validatePassword(password: String): Pair<Boolean, String> {
    return when {
        password.isEmpty() -> Pair(false, "La contraseña es obligatoria")
        password.length < 8 -> Pair(false, "La contraseña debe tener al menos 8 caracteres")
        !password.any { it.isDigit() } -> Pair(
            false,
            "La contraseña debe contener al menos un dígito"
        )

        else -> Pair(true, "")
    }
}

fun validatePasswordConfirmation(password: String, confirmPassword: String): Pair<Boolean, String> {
    return when {
        confirmPassword.isEmpty() -> Pair(false, "La contraseña es obligatoria")
        confirmPassword != password -> Pair(false, "Las contraseñas no coinciden")
        else -> Pair(true, "")
    }
}


fun validateName(name: String): Pair<Boolean, String> {
    return when {
        name.isEmpty() -> Pair(false, "El nombre es obligatorio")
        name.length < 3 -> Pair(false, "El nombre debe tener al menos 3 caracteres")
        else -> Pair(true, "")
    }
}