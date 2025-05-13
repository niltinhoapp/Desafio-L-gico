package com.desafiolgico.settings

import android.content.DialogInterface
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.desafiolgico.databinding.ActivityDeleteAccountBinding
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.applyEdgeToEdge
import com.google.firebase.auth.FirebaseAuth

class DeleteAccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeleteAccountBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()
        binding = ActivityDeleteAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Excluir apenas dados locais
        binding.btnDeleteDataOnly.setOnClickListener {
            confirmDataDeletion()
        }

        // Excluir conta e dados (Firebase + local)
        binding.btnDeleteAccount.setOnClickListener {
            confirmAccountDeletion()
        }
        // Botão Voltar
        binding.btnBack.setOnClickListener {
            finish() // Fecha a tela e volta automaticamente para BoasVindasActivity
        }
    }

    private fun confirmDataDeletion() {
        AlertDialog.Builder(this)
            .setTitle("Excluir dados?")
            .setMessage("Isso apagará seu progresso, pontuação e histórico local, mas sua conta continuará ativa.")
            .setPositiveButton("Sim, excluir dados") { _, _ ->
                GameDataManager.resetAll(this)
                Toast.makeText(
                    this,
                    "Seus dados locais foram excluídos com sucesso.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmAccountDeletion() {
        AlertDialog.Builder(this)
            .setTitle("Excluir conta?")
            .setMessage("Tem certeza que deseja excluir sua conta e todos os seus dados? Esta ação não pode ser desfeita.")
            .setPositiveButton("Sim, excluir tudo") { _: DialogInterface, _: Int ->
                deleteAccountAndData()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteAccountAndData() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Nenhum usuário logado.", Toast.LENGTH_SHORT).show()
            return
        }

        user.delete()
            .addOnSuccessListener {
                GameDataManager.resetAll(this)
                Toast.makeText(this, "Conta e dados excluídos com sucesso.", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao excluir conta: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }
}
