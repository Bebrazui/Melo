package com.melo.music.auth

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Flavor google: полная реализация Google Sign-In.
 * Инициализируется в MainActivity.onCreate().
 */
object GoogleAuthHelper {

    private lateinit var launcher: ActivityResultLauncher<Intent>
    private var result: CompletableDeferred<Result<Unit>>? = null

    fun init(activity: ComponentActivity) {
        launcher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { res ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(res.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    result?.complete(Result.failure(Exception("Google не вернул idToken")))
                } else {
                    val photo = account.photoUrl?.toString()
                    val display = account.displayName
                    activity.lifecycleScope.launch {
                        result?.complete(AuthManager.loginWithGoogleIdToken(idToken, photo, display))
                    }
                }
            } catch (e: ApiException) {
                android.util.Log.e("MeloAuth", "GoogleSignIn failed code=${e.statusCode}", e)
                result?.complete(Result.failure(Exception("Google: код ${e.statusCode}")))
            }
        }
    }

    suspend fun signIn(activity: ComponentActivity): Result<Unit> {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(AuthManager.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(activity, gso)
        runCatching { client.signOut() }
        val deferred = CompletableDeferred<Result<Unit>>()
        result = deferred
        launcher.launch(client.signInIntent)
        return deferred.await()
    }
}
