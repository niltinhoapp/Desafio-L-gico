package com.desafiolgico.utils

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object RewardTestRepo {
    private const val TAG = "PREMIO"

    private val db = FirebaseFirestore.getInstance()

    fun grantTestCoins(amount: Long = 10, onDone: (Boolean) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.e(TAG, "NO_AUTH_USER")
            onDone(false)
            return
        }

        val userRef = db.collection("users").document(uid)
        val logRef = userRef.collection("rewards_log").document() // log de eventos

        Log.d(TAG, "PREMIO_START uid=$uid amount=$amount")

        db.runBatch { batch ->
            batch.set(logRef, mapOf(
                "uid" to uid,
                "amount" to amount,
                "type" to "TEST",
                "createdAt" to FieldValue.serverTimestamp()
            ))
            batch.set(userRef, mapOf(
                "coins" to FieldValue.increment(amount),
                "updatedAt" to FieldValue.serverTimestamp()
            ), com.google.firebase.firestore.SetOptions.merge())
        }.addOnSuccessListener {
            Log.d(TAG, "DB_WRITE_OK")
            onDone(true)
        }.addOnFailureListener { e ->
            Log.e(TAG, "DB_WRITE_FAIL", e)
            onDone(false)
        }
    }

    fun readCoins(onResult: (Long) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Log.e(TAG, "NO_AUTH_USER_READ")
            onResult(0)
            return
        }
        db.collection("users").document(uid).get()
            .addOnSuccessListener { snap ->
                val coins = snap.getLong("coins") ?: 0L
                Log.d(TAG, "DB_READ_OK coins=$coins")
                onResult(coins)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "DB_READ_FAIL", e)
                onResult(0)
            }
    }
}
