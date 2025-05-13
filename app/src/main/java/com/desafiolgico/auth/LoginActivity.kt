package com.desafiolgico.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.Log
import android.widget.Toast
import androidx.core.view.isVisible


import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.lifecycleScope
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityLoginBinding
import com.desafiolgico.main.BoasVindasActivity
import com.desafiolgico.utils.CoinManager
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.UserManager
import com.google.android.gms.ads.MobileAds
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit
import androidx.core.net.toUri
import com.desafiolgico.information.OnboardingActivity
import com.desafiolgico.utils.applyEdgeToEdge
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding
    private lateinit var credentialManager: CredentialManager

    companion object {
        private const val TAG = "LoginActivity"
        private const val URL_TERMOS_DE_SERVICO = "https://www.desafiologico.com/terms"
        private const val URL_POLITICA_DE_PRIVACIDADE = "https://www.desafiologico.com/privacy"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.guestButton.isVisible = resources.getBoolean(R.bool.show_guest)




        // AdMob
        MobileAds.initialize(this) { Log.i(TAG, "✅ AdMob inicializado.") }

        // Game managers
        GameDataManager.init(this)
        auth = FirebaseAuth.getInstance()

        // Credential Manager
        credentialManager = CredentialManager.create(this)

        // Botão principal
        binding.signInButton.setOnClickListener { signInWithCredentialManager() }

        binding.guestButton.setOnClickListener {
            Toast.makeText(this, "Entrando como convidado...", Toast.LENGTH_SHORT).show()

            GameDataManager.setActiveUserId(this, "guest_mode")

            GameDataManager.saveUserData(
                context = this,
                username = "Convidado",
                photoUrl = null,
                avatarId = R.drawable.avatar1
            )

            UserManager.salvarDadosUsuario(
                context = this,
                nome = "Convidado",
                email = "guest@desafiologico.com",
                photoUrl = null,
                avatarId = R.drawable.avatar1
            )

            // prefs
            getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
                .putBoolean("is_guest_mode", true)
                // deixa onboarding_completed como false (primeira vez)
                .apply()

            goToNextAfterLogin()
        }
            // Links legais
        setupLegalLinks()
    }

    private fun loginComoConvidado() {
        Toast.makeText(this, "Entrando como convidado...", Toast.LENGTH_SHORT).show()

        GameDataManager.setActiveUserId(this, "guest_mode")

        GameDataManager.saveUserData(
            context = this,
            username = "Convidado",
            photoUrl = null,
            avatarId = R.drawable.avatar1
        )

        UserManager.salvarDadosUsuario(
            context = this,
            nome = "Convidado",
            email = "guest@desafiologico.com",
            photoUrl = null,
            avatarId = R.drawable.avatar1
        )

        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
            .putBoolean("is_guest_mode", true)
            .apply()

        goToNextAfterLogin()
    }


    /** --------- UI util --------- **/
    private fun setupLegalLinks() {
        val termsText = SpannableString(binding.termsOfServiceLink.text).apply {
            setSpan(UnderlineSpan(), 0, length, 0)
        }
        val privacyText = SpannableString(binding.privacyPolicyLink.text).apply {
            setSpan(UnderlineSpan(), 0, length, 0)
        }
        binding.termsOfServiceLink.text = termsText
        binding.privacyPolicyLink.text = privacyText

        binding.termsOfServiceLink.setOnClickListener { openUrl(URL_TERMOS_DE_SERVICO) }
        binding.privacyPolicyLink.setOnClickListener { openUrl(URL_POLITICA_DE_PRIVACIDADE) }
    }

    private fun openUrl(url: String) {
        try {
            CustomTabsIntent.Builder().build().launchUrl(this, url.toUri())
        } catch (e: Exception) {
            Log.w(TAG, "CustomTabs falhou: ${e.message}")
            try { startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
            catch (_: Exception) { Toast.makeText(this, "Não foi possível abrir o link.", Toast.LENGTH_SHORT).show() }
        }
    }

    /** --------- NOVO: Credential Manager Google Sign-In --------- **/
    private fun signInWithCredentialManager() {
        // Opção GoogleId (One Tap / Autopreenchimento moderno)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)                 // mostra também contas não-autorizadas previamente
            .setAutoSelectEnabled(false)                          // se quiser login 1-toque, pode true
            .setServerClientId(getString(R.string.default_web_client_id)) // do google-services.json
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                // Chamada suspensa
                val response: GetCredentialResponse = withContext(Dispatchers.IO) {
                    credentialManager.getCredential(this@LoginActivity, request)
                }
                handleCredentialResponse(response)
            } catch (e: GetCredentialException) {
                // Usuário cancelou, não há provedores, etc.
                Log.w(TAG, "GetCredential falhou: ${e::class.simpleName} / ${e.message}")
                showError("Não foi possível continuar com o login.")
            } catch (t: Throwable) {
                Log.e(TAG, "Erro inesperado no Credential Manager", t)
                showError("Erro inesperado no login.")
            }
        }
    }

    private fun goToNextAfterLogin() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val completed = prefs.getBoolean("onboarding_completed", false)
        val alwaysShow = prefs.getBoolean("always_show_onboarding", false)

        if (!completed || alwaysShow) {
            startActivity(
                Intent(this, com.desafiolgico.information.OnboardingActivity::class.java)
                    .putExtra("FROM_SETTINGS", false) // veio do Login
            )
        } else {
            startActivity(Intent(this, com.desafiolgico.main.BoasVindasActivity::class.java))
        }
        finish()
    }


    private fun handleCredentialResponse(response: GetCredentialResponse) {
        val credential: Credential = response.credential
        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    signInWithFirebase(idToken, googleIdTokenCredential)
                } else {
                    Log.w(TAG, "Tipo CustomCredential não suportado: ${credential.type}")
                    showError("Provedor não suportado.")
                }
            }
            else -> {
                // (Opcional) tratar PasswordCredential etc., se quiser suportar.
                Log.w(TAG, "Credential não suportada: ${credential::class.java.simpleName}")
                showError("Provedor não suportado.")
            }
        }
    }

    private fun signInWithFirebase(idToken: String, googleCred: GoogleIdTokenCredential) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val user = task.result.user
                GameDataManager.setActiveUserId(this, user?.uid)

                Log.d(TAG, "✅ Firebase login OK. UID=${user?.uid}")

                val username = googleCred.displayName
                    ?: user?.displayName
                    ?: user?.email
                    ?: "Jogador"

                val photoUrl = googleCred.profilePictureUri?.toString()
                    ?: user?.photoUrl?.toString()

                // 🔹 Carrega avatar salvo (se existir)
                val (_, _, savedAvatar) = GameDataManager.loadUserData(this)
                val avatarToUse = savedAvatar ?: R.drawable.avatar1

                // 🔹 Salva dados SEM sobrescrever avatar existente
                GameDataManager.saveUserData(
                    context = this,
                    username = username,
                    photoUrl = photoUrl,
                    avatarId = avatarToUse
                )

                UserManager.salvarDadosUsuario(
                    context = this,
                    nome = username,
                    email = user?.email,
                    photoUrl = photoUrl,
                    avatarId = avatarToUse
                )



                Toast.makeText(this, "Bem-vindo, $username!", Toast.LENGTH_SHORT).show()

                getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
                    .putBoolean("is_guest_mode", false)

                    .apply()

                goToNextAfterLogin()

            }
            else {
                Log.e(TAG, "FirebaseAuth falhou", task.exception)
                showError("Falha na autenticação: ${task.exception?.localizedMessage}")
            }
        }
    }



    /** --------- Logout seguro --------- **/
    private fun signOut(onSignedOut: () -> Unit) {
        // Limpa sessão do Firebase
        auth.signOut()

        // (Opcional) Você pode também limpar estado de credenciais se necessário:
        // No fluxo do Google ID Token geralmente não é estritamente necessário.

        GameDataManager.setActiveUserId(this, "guest")

        Log.d(TAG, "Usuário deslogado. Sessão limpa.")
        onSignedOut()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
