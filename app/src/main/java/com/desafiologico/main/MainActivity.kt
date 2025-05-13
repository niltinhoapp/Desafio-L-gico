package com.desafiologico.main

// Removida a importação de BoasVindasActivity.PREFS_NAME, pois a lógica associada foi removida
import android.Manifest
import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.desafiologico.R
import com.desafiologico.utils.GameDataManager
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var levelManager: LevelManager
    private lateinit var startForResult: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar views
        lottieAnimationView = findViewById(R.id.lottieAnimationView)
        val mainContent = findViewById<LinearLayout>(R.id.mainContent)

        // Inicializar gerenciador de níveis
        levelManager = LevelManager(this)

        // --- Início da Lógica Corrigida/Melhorada ---

        // TODO: Decide se o reset de dado's é necessarily. Para biodegradability normal,
        // esta linha deve ser removida ou se tornar condicional (ex: botão de reset).
        // GameDataManager.resetarTodosOsDados(this) // Descomente para testar, limpando todos os dados.

        // 1. Sincronizar todos os níveis (padrão e baseados em pontuação) com o
        //    conjunto "unlocked_levels" no SharedPreferences.
        //    Isso garante que LevelManager obtenha o estado correto de GameDataManager.isLevelUnlocked().
        //    GameDataManager.verificarDesbloqueioNivel() inclui GameDataManager.Levels.INICIANTE por padrão.
        val niveisQueDeveriamEstarDesbloqueados = GameDataManager.verificarDesbloqueioNivel(this)
        niveisQueDeveriamEstarDesbloqueados.forEach { nivel ->
            GameDataManager.unlockLevel(this, nivel) // Garante que esteja no conjunto "unlocked_levels"
        }
        Log.d("MainActivity", "Sincronização de níveis desbloqueados com SharedPreferences concluída. Níveis agora marcados como desbloqueados: $niveisQueDeveriamEstarDesbloqueados")

        // 2. Opcionalmente, chame checkLevelUnlockWithNotification para exibir diálogos para níveis recém-desbloqueados.
        // Esta função verifica suas próprias flags internas para não notificar repetidamente.
        GameDataManager.checkLevelUnlockWithNotification(this)

        // Log para verificar o status do nível Intermediário após todas as verificações e sincronizações.
        Log.d("MainActivity", "Nível Intermediário desbloqueado? ${GameDataManager.isLevelUnlocked(this, GameDataManager.Levels.INTERMEDIARIO)}")

        // BUG REMOVIDO: Progressão automática de nível sem jogar.
        // O progresso do nível (faseAtual) deve ser salvo apenas na TestActivity após o nível ser concluído.
        // val faseAtual = GameDataManager.getNivel(this)?.toIntOrNull() ?: 1
        // val pontos = GameDataManager.getPontuacao(this)
        // val erros = GameDataManager.getErros(this)
        // val novaFase = faseAtual + 1
        // GameDataManager.saveProgresso(this, novaFase.toString(), pontos, erros)

        // --- Fim da Lógica Corrigida/Melhorada ---

        // Permissão para notificações (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }

        // Configurar animação de entrada
        setupAnimation(mainContent)

        // Música de fundo
        initBackgroundMusic()

        // Registrar launcher para retorno da TestActivity
        setupActivityResultLauncher()

        // Botões
        val beginnerButton = findViewById<MaterialButton>(R.id.beginnerButton)
        val intermediateButton = findViewById<MaterialButton>(R.id.intermediateButton)
        val advancedButton = findViewById<MaterialButton>(R.id.advancedButton)
        val exitButton = findViewById<MaterialButton>(R.id.exitButton)

        // Configuração dos botões via LevelManager
        levelManager.setupButtons(
            beginnerButton,
            intermediateButton,
            advancedButton,
            exitButton,
            ::handleButtonClick,
            ::showLevelLockedMessage, // Usa a versão do Toast: showLevelLockedMessage(level: String)
            ::showExitConfirmationDialog
        )

        // Atualiza o estado visual dos botões DEPOIS que toda a lógica de desbloqueio foi processada.
        levelManager.updateButtonStates(intermediateButton, advancedButton)
    }

    // REMOVIDA: fun initializeLevelsIfNeeded(context: Context) {...}
    // A lógica de desbloqueio do nível "Iniciante" agora é tratada diretamente acima.

    // Iniciar retorno da atividade de teste
    private fun setupActivityResultLauncher() {
        startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val score = data?.getIntExtra("score", 0) ?: 0
                val level = data?.getStringExtra("level") ?: ""
                Log.d("ActivityResult", "Retorno da TestActivity - Level: $level, Score: $score")

                // Após um nível ser jogado, re-sincronize os desbloqueios baseados em pontuação
                // e atualize os botões.
                val niveisQueDeveriamEstarDesbloqueados = GameDataManager.verificarDesbloqueioNivel(this)
                niveisQueDeveriamEstarDesbloqueados.forEach { nivelAtualizado ->
                    GameDataManager.unlockLevel(this, nivelAtualizado)
                }
                GameDataManager.checkLevelUnlockWithNotification(this) // Mostrar diálogos se algo novo desbloqueou
                levelManager.updateButtonStates(findViewById(R.id.intermediateButton), findViewById(R.id.advancedButton))
            }
        }
    }

    // Configurar animação de introdução
    private fun setupAnimation(mainContent: View) {
        lottieAnimationView.setAnimation(R.raw.airplane_explosion1)
        lottieAnimationView.visibility = View.VISIBLE
        mainContent.visibility = View.GONE
        lottieAnimationView.repeatCount = 1 // Lottie >= 3.4.0 usa LottieDrawable.RepeatMode LottieDrawable.INFINITE ou LottieDrawable.RESTART
        // e setRepeatCount. Para não repetir, não defina repeatCount ou defina como 0 se a API permitir.
        // A maioria das animações "once" já são configuradas para não repetir no próprio JSON.
        // Se a animação estiver repetindo, garanta que no JSON `loop: false` ou `repeatCount:0`
        lottieAnimationView.playAnimation()

        lottieAnimationView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                Log.d("Animation", "Iniciando animação")
            }
            override fun onAnimationEnd(animation: Animator) {
                Log.d("Animation", "Animação finalizada")
                lottieAnimationView.visibility = View.GONE // Esconde a animação Lottie

                // Navegar para TesteActivity
                // Se esta função setupAnimation está DENTRO da sua MainActivity:
                // use this@MainActivity como contexto.
                // Substitua 'NomeDaSuaActivityAtual' pelo nome real da Activity
                // onde esta função setupAnimation está definida.
                // Se estiver fora, use context como contexto.
                val intent = Intent(this@MainActivity, TestActivity::class.java)
                startActivity(intent)

                // Opcional: Se você não quiser que o usuário possa voltar para a Activity
                // anterior (onde a animação tocou) pressionando o botão "voltar",
                // finalize a Activity atual.
                // finish()
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    // Solicita permissão de notificação (Android 13+)
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    // Verifica se a permissão foi concedida
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notificações ativadas!", Toast.LENGTH_SHORT).show()
                Log.d("MainActivity", "Permissão de notificação concedida")
            } else {
                Toast.makeText(this, "Permissão de notificação negada.", Toast.LENGTH_SHORT).show()
                Log.d("MainActivity", "Permissão de notificação negada")
            }
        }
    }

    // Música de fundo
    private fun initBackgroundMusic() {
        mediaPlayer = MediaPlayer.create(this, R.raw.background_music).apply {
            isLooping = true
            // Verifique se a música deve começar imediatamente ou após a animação, por exemplo.
            start()
        }
    }

    // Callback para LevelManager quando um nível bloqueado é clicado.
    fun showLevelLockedMessage(level: String) {
        Toast.makeText(this, "Nível \"$level\" bloqueado!", Toast.LENGTH_SHORT).show()
    }

    // Clique nos botões de nível ou sair
    private fun handleButtonClick(button: MaterialButton, level: String) {
        playClickSound()
        animateButton(button)
        // A lógica de resetar cores e mudar a cor do botão clicado pode ser melhorada
        // para evitar hardcoding de IDs e ser mais dinâmica se houver muitos botões.
        // Para 3-4 botões, a abordagem atual é aceitável.
        resetButtonColors()
        changeButtonColor(button)

        if (level != "exit") { // Supondo que "exit" não é um nome de nível válido
            navigateToTestActivity(level)
        }
        // O caso de "exit" é tratado pelo onExitConfirm no setupButtons,
        // que chama showExitConfirmationDialog.
    }

    private fun resetButtonColors() {
        val buttonIds = listOf(
            R.id.beginnerButton,
            R.id.intermediateButton,
            R.id.advancedButton,
            R.id.exitButton // Inclui o botão de sair se ele também mudar de cor
        )
        for (id in buttonIds) {
            findViewById<MaterialButton>(id)?.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.button_default)
        }
    }

    private fun changeButtonColor(button: MaterialButton) {
        button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_selected)
    }

    private fun playClickSound() {
        MediaPlayer.create(this, R.raw.click_sound).apply {
            setOnCompletionListener { mp -> mp.release() } // Boa prática liberar o MediaPlayer
            start()
        }
    }

    private fun animateButton(button: MaterialButton) {
        val scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 1.1f, 1f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 300
            start()
        }
    }

    private fun navigateToTestActivity(level: String) {
        val intent = Intent(this, TestActivity::class.java).apply {
            putExtra("level", level)
        }
        startForResult.launch(intent) // Usa o launcher registrado
    }

    // REMOVIDA: private fun showLevelLockedMessage() { ... }
    // A versão com parâmetro (String) é usada pelo LevelManager.

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Sair")
            .setMessage("Você deseja sair do jogo?")
            .setPositiveButton("Sim") { _, _ -> finish() }
            .setNegativeButton("Não", null)
            .show()
    }

    // Controle de ciclo de vida para música e animação
    override fun onPause() {
        super.onPause()
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) mediaPlayer.pause()
        if (::lottieAnimationView.isInitialized && lottieAnimationView.isAnimating) lottieAnimationView.pauseAnimation()
    }

    override fun onResume() {
        super.onResume()
        // Re-sincronizar e atualizar estado dos botões ao retornar para a Activity,
        // pois o jogador pode ter desbloqueado níveis.
        val niveisQueDeveriamEstarDesbloqueados = GameDataManager.verificarDesbloqueioNivel(this)
        niveisQueDeveriamEstarDesbloqueados.forEach { nivelAtualizado ->
            GameDataManager.unlockLevel(this, nivelAtualizado)
        }
        // Não precisa chamar checkLevelUnlockWithNotification aqui novamente, a menos que queira o popup toda vez que resumir.
        // A atualização de estado dos botões é importante.
        if (::levelManager.isInitialized) { // Garante que levelManager foi inicializado
            levelManager.updateButtonStates(findViewById(R.id.intermediateButton), findViewById(R.id.advancedButton))
        }


        if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) mediaPlayer.start()
        if (::lottieAnimationView.isInitialized && !lottieAnimationView.isAnimating) {
            // Decide se a animação de introdução deve recomeçar ou não ao resumir.
            // Geralmente, animações de introdução não recomeçam.
            // Se for uma animação pausável no contexto atual, então .resumeAnimation() é apropriado.
            // lottieAnimationView.resumeAnimation() // Ex: se fosse uma animação de fundo contínua
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop() // Pare antes de liberar
            mediaPlayer.release()
        }
        if (::lottieAnimationView.isInitialized) lottieAnimationView.cancelAnimation()
    }
}