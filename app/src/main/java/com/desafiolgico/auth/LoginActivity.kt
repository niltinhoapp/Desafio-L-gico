package com.desafiolgico.auth

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.Choreographer
import android.view.View
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

    private var dotsAnimator: android.animation.ValueAnimator? = null

    // ✅ Lazy: só cria quando realmente precisar (clique no login / uso do auth)
    private val auth: FirebaseAuth by lazy(LazyThreadSafetyMode.NONE) {
        FirebaseApp.initializeApp(applicationContext)
        FirebaseAuth.getInstance()
    }

    private val credentialManager: CredentialManager by lazy(LazyThreadSafetyMode.NONE) {
        CredentialManager.create(this)
    }

    companion object {
        private const val TAG = "LoginActivity"
        private const val URL_TERMOS_DE_SERVICO = "https://www.desafiologico.com/terms"
        private const val URL_POLITICA_DE_PRIVACIDADE = "https://www.desafiologico.com/privacy"
    }

    private enum class LoginStep { GOOGLE, FIREBASE, PREPARANDO }

    override fun onCreate(savedInstanceState: Bundle?) {
        val t0 = SystemClock.elapsedRealtime()
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()

        FirebaseApp.initializeApp(applicationContext)

        val opts = FirebaseApp.getInstance().options
        Log.e("FBKEY", "appId=${opts.applicationId}")
        Log.e("FBKEY", "apiKey=${opts.apiKey}")
        Log.e("FBKEY", "projectId=${opts.projectId}")


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

        // ✅ mede o tempo real até o 1º frame
        Choreographer.getInstance().postFrameCallback {
            Log.d(TAG, "🎬 first frame: ${SystemClock.elapsedRealtime() - t0}ms desde onCreate()")
        }

        // ✅ Links legais fora do caminho crítico do 1º frame
        binding.root.post {
            val tLinks = SystemClock.elapsedRealtime()
            setupLegalLinks()
            Log.d(TAG, "⏱ setupLegalLinks(post): ${SystemClock.elapsedRealtime() - tLinks}ms")
        }

        binding.signInButton.setOnClickListener {
            setLoading(true, "Conectando com o Google", "Aguarde um instante")
            setStep(LoginStep.GOOGLE)
            signInWithCredentialManager()
        }



            binding.guestButton.setOnClickListener {
                setLoading(true, "Entrando como convidado", "Preparando sua sessão")
                setStep(LoginStep.PREPARANDO)

                // troca de tela primeiro (percepção melhor)
                goToNextAfterLogin()

                // gravações em background depois (com cancelamento automático)
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        ensureGameData()

                        // ✅ tudo que é "persistência / init" fica no IO
                        GameDataManager.setActiveUserId(this@LoginActivity, "guest_mode")

                        GameDataManager.saveUserData(
                            context = this@LoginActivity,
                            username = "Convidado",
                            photoUrl = null,
                            avatarId = R.drawable.avatar1
                        )

                        UserManager.salvarDadosUsuario(
                            context = this@LoginActivity,
                            nome = "Convidado",
                            email = "guest@desafiologico.com",
                            photoUrl = null,
                            avatarId = R.drawable.avatar1
                        )

                        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
                            .putBoolean("is_guest_mode", true)
                            .apply()

                        AdMobInitializer.ensureInitialized(applicationContext)
                    }

                    // (opcional) se quiser dar “ok” final e ainda estiver vivo:
                    if (!isFinishing && !isDestroyed) {
                        // setLoading(false) etc, se fizer sentido no seu fluxo
                    }
                }
            }

    }

    private fun ensureGameData() {
        val t = SystemClock.elapsedRealtime()
        GameDataManager.init(applicationContext)
        Log.d(TAG, "⏱ GameDataManager.init(lazy): ${SystemClock.elapsedRealtime() - t}ms")
    }

    // =============================================================================================
    // LINKS LEGAIS
    // =============================================================================================

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
            try {
                startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            } catch (_: Exception) {
                Toast.makeText(this, "Não foi possível abrir o link.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // =============================================================================================
    // CREDENTIAL MANAGER
    // =============================================================================================

    private fun signInWithCredentialManager() {
        setStep(LoginStep.GOOGLE)
        val tCM = SystemClock.elapsedRealtime()
        val cm = credentialManager
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

    private fun handleCredentialResponse(response: GetCredentialResponse) {
        val credential: Credential = response.credential
        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    // ✅ próxima etapa
                    setStep(LoginStep.FIREBASE)
                    binding.loginLoadingTitle.text = "Validando sua conta"
                    binding.loginLoadingText.text = "Finalizando autenticação"

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

    // =============================================================================================
    // FIREBASE
    // =============================================================================================

    private fun signInWithFirebase(idToken: String, googleCred: GoogleIdTokenCredential) {
        setStep(LoginStep.FIREBASE)

        val tAuth = SystemClock.elapsedRealtime()
        val firebase = auth
        Log.d(TAG, "⏱ FirebaseAuth.getInstance(lazy): ${SystemClock.elapsedRealtime() - tAuth}ms")

        val credential = GoogleAuthProvider.getCredential(idToken, null)

        firebase.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (!task.isSuccessful) {
                Log.e(TAG, "FirebaseAuth falhou", task.exception)
                showError("Falha na autenticação: ${task.exception?.localizedMessage ?: "tente novamente"}")
                return@addOnCompleteListener
            }

            val user = task.result.user

            // ✅ etapa final (preparando ambiente)
            setStep(LoginStep.PREPARANDO)
            binding.loginLoadingTitle.text = "Preparando sua sessão"
            binding.loginLoadingText.text = "Carregando seus dados"

            // 1) Dados do jogo e userId ativo
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

            // 3) Mantém avatar salvo
            val (_, _, savedAvatar) = GameDataManager.loadUserData(this)
            val avatarToUse = savedAvatar ?: R.drawable.avatar1

            // 4) Persiste
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

            // 5) Crashlytics
            CrashlyticsHelper.setUserContext(
                context = applicationContext,
                userId = GameDataManager.currentUserId,
                email = user?.email,
                username = username
            )

            // Background (não trava UI)
            Thread { CrashlyticsHelper.setGameState(applicationContext) }.start()

            // 6) AdMob pós login
            AdMobInitializer.ensureInitialized(applicationContext)

            // 7) UMP (não duplicar)
            Thread { CrashlyticsHelper.enrichWithUmp(applicationContext) }.start()

            Toast.makeText(this, "Bem-vindo, $username!", Toast.LENGTH_SHORT).show()

            // ✅ desliga overlay antes de trocar de tela
            setLoading(false, "", "")
            goToNextAfterLogin()
        }
    }

    // =============================================================================================
    // NAV
    // =============================================================================================

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

    // =============================================================================================
    // UI: LOADING / ETAPAS
    // =============================================================================================

    private fun showError(message: String) {
        setLoading(false, "", "")
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setLoading(show: Boolean, title: String, baseText: String) {
        binding.loginLoadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.loginLoadingTitle.text = title
        binding.loginLoadingText.text = baseText

        binding.signInButton.isEnabled = !show
        binding.guestButton.isEnabled = !show

        if (show) {
            startDots(baseText)
            applyBlurIfPossible(true)

            // ✅ some com o conteúdo atrás (foco total no overlay)
            binding.loginScroll.animate()
                .alpha(0.08f)
                .scaleX(0.98f)
                .scaleY(0.98f)
                .setDuration(160)
                .start()

            // card entra “premium”
            binding.loadingCard.scaleX = 0.96f
            binding.loadingCard.scaleY = 0.96f
            binding.loadingCard.alpha = 0f
            binding.loadingCard.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start()

        } else {
            stopDots()
            applyBlurIfPossible(false)

            // ✅ volta o conteúdo
            binding.loginScroll.animate()
                .alpha(if (show) 0.06f else 1f)
                .scaleX(if (show) 0.98f else 1f)
                .scaleY(if (show) 0.98f else 1f)
                .setDuration(160)
                .start()

            binding.legalLinksLayout.isVisible = !show

        }
    }

    private fun startDots(base: String) {
        stopDots()
        dotsAnimator = android.animation.ValueAnimator.ofInt(0, 3).apply {
            duration = 900
            repeatCount = android.animation.ValueAnimator.INFINITE
            addUpdateListener { anim ->
                val n = anim.animatedValue as Int
                binding.loginLoadingText.text = base + ".".repeat(n)
            }
            start()
        }
    }

    private fun stopDots() {
        dotsAnimator?.cancel()
        dotsAnimator = null
    }

    private fun applyBlurIfPossible(enable: Boolean) {
        // ✅ Blur premium só no Android 12+ (API 31)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val radius = if (enable) 18f else 0f
            val effect = android.graphics.RenderEffect.createBlurEffect(
                radius, radius, android.graphics.Shader.TileMode.CLAMP
            )
            binding.loginScroll.setRenderEffect(if (enable) effect else null)
        }
    }

    private fun setStep(step: LoginStep) {
        // Se você ainda não adicionou os steps no XML, isso aqui pode crashar.
        // Então: só tenta atualizar se existirem no binding (compile-time garante, mas depende do XML).
        try {
            val active = R.drawable.bg_step_dot_active
            val inactive = R.drawable.bg_step_dot_inactive

            when (step) {
                LoginStep.GOOGLE -> {
                    binding.stepGoogleDot.setBackgroundResource(active)
                    binding.stepFirebaseDot.setBackgroundResource(inactive)
                    binding.stepReadyDot.setBackgroundResource(inactive)

                    binding.stepGoogleText.setTextColor(0xFFFFFFFF.toInt())
                    binding.stepFirebaseText.setTextColor(0xB3FFFFFF.toInt())
                    binding.stepReadyText.setTextColor(0xB3FFFFFF.toInt())
                }

                LoginStep.FIREBASE -> {
                    binding.stepGoogleDot.setBackgroundResource(active)
                    binding.stepFirebaseDot.setBackgroundResource(active)
                    binding.stepReadyDot.setBackgroundResource(inactive)

                    binding.stepGoogleText.setTextColor(0xFFFFFFFF.toInt())
                    binding.stepFirebaseText.setTextColor(0xFFFFFFFF.toInt())
                    binding.stepReadyText.setTextColor(0xB3FFFFFF.toInt())
                }

                LoginStep.PREPARANDO -> {
                    binding.stepGoogleDot.setBackgroundResource(active)
                    binding.stepFirebaseDot.setBackgroundResource(active)
                    binding.stepReadyDot.setBackgroundResource(active)

                    binding.stepGoogleText.setTextColor(0xFFFFFFFF.toInt())
                    binding.stepFirebaseText.setTextColor(0xFFFFFFFF.toInt())
                    binding.stepReadyText.setTextColor(0xFFFFFFFF.toInt())
                }
            }
        } catch (_: Exception) {
            // ✅ não quebra nada se os steps não existirem ainda
        }
    }
}
