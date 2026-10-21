package com.unaj.appmoviles.features.auth.google

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.unaj.appmoviles.obratrack.core.domain.error.AuthError
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

sealed interface GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data class Error(val error: AuthError) : GoogleSignInResult
}

object GoogleSignInHelper {

    private const val TAG = "GoogleSignIn"

    suspend fun signIn(context: Context): GoogleSignInResult {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId == 0) {
            Log.e(TAG, "default_web_client_id resource not found in package ${context.packageName}")
            return GoogleSignInResult.Error(AuthError.GoogleConfigurationError)
        }
        val serverClientId = context.getString(resId)
        Log.d(TAG, "Using serverClientId: $serverClientId")

        return try {
            val credentialManager = CredentialManager.create(context)

            val rawNonce = UUID.randomUUID().toString()
            val bytes = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray())
            val nonce = bytes.joinToString("") { "%02x".format(it) }

            val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .setNonce(nonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Log.d(TAG, "Google Sign-In successful, idToken obtained")
                GoogleSignInResult.Success(googleIdTokenCredential.idToken)
            } else {
                Log.e(TAG, "Unexpected credential type: ${credential.type}")
                GoogleSignInResult.Error(AuthError.GoogleSignInFailed)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "User or system cancelled Google Sign-In: ${e.message}")
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.e(TAG, "No Google accounts available or no credentials: ${e.message}", e)
            GoogleSignInResult.Error(AuthError.GoogleNoAccounts)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "GetCredentialException during Google Sign-In: ${e.message}", e)
            GoogleSignInResult.Error(AuthError.GoogleSignInFailed)
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "Failed to parse Google ID token: ${e.message}", e)
            GoogleSignInResult.Error(AuthError.GoogleSignInFailed)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception during Google Sign-In: ${e.message}", e)
            GoogleSignInResult.Error(AuthError.GoogleSignInFailed)
        }
    }
}
