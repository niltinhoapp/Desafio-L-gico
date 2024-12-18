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
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
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

        // Shared Preferences
        sharedPreferences = getSharedPreferences(GAME_PREFS, MODE_PRIVATE)

        // Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Configuração do Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Corrigir o tipo do botão para SignInButton
        findViewById<SignInButton>(R.id.signInButton).setOnClickListener {
            signInWithGoogle()
        }

        // Botão de Login com Email e Senha
        findViewById<MaterialButton>(R.id.loginButton).setOnClickListener {
            val email = findViewById<TextInputEditText>(R.id.emailEditText).text.toString()
            val password = findViewById<TextInputEditText>(R.id.passwordEditText).text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                signInWithEmail(email, password)
            } else {
                showError("Por favor, preencha todos os campos")
            }
        }

        // Navegação para criação de conta
        findViewById<TextView>(R.id.createAccountTextView).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // Função para login com Google
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    // Recebe o resultado da atividade de login
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    // Processa o resultado do Google Sign-In
    private fun handleSignInResult(task: com.google.android.gms.tasks.Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.w(TAG, "Erro no Google Sign-In: ${e.statusCode}")
            showError("Erro no login com Google: ${e.localizedMessage}")
        }
    }

    // Autentica no Firebase com Google
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    Log.w(TAG, "Erro na autenticação com Google", task.exception)
                    showError("Autenticação falhou: ${task.exception?.localizedMessage}")
                    updateUI(null)
                }
            }
    }

    // Login com Email e Senha
    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    Log.w(TAG, "Erro na autenticação com Email", task.exception)
                    showError("Falha no login: ${task.exception?.localizedMessage}")
                    updateUI(null)
                }
            }
    }

    // Atualiza a interface do usuário após login
    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // Salva o nome de usuário (opcional)
            val username = user.displayName ?: user.email ?: "Usuário"
            saveUsername(username)

            // Navega para a próxima atividade
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        } else {
            showError("Erro na autenticação. Tente novamente.")
        }
    }

    // Salva o nome de usuário
    private fun saveUsername(username: String) {
        sharedPreferences.edit().apply {
            putString("username", username)
            apply()
        }
        Log.d("SaveUsername", "Nome de usuário salvo: $username")
    }

    // Método separado para exibir mensagens de erro
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
