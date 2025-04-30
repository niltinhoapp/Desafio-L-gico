package com.desafiolgico.auth

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.desafiolgico.information.OnboardingActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.desafiolgico.R

class LoginActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "LoginActivity"
        private const val GAME_PREFS = "game_prefs"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializa SharedPreferences
        sharedPreferences = getSharedPreferences(GAME_PREFS, MODE_PRIVATE)

        // Inicializa FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Configura Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Botão de Login com Google
        findViewById<SignInButton>(R.id.signInButton).setOnClickListener {
            signInWithGoogle()
        }

        // Botão para registro
        findViewById<TextView>(R.id.createAccountTextView).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(task: com.google.android.gms.tasks.Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                Log.d(TAG, "Google Sign-In bem-sucedido, autenticando no Firebase...")
                firebaseAuthWithGoogle(account.idToken!!)
            }
        } catch (e: ApiException) {
            Log.w(TAG, "Erro no Google Sign-In: ${e.statusCode}")
            showError("Erro ao fazer login: ${e.localizedMessage}")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    Log.w(TAG, "Erro na autenticação com Firebase.", task.exception)
                    showError("Autenticação falhou: ${task.exception?.localizedMessage}")
                    updateUI(null)
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // Obtém nome ou email
            val username = user.displayName ?: user.email ?: "Usuário"

            // Salva no SharedPreferences
            saveUsername(username)

            // Log para depuração
            Log.d(TAG, "Usuário logado: $username")

            // Navega para a próxima tela
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        } else {
            showError("Erro na autenticação. Por favor, tente novamente.")
        }
    }

    private fun saveUsername(username: String) {
        sharedPreferences.edit().putString("username", username).apply()
        Log.d(TAG, "Nome salvo: ${sharedPreferences.getString("username", "N/A")}")
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}