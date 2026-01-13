package com.desafiolgico.auth

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.Choreographer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityLoginBinding
import com.desafiolgico.utils.AdMobInitializer
import com.desafiolgico.utils.CrashlyticsHelper
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.UserManager
import com.desafiolgico.utils.applyEdgeToEdge
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    // ✅ Lazy: só cria quando realmente precisar (clique no login / uso do auth)
    private val auth: FirebaseAuth by lazy(LazyThreadSafetyMode.NONE) {
        FirebaseApp.initializeApp(applicationContext)
        FirebaseAuth.getInstance()
    }
    private val credentialManager: CredentialManager by lazy(LazyThreadSafetyMode.NONE) { CredentialManager.create(this) }

    companion object {
        private const val TAG = "LoginActivity"
        private const val URL_TERMOS_DE_SERVICO = "https://www.desafiologico.com/terms"
        private const val URL_POLITICA_DE_PRIVACIDADE = "https://www.desafiologico.com/privacy"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val t0 = SystemClock.elapsedRealtime()
        super.onCreate(savedInstanceState)

        applyEdgeToEdge()

        val tInflate = SystemClock.elapsedRealtime()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "⏱ inflate+setContentView: ${SystemClock.elapsedRealtime() - tInflate}ms")

        // ✅ segura a animação no primeiro frame (melhora abertura)
        binding.lottieAnimationView.cancelAnimation()
        binding.lottieAnimationView.progress = 0f
        binding.lottieAnimationView.isVisible = false

        Choreographer.getInstance().postFrameCallback {
            binding.lottieAnimationView.isVisible = true
            binding.lottieAnimationView.playAnimation()
        }


        binding.guestButton.isVisible = resources.getBoolean(R.bool.show_guest)

        // ✅ mede o tempo real até o 1º frame (bem útil pra cold start)
        Choreographer.getInstance().postFrameCallback {
            Log.d(TAG, "🎬 first frame: ${SystemClock.elapsedRealtime() - t0}ms desde onCreate()")
        }

        // ✅ Links legais fora do caminho crítico do 1º frame
        binding.root.post {
            val tLinks = SystemClock.elapsedRealtime()
            setupLegalLinks()
            Log.d(TAG, "⏱ setupLegalLinks(post): ${SystemClock.elapsedRealtime() - tLinks}ms")
        }

        // ✅ clique: aqui sim faz lazy init do CredentialManager
        binding.signInButton.setOnClickListener { signInWithCredentialManager() }

        binding.guestButton.setOnClickListener {
            Toast.makeText(this, "Entrando como convidado...", Toast.LENGTH_SHORT).show()

            // troca de tela primeiro (percepção melhor)
            goToNextAfterLogin()

            // gravações em background depois
            Thread {
                ensureGameData()
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

                AdMobInitializer.ensureInitialized(applicationContext)
            }.start()
        }

    }

    private fun ensureGameData() {
        val t = SystemClock.elapsedRealtime()
        GameDataManager.init(applicationContext)
        Log.d(TAG, "⏱ GameDataManager.init(lazy): ${SystemClock.elapsedRealtime() - t}ms")
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

    /** --------- Credential Manager Google Sign-In --------- **/
    private fun signInWithCredentialManager() {
        val tCM = SystemClock.elapsedRealtime()
        val cm = credentialManager // ✅ força lazy init aqui
        Log.d(TAG, "⏱ CredentialManager.create(lazy): ${SystemClock.elapsedRealtime() - tCM}ms")

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val response: GetCredentialResponse = withContext(Dispatchers.IO) {
                    cm.getCredential(this@LoginActivity, request)
                }
                handleCredentialResponse(response)
            } catch (e: GetCredentialException) {
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
                    .putExtra("FROM_SETTINGS", false)
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
                Log.w(TAG, "Credential não suportada: ${credential::class.java.simpleName}")
                showError("Provedor não suportado.")
            }
        }
    }

    private fun signInWithFirebase(idToken: String, googleCred: GoogleIdTokenCredential) {
        val tAuth = SystemClock.elapsedRealtime()
        val firebase = auth // ✅ força lazy init aqui
        Log.d(TAG, "⏱ FirebaseAuth.getInstance(lazy): ${SystemClock.elapsedRealtime() - tAuth}ms")

        val credential = GoogleAuthProvider.getCredential(idToken, null)

        firebase.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (!task.isSuccessful) {
                Log.e(TAG, "FirebaseAuth falhou", task.exception)
                showError("Falha na autenticação: ${task.exception?.localizedMessage}")
                return@addOnCompleteListener
            }

            val user = task.result.user

            // 1) Garante dados de jogo e define o userId ativo
            ensureGameData()
            GameDataManager.setActiveUserId(this, user?.uid)

            Log.d(TAG, "✅ Firebase login OK. UID=${user?.uid}")

            // 2) Dados do usuário
            val username = googleCred.displayName
                ?: user?.displayName
                ?: user?.email
                ?: "Jogador"

            val photoUrl = googleCred.profilePictureUri?.toString()
                ?: user?.photoUrl?.toString()

            // 3) Mantém avatar salvo (se existir)
            val (_, _, savedAvatar) = GameDataManager.loadUserData(this)
            val avatarToUse = savedAvatar ?: R.drawable.avatar1

            // 4) Persiste dados
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

            getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
                .putBoolean("is_guest_mode", false)
                .apply()

            // 5) Crashlytics: contexto do usuário (após login, fora do cold start)
            CrashlyticsHelper.setUserContext(
                context = applicationContext,
                userId = GameDataManager.currentUserId,
                email = user?.email,
                username = username
            )

            // Estado do jogo em background (não trava UI)

            Thread { CrashlyticsHelper.setGameState(applicationContext) }.start()
            Thread { CrashlyticsHelper.enrichWithUmp(applicationContext) }.start() // só se Ads inicializou


            // 6) AdMob só depois do login
            AdMobInitializer.ensureInitialized(applicationContext)

            // 7) UMP só junto com Ads (também fora da UI thread)
            Thread { CrashlyticsHelper.enrichWithUmp(applicationContext) }.start()

            Toast.makeText(this, "Bem-vindo, $username!", Toast.LENGTH_SHORT).show()
            goToNextAfterLogin()
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
