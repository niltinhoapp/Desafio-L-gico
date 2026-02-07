package com.desafiolgico.model

data class Question(
   // val id: String,
    val questionText: String,
    val options: List<String>,
    val correctAnswerIndex: Int
)
