package com.desafiologico.auth

import android.content.Intent
import android.net.Uri // Necessário para Uri.parse()
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.desafiologico.R
import com.desafiologico.databinding.ActivityLoginBinding // Assumindo que você usa ViewBinding para LoginActivity
import com.desafiologico.main.BoasVindasActivity
// import com.desafiologico.main.MainActivity // Removido se a navegação primária é para BoasVindasActivity
import com.desafiologico.utils.GameDataManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton // Já deve estar importado
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    // private lateinit var sharedPreferences: SharedPreferences // Removido se não usado diretamente para dados do utilizador
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var binding: ActivityLoginBinding // Adicionado para ViewBinding

    companion object {
        private const val TAG = "LoginActivity"
        // URLs atualizadas com o endereço fornecido
        private const val URL_TERMOS_DE_SERVICO = "https://vitrineletronicos.com/" // URL ATUALIZADA
        private const val URL_POLITICA_DE_PRIVACIDADE = "https://vitrineletronicos.com/" // URL ATUALIZADA (mesma para este exemplo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater) // Infla o layout usando ViewBinding
        setContentView(binding.root) // Define a view raiz do binding

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // O acesso aos botões e textviews agora é feito através do 'binding'
        binding.signInButton.setOnClickListener {
            signInWithGoogle()
        }

        binding.createAccountTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Configurar links legais
        setupLegalLinks()
    }

    private fun setupLegalLinks() {
        // Tornar o texto dos links sublinhado para parecerem mais com links
        val termsText = SpannableString(binding.termsOfServiceLink.text)
        termsText.setSpan(UnderlineSpan(), 0, termsText.length, 0)
        binding.termsOfServiceLink.text = termsText

        val privacyText = SpannableString(binding.privacyPolicyLink.text)
        privacyText.setSpan(UnderlineSpan(), 0, privacyText.length, 0)
        binding.privacyPolicyLink.text = privacyText

        binding.termsOfServiceLink.setOnClickListener {
            openUrl(URL_TERMOS_DE_SERVICO)
        }

        binding.privacyPolicyLink.setOnClickListener {
            openUrl(URL_POLITICA_DE_PRIVACIDADE)
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao abrir URL: $url", e)
            Toast.makeText(this, "Não foi possível abrir o link.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        resultLauncher.launch(signInIntent)
        Log.d(TAG, "Iniciando o login com Google...")
    }

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                handleSignInResult(task)
            } else {
                Log.w(TAG, "Fluxo de login com Google cancelado ou falhou no ActivityResult. ResultCode: ${result.resultCode}")
                showError("Login com Google cancelado.")
            }
        }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                Log.d(TAG, "Google Sign-In Account obtido com sucesso. Autenticando com Firebase...")
                firebaseAuthWithGoogle(account.idToken!!, account)
            } else {
                Log.w(TAG, "Google Sign-In Account é nulo, mesmo sem exceção.")
                showError("Erro ao obter dados da conta Google.")
            }
        } catch (e: ApiException) {
            Log.w(TAG, "Erro no Google Sign-In: ${e.statusCode}", e)
            showError("Erro ao fazer login com Google: ${e.localizedMessage}")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String, googleAccount: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Login com Firebase bem-sucedido.")
                val username = googleAccount.displayName ?: googleAccount.email ?: "Jogador"
                val photoUrl = googleAccount.photoUrl?.toString()

                GameDataManager.saveUserData(this, username, photoUrl, null)
                Log.d(TAG, "Dados do utilizador ($username) salvos no GameDataManager.")

                startActivity(Intent(this, BoasVindasActivity::class.java))
                finish()

            } else {
                Log.w(TAG, "Erro no login com Firebase.", task.exception)
                showError("Erro na autenticação com Firebase: ${task.exception?.localizedMessage}")
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
