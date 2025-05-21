package com.sergiocuadros.dannacarrillo.busunab.models

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: UserRole = UserRole.DRIVER,
)

enum class UserRole {
    ADMIN,
    DRIVER
} 