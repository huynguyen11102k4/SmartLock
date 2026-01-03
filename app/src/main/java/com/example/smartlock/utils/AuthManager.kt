package com.example.smartlock.utils

import com.example.smartlock.model.entity.User

object AuthManager {
    private var currentUser: User? = null

    fun setCurrentUser(user: User) {
        currentUser = user
    }

    fun getCurrentUser(): User? {
        return currentUser
    }

    fun isLoggedIn(): Boolean {
        return currentUser != null
    }

    fun logout() {
        currentUser = null
    }
}