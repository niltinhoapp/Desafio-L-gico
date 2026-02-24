package com.desafiolgico.auth

import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.Choreographer
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.desafiolgico.utils.applySystemBarsPadding
import com.desafiolgico.utils.dp
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    // animação dos pontinhos
    private var dotsAnimator: android.animation.ValueAnimator? = null
    private var timeoutJob: Job? = null

    // Blur safe
    private var blurDisabledForThisSession = false
    private var blurApplied = false
    private var blurRetryCount = 0

    // ✅ Lazy: inicializa Firebase só quando usar auth
    private val auth: FirebaseAuth by lazy(LazyThreadSafetyMode.NONE) {
        FirebaseApp.initializeApp(applicationContext)
        FirebaseAuth.getInstance()
    }

    private val credentialManager: CredentialManager by lazy(LazyThreadSafetyMode.NONE) {
        CredentialManager.create(this)
    }

    companion object {
        private const val TAG = "LoginActivity"
        private const val BLUR_MAX_RETRIES = 2

        // ✅ usado pela MainActivity ao fazer logout
        const val EXTRA_FROM_LOGOUT = "EXTRA_FROM_LOGOUT"

        private const val URL_TERMOS_DE_SERVICO = "https://www.desafiologico.com/terms"
        private const val URL_POLITICA_DE_PRIVACIDADE = "https://www.desafiologico.com/privacy"
    }

    private enum class LoginStep { GOOGLE, FIREBASE, PREPARANDO }

    override fun onCreate(savedInstanceState: Bundle?) {
        val t0 = SystemClock.elapsedRealtime()
        super.onCreate(savedInstanceState)

        applyEdgeToEdge(lightSystemBarIcons = false)

        val tInflate = SystemClock.elapsedRealtime()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "⏱ inflate+setContentView: ${SystemClock.elapsedRealtime() - tInflate}ms")

        // Insets
        binding.loginRootLayout.applySystemBarsPadding(applyTop = true, applyBottom = false)
        binding.legalLinksLayout.applySystemBarsPadding(applyTop = false, applyBottom = true)

        // respiro no scroll
        binding.loginScroll.setPadding(
            binding.loginScroll.paddingLeft,
            binding.loginScroll.paddingTop,
            binding.loginScroll.paddingRight,
            binding.loginScroll.paddingBottom + dp(8)
        )

        // segura lottie até o primeiro frame (cold start mais suave)
        binding.lottieAnimationView.cancelAnimation()
        binding.lottieAnimationView.progress = 0f
        binding.lottieAnimationView.isVisible = false
        Choreographer.getInstance().postFrameCallback {
            Log.d(TAG, "🎬 first frame: ${SystemClock.elapsedRealtime() - t0}ms desde onCreate()")
            binding.lottieAnimationView.isVisible = true
            binding.lottieAnimationView.playAnimation()
        }

        // convidado via flag
        binding.guestButton.isVisible = resources.getBoolean(R.bool.show_guest)

        // Links legais fora do caminho crítico
        binding.root.post { setupLegalLinks() }

        // ✅ Se veio de logout, avisa e limpa a flag do intent
        if (intent.getBooleanExtra(EXTRA_FROM_LOGOUT, false)) {
            Toast.makeText(this, "Você saiu da conta ✅", Toast.LENGTH_SHORT).show()

            // importante: remove a flag pra não “prender” em login em recriações
            intent.removeExtra(EXTRA_FROM_LOGOUT)
        }

        // ✅ trava back enquanto está em loading (evita estado quebrado)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.loginLoadingOverlay.isVisible) return
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })

        // Clique Google
        binding.signInButton.setOnClickListener {
            if (binding.loginLoadingOverlay.isVisible) return@setOnClickListener
            setLoading(true)
            setStep(LoginStep.GOOGLE)
            startDots()
            startTimeoutUX()
            signInWithCredentialManager()
        }

        // Clique Convidado
        binding.guestButton.setOnClickListener {
            if (binding.loginLoadingOverlay.isVisible) return@setOnClickListener
            setLoading(true)
            setStep(LoginStep.PREPARANDO)
            startDots()
            startTimeoutUX()

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    ensureGameData()

                    // ✅ marca guest
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

                stopTimeoutUX()
                stopDots()
                setLoading(false)
                goToNextAfterLogin(clearTask = true)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // ✅ se veio do logout, fica no login (não auto-entra)
        val fromLogout = intent.getBooleanExtra(EXTRA_FROM_LOGOUT, false)
        if (fromLogout) return

        // ✅ se já tem user logado, pula o login
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            goToNextAfterLogin(clearTask = true)
        }
    }

    override fun onStop() {
        super.onStop()
        stopDots()
        stopTimeoutUX()
        setBlurEnabled(false)
        blurApplied = false
        blurRetryCount = 0
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
                    credentialManager.getCredential(this@LoginActivity, request)
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

                    setStep(LoginStep.FIREBASE)
                    startDots()

                    signInWithFirebase(idToken, googleIdTokenCredential)
                } else {
                    showError("Provedor não suportado.")
                }
            }
            else -> showError("Provedor não suportado.")
        }
    }

    // =============================================================================================
    // FIREBASE AUTH
    // =============================================================================================

    private fun signInWithFirebase(idToken: String, googleCred: GoogleIdTokenCredential) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (!task.isSuccessful) {
                Log.e(TAG, "FirebaseAuth falhou", task.exception)
                showError("Falha na autenticação. Tente novamente.")
                return@addOnCompleteListener
            }

            val user = task.result.user

            setStep(LoginStep.PREPARANDO)
            startDots()

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    ensureGameData()
                    GameDataManager.setActiveUserId(this@LoginActivity, user?.uid)
                }

                val username = googleCred.displayName
                    ?: user?.displayName
                    ?: user?.email
                    ?: "Jogador"

                val photoUrl = googleCred.profilePictureUri?.toString()
                    ?: user?.photoUrl?.toString()

                val (_, _, savedAvatar) = GameDataManager.loadUserData(this@LoginActivity)
                val avatarToUse = savedAvatar ?: R.drawable.avatar1

                GameDataManager.saveUserData(
                    context = this@LoginActivity,
                    username = username,
                    photoUrl = photoUrl,
                    avatarId = avatarToUse
                )

                UserManager.salvarDadosUsuario(
                    context = this@LoginActivity,
                    nome = username,
                    email = user?.email,
                    photoUrl = photoUrl,
                    avatarId = avatarToUse
                )

                getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
                    .putBoolean("is_guest_mode", false)
                    .apply()

                CrashlyticsHelper.setUserContext(
                    context = applicationContext,
                    userId = GameDataManager.currentUserId,
                    email = user?.email,
                    username = username
                )

                AdMobInitializer.ensureInitialized(applicationContext)

                Toast.makeText(this@LoginActivity, "Bem-vindo, $username!", Toast.LENGTH_SHORT).show()

                stopTimeoutUX()
                stopDots()
                setLoading(false)
                goToNextAfterLogin(clearTask = true)
            }
        }
    }

    // =============================================================================================
    // NAV
    // =============================================================================================

    private fun goToNextAfterLogin(clearTask: Boolean) {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val completed = prefs.getBoolean("onboarding_completed", false)
        val alwaysShow = prefs.getBoolean("always_show_onboarding", false)

        val next = if (!completed || alwaysShow) {
            Intent(this, com.desafiolgico.information.OnboardingActivity::class.java)
                .putExtra("FROM_SETTINGS", false)
        } else {
            Intent(this, com.desafiolgico.main.BoasVindasActivity::class.java)
        }

        if (clearTask) {
            next.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        startActivity(next)
        finish()
    }

    // =============================================================================================
    // UI: LOADING
    // =============================================================================================

    private fun showError(message: String) {
        stopTimeoutUX()
        stopDots()
        setLoading(false)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setLoading(show: Boolean) {
        binding.loginLoadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.signInButton.isEnabled = !show
        binding.guestButton.isEnabled = !show

        if (show) {
            binding.loginScroll.post { setBlurEnabled(true) }

            binding.loginScroll.animate()
                .alpha(0.35f)
                .scaleX(0.985f)
                .scaleY(0.985f)
                .setDuration(160)
                .start()

            binding.loadingCard.scaleX = 0.96f
            binding.loadingCard.scaleY = 0.96f
            binding.loadingCard.alpha = 0f
            binding.loadingCard.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(180)
                .start()

            binding.legalLinksLayout.isVisible = false
        } else {
            binding.loginScroll.post { setBlurEnabled(false) }

            binding.loginScroll.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(160)
                .start()

            binding.legalLinksLayout.isVisible = true
        }
    }

    private fun setStep(step: LoginStep) {
        when (step) {
            LoginStep.GOOGLE -> {
                binding.loginLoadingTitle.text = "Conectando…"
                binding.loginLoadingText.text = "Só um instante"
            }
            LoginStep.FIREBASE -> {
                binding.loginLoadingTitle.text = "Validando acesso…"
                binding.loginLoadingText.text = "Protegendo sua conta"
            }
            LoginStep.PREPARANDO -> {
                binding.loginLoadingTitle.text = "Finalizando…"
                binding.loginLoadingText.text = "Carregando seu perfil"
            }
        }
    }

    private fun startDots() {
        stopDots()
        val base = binding.loginLoadingText.text.toString().trimEnd('.', ' ')
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

    private fun startTimeoutUX() {
        stopTimeoutUX()
        timeoutJob = lifecycleScope.launch {
            delay(6000)
            if (binding.loginLoadingOverlay.isVisible) {
                binding.loginLoadingText.text = "Isso pode levar alguns segundos"
                startDots()
            }
        }
    }

    private fun stopTimeoutUX() {
        timeoutJob?.cancel()
        timeoutJob = null
    }

    // =============================================================================================
    // GAME DATA
    // =============================================================================================

    private fun ensureGameData() {
        val t = SystemClock.elapsedRealtime()
        GameDataManager.init(applicationContext)
        Log.d(TAG, "⏱ GameDataManager.init(lazy): ${SystemClock.elapsedRealtime() - t}ms")
    }

    // =============================================================================================
    // BLUR — SEM WARNING DE API 31 / SEM CRASH SAMSUNG
    // =============================================================================================

    private fun isSamsung(): Boolean {
        val m = (Build.MANUFACTURER ?: "").lowercase()
        return m.contains("samsung")
    }

    private fun setBlurEnabled(enable: Boolean) {
        if (!::binding.isInitialized) return
        if (isFinishing || isDestroyed) return

        // ✅ Samsung: não tenta blur
        if (isSamsung()) {
            blurDisabledForThisSession = true
            blurApplied = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                runCatching { binding.loginScroll.setRenderEffect(null) }
            }
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        // garante main thread
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            runOnUiThread { setBlurEnabled(enable) }
            return
        }

        val target = binding.loginScroll

        if (blurDisabledForThisSession) {
            runCatching { target.setRenderEffect(null) }
            blurApplied = false
            return
        }

        if (!enable) {
            runCatching { target.setRenderEffect(null) }
            blurApplied = false
            return
        }

        if (!target.isAttachedToWindow || !target.isHardwareAccelerated) {
            if (blurRetryCount < BLUR_MAX_RETRIES) {
                blurRetryCount++
                target.post { setBlurEnabled(true) }
            }
            return
        }
        blurRetryCount = 0

        if (blurApplied) return

        try {
            val radius = 18f
            val effect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
            target.setRenderEffect(effect)
            blurApplied = true
        } catch (e: IllegalArgumentException) {
            blurDisabledForThisSession = true
            blurApplied = false
            runCatching { target.setRenderEffect(null) }
            Log.w(TAG, "Blur desativado (device bug): ${e.message}", e)
        } catch (t: Throwable) {
            blurDisabledForThisSession = true
            blurApplied = false
            runCatching { target.setRenderEffect(null) }
            Log.w(TAG, "Blur falhou e foi desativado: ${t.message}", t)
        }
    }
}
