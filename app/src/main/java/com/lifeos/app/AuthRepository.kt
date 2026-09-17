package com.lifeos.app

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val isSignedIn: Boolean
        get() = auth.currentUser != null

    fun addAuthStateListener(listener: (Boolean) -> Unit): FirebaseAuth.AuthStateListener {
        val authStateListener = FirebaseAuth.AuthStateListener { listener(it.currentUser != null) }
        auth.addAuthStateListener(authStateListener)
        return authStateListener
    }

    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.removeAuthStateListener(listener)
    }

    fun signIn(identifier: String, password: String, callback: (Result<Unit>) -> Unit) {
        resolveEmail(identifier) { emailResult ->
            emailResult.fold(
                onSuccess = { email ->
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { callback(Result.success(Unit)) }
                        .addOnFailureListener { callback(Result.failure(it)) }
                },
                onFailure = { callback(Result.failure(it)) }
            )
        }
    }

    fun signUp(
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        password: String,
        callback: (Result<Unit>) -> Unit
    ) {
        val normalizedUsername = username.trim().lowercase()
        val usernameRef = firestore.collection("usernames").document(normalizedUsername)
        usernameRef.get().addOnSuccessListener { existing ->
            if (existing.exists()) {
                callback(Result.failure(IllegalArgumentException("That username is already taken.")))
                return@addOnSuccessListener
            }

            auth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener { result ->
                    val userId = result.user?.uid
                    if (userId == null) {
                        callback(Result.failure(IllegalStateException("Could not create the account.")))
                        return@addOnSuccessListener
                    }

                    val profile = mapOf(
                        "firstName" to firstName.trim(),
                        "lastName" to lastName.trim(),
                        "username" to normalizedUsername,
                        "email" to email.trim()
                    )
                    val batch = firestore.batch()
                    batch.set(firestore.collection("users").document(userId), profile)
                    batch.set(usernameRef, mapOf("userId" to userId, "email" to email.trim()))
                    batch.commit()
                        .addOnSuccessListener { callback(Result.success(Unit)) }
                        .addOnFailureListener { callback(Result.failure(it)) }
                }
                .addOnFailureListener { callback(Result.failure(it)) }
        }.addOnFailureListener { callback(Result.failure(it)) }
    }

    fun signOut() = auth.signOut()

    fun signInWithGoogleIdToken(idToken: String, callback: (Result<Unit>) -> Unit) {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    private fun resolveEmail(identifier: String, callback: (Result<String>) -> Unit) {
        val trimmed = identifier.trim()
        if (trimmed.contains("@")) {
            callback(Result.success(trimmed))
            return
        }

        firestore.collection("usernames").document(trimmed.lowercase()).get()
            .addOnSuccessListener { document ->
                val email = document.getString("email")
                if (email.isNullOrBlank()) {
                    callback(Result.failure(IllegalArgumentException("No account was found for that username.")))
                } else {
                    callback(Result.success(email))
                }
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }
}
