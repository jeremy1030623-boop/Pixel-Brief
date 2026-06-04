package com.example.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

object GoogleSignInHelper {
    // A standard Web Client ID for Google Workspace authentication or simulation.
    private const val WEB_CLIENT_ID = "1042767011985-oauth2client.apps.googleusercontent.com"

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestIdToken(WEB_CLIENT_ID)
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun parseSignInResult(intent: Intent?): GoogleSignInAccount? {
        if (intent == null) return null
        val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(intent)
        return try {
            task.getResult(ApiException::class.java)
        } catch (e: Exception) {
            android.util.Log.e("GoogleSignInHelper", "Sign in result parsed failed or direct exception: ${e.message}", e)
            null
        }
    }
}
