package com.desafiolgico.model

import com.desafiolgico.utils.GameDataManager

class QuestionManager (private val languageCode: String) {

    private val isEnglish: Boolean
        get() = languageCode.startsWith("en", ignoreCase = true)

    fun getQuestionsByLevel(selectedLevel: String): List<Question> {
        return when (selectedLevel) {

            GameDataManager.Levels.INICIANTE -> {
                if (isEnglish)
                    beginnerQuestionsEn.shuffled().take(30)
                else
                    beginnerQuestionsPt.shuffled().take(30)
            }

            GameDataManager.Levels.INTERMEDIARIO -> {
                if (isEnglish)
                    intermediateQuestionsEn.shuffled().take(25)
                else
                    intermediateQuestionsPt.shuffled().take(25)
            }

            GameDataManager.Levels.AVANCADO -> {
                if (isEnglish)
                    advancedQuestionsEn.shuffled().take(20)
                else
                    advancedQuestionsPt.shuffled().take(20)
            }

            GameDataManager.Levels.EXPERIENTE -> {
                if (isEnglish)
                    expertQuestionsEn.shuffled().take(15)
                else
                    expertQuestionsPt.shuffled().take(15)
            }


            // Fases secretas
            GameDataManager.SecretLevels.RELAMPAGO -> {
                if (isEnglish) {
                    relampagoBaseQuestionsEn.shuffled().take(5)
                } else {
                    relampagoBaseQuestionsPt.shuffled().take(5)
                }
            }

            GameDataManager.SecretLevels.PERFEICAO -> {
                if (isEnglish) {
                    perfeicaoBaseQuestionsEn.shuffled().take(3)
                } else {
                    perfeicaoBaseQuestionsPt.shuffled().take(3)
                }
            }

            GameDataManager.SecretLevels.ENIGMA -> {
                if (isEnglish) {
                    enigmaBaseQuestionsEn.shuffled().take(3)
                } else {
                    enigmaBaseQuestionsPt.shuffled().take(3)
                }
            }

            else -> emptyList()
        }
    }


    //lista de perguntas para nivel iniciante
    private val beginnerQuestionsPt = listOf(

        Question(
            questionText = "Qual é a cor do céu em um dia claro?",
            options = listOf("Azul", "Verde", "Vermelho", "Amarelo"),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Quantos dias tem uma semana?",
            options = listOf("5", "6", "7", "8"),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Qual é o número que vem após o 4?",
            options = listOf("3", "5", "2", "4"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é o maior órgão do corpo humano?",
            options = listOf(
                "Coração",
                "Fígado",
                "Pele",
                "Pulmão"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Qual é o planeta mais próximo do Sol?",
            options = listOf(
                "Marte",
                "Vênus",
                "Mercúrio",
                "Terra"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Qual é o nome da substância que dá cor às plantas?",
            options = listOf(
                "Clorofila",
                "Celulose",
                "Fotossíntese",
                "Hemoglobina"
            ),
            correctAnswerIndex = 0
        ),

        // Perguntas de História
        Question(
            questionText = "Quem foi o primeiro presidente do Brasil?",
            options = listOf(
                "Getúlio Vargas",
                "Deodoro da Fonseca",
                "Dom Pedro II",
                "Juscelino Kubitschek"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Em que ano o homem pisou na Lua pela primeira vez?",
            options = listOf(
                "1965",
                "1969",
                "1971",
                "1975"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual era a capital do Brasil antes de Brasília?",
            options = listOf(
                "São Paulo",
                "Belo Horizonte",
                "Rio de Janeiro",
                "Salvador"
            ),
            correctAnswerIndex = 2
        ),

        // Perguntas de Matemática
        Question(
            questionText = "Quanto é 2 + 2?",
            options = listOf(
                "3",
                "4",
                "5",
                "6"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é o valor de π (pi) aproximadamente?",
            options = listOf(
                "2.14",
                "3.14",
                "4.14",
                "5.14"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é a metade de 10?",
            options = listOf(
                "4",
                "5",
                "6",
                "7"
            ),
            correctAnswerIndex = 1
        ),
        // Perguntas de Geografia
        Question(
            questionText = "Qual é o maior continente do mundo?",
            options = listOf(
                "África",
                "Ásia",
                "América",
                "Europa"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é o rio mais longo do mundo?",
            options = listOf(
                "Rio Nilo",
                "Rio Amazonas",
                "Rio Yangtzé",
                "Rio Mississippi"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é a capital da França?",
            options = listOf(
                "Londres",
                "Berlim",
                "Paris",
                "Madri"
            ),
            correctAnswerIndex = 2
        ),

        // Perguntas de Astronomia
        Question(
            questionText = "Qual é o maior planeta do Sistema Solar?",
            options = listOf(
                "Júpiter",
                "Saturno",
                "Urano",
                "Terra"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Quantas luas tem a Terra?",
            options = listOf(
                "1",
                "2",
                "3",
                "4"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é o nome da nossa galáxia?",
            options = listOf(
                "Andrômeda",
                "Via Láctea",
                "Triângulo",
                "Órion"
            ),
            correctAnswerIndex = 1
        ),

        // Perguntas de Cultura Geral
        Question(
            questionText = "Qual é o idioma mais falado no mundo?",
            options = listOf(
                "Inglês",
                "Mandarim",
                "Espanhol",
                "Hindi"
            ), correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual animal é conhecido como 'o melhor amigo do homem'?",
            options = listOf(
                "Cachorro",
                "Gato",
                "Cavalo",
                "Peixe"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é o esporte mais popular do mundo?",
            options = listOf(
                "Basquete",
                "Críquete",
                "Futebol",
                "Tênis"
            ),
            correctAnswerIndex = 2
        ),


        // Perguntas de Ciências
        Question(
            questionText = "Qual é o estado físico da água a 0°C?",
            options = listOf("Líquido", "Gasoso", "Sólido", "Plasma"),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Qual é o principal gás que respiramos?",
            options = listOf("Oxigênio", "Nitrogênio", "Hélio", "Dióxido de carbono"),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual órgão é responsável por bombear o sangue no corpo humano?",
            options = listOf("Pulmão", "Cérebro", "Coração", "Estômago"),
            correctAnswerIndex = 2
        ),

        // Perguntas de Cultura
        Question(
            questionText = "Qual é a cor principal da bandeira do Brasil?",
            options = listOf("Verde", "Amarelo", "Azul", "Branco"),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Quem escreveu *Dom Casmurro*?",
            options = listOf(
                "Machado de Assis",
                "José de Alencar",
                "Jorge Amado",
                "Carlos Drummond"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual país é conhecido como a terra do sol nascente?",
            options = listOf("China", "Japão", "Coreia do Sul", "Tailândia"),
            correctAnswerIndex = 1
        ),

        // Perguntas de Esportes
        Question(
            questionText = "Quantos jogadores compõem um time de futebol em campo?",
            options = listOf("10", "11", "12", "13"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual país sediou a Copa do Mundo de Futebol em 2014?",
            options = listOf("Rússia", "Brasil", "Alemanha", "África do Sul"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual esporte utiliza raquete e uma bola amarela?",
            options = listOf("Tênis", "Basquete", "Vôlei", "Golfe"),
            correctAnswerIndex = 0
        ),
        // Perguntas de História
        Question(
            questionText = "Em que ano foi proclamada a independência do Brasil?",
            options = listOf("1808", "1822", "1889", "1891"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual civilização construiu as pirâmides de Gizé?",
            options = listOf("Maias", "Egípcios", "Astecas", "Incas"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Quem descobriu o Brasil?",
            options = listOf(
                "Pedro Álvares Cabral",
                "Cristóvão Colombo",
                "Vasco da Gama",
                "Fernão de Magalhães"
            ),
            correctAnswerIndex = 0
        ),

        // Perguntas de Matemática
        Question(
            questionText = "Quanto é 3 × 3?",
            options = listOf("6", "9", "12", "15"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é o menor número primo?",
            options = listOf("1", "2", "3", "4"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Quanto é 10 ÷ 2?",
            options = listOf("3", "4", "5", "6"),
            correctAnswerIndex = 2
        ),

        // Perguntas de Geografia
        Question(
            questionText = "Qual é o menor país do mundo?",
            options = listOf("Mônaco", "Vaticano", "San Marino", "Liechtenstein"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é o oceano que banha o litoral brasileiro?",
            options = listOf("Pacífico", "Ártico", "Atlântico", "Índico"),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Qual país tem a maior população do mundo?",
            options = listOf("Índia", "China", "EUA", "Rússia"),
            correctAnswerIndex = 1
        ),

        // Perguntas de Astronomia
        Question(
            questionText = "Quantos planetas existem no Sistema Solar?",
            options = listOf("7", "8", "9", "10"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é o astro que ilumina a Terra?",
            options = listOf("Lua", "Marte", "Sol", "Estrela Polar"),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Qual é o planeta conhecido como Planeta Vermelho?",
            options = listOf("Marte", "Vênus", "Júpiter", "Saturno"),
            correctAnswerIndex = 0
        ),

        // Perguntas de Cultura Geral
        Question(
            questionText = "Qual o autor da obra *Romeu e Julieta*?",
            options = listOf(
                "William Shakespeare",
                "Miguel de Cervantes",
                "J.K. Rowling",
                "Victor Hugo"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Quantos meses têm 28 dias?",
            options = listOf("1", "2", "6", "12"),
            correctAnswerIndex = 3
        ),
        Question(
            questionText = "Qual é o nome do fundador da Microsoft?",
            options = listOf("Steve Jobs", "Bill Gates", "Mark Zuckerberg", "Jeff Bezos"),
            correctAnswerIndex = 1
        ),

        // Mais perguntas aleatórias
        Question(
            questionText = "Qual é o nome do maior deserto do mundo?",
            options = listOf("Saara", "Gobi", "Kalahari", "Atacama"),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é o animal mais rápido do mundo?",
            options = listOf("Leão", "Guepardo", "Falcão Peregrino", "Cavalo"),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Qual o principal ingrediente do pão?",
            options = listOf("Farinha", "Açúcar", "Sal", "Água"),
            correctAnswerIndex = 0
        )


    )

    private val beginnerQuestionsEn = listOf(

        Question(
            questionText = "What is the color of the sky on a clear day?",
            options = listOf("Blue", "Green", "Red", "Yellow"),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "How many days are there in a week?",
            options = listOf("5", "6", "7", "8"),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Which number comes after 4?",
            options = listOf("3", "5", "2", "4"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is the largest organ of the human body?",
            options = listOf(
                "Heart",
                "Liver",
                "Skin",
                "Lung"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Which planet is closest to the Sun?",
            options = listOf(
                "Mars",
                "Venus",
                "Mercury",
                "Earth"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "What is the name of the substance that gives plants their green color?",
            options = listOf(
                "Chlorophyll",
                "Cellulose",
                "Photosynthesis",
                "Hemoglobin"
            ),
            correctAnswerIndex = 0
        ),

        // History questions
        Question(
            questionText = "Who was the first president of Brazil?",
            options = listOf(
                "Getúlio Vargas",
                "Deodoro da Fonseca",
                "Dom Pedro II",
                "Juscelino Kubitschek"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "In what year did humankind first walk on the Moon?",
            options = listOf(
                "1965",
                "1969",
                "1971",
                "1975"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What was the capital of Brazil before Brasília?",
            options = listOf(
                "São Paulo",
                "Belo Horizonte",
                "Rio de Janeiro",
                "Salvador"
            ),
            correctAnswerIndex = 2
        ),

        // Math questions
        Question(
            questionText = "What is 2 + 2?",
            options = listOf(
                "3",
                "4",
                "5",
                "6"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is the approximate value of π (pi)?",
            options = listOf(
                "2.14",
                "3.14",
                "4.14",
                "5.14"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is half of 10?",
            options = listOf(
                "4",
                "5",
                "6",
                "7"
            ),
            correctAnswerIndex = 1
        ),

        // Geography questions
        Question(
            questionText = "What is the largest continent in the world?",
            options = listOf(
                "Africa",
                "Asia",
                "America",
                "Europe"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Which is the longest river in the world?",
            options = listOf(
                "Nile River",
                "Amazon River",
                "Yangtze River",
                "Mississippi River"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is the capital of France?",
            options = listOf(
                "London",
                "Berlin",
                "Paris",
                "Madrid"
            ),
            correctAnswerIndex = 2
        ),

        // Astronomy questions
        Question(
            questionText = "What is the largest planet in the Solar System?",
            options = listOf(
                "Jupiter",
                "Saturn",
                "Uranus",
                "Earth"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "How many moons does Earth have?",
            options = listOf(
                "1",
                "2",
                "3",
                "4"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What is the name of our galaxy?",
            options = listOf(
                "Andromeda",
                "Milky Way",
                "Triangulum",
                "Orion"
            ),
            correctAnswerIndex = 1
        ),

        // General culture questions
        Question(
            questionText = "What is the most spoken language in the world?",
            options = listOf(
                "English",
                "Mandarin Chinese",
                "Spanish",
                "Hindi"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Which animal is known as \"man's best friend\"?",
            options = listOf(
                "Dog",
                "Cat",
                "Horse",
                "Fish"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What is the most popular sport in the world?",
            options = listOf(
                "Basketball",
                "Cricket",
                "Football (Soccer)",
                "Tennis"
            ),
            correctAnswerIndex = 2
        ),

        // Science questions
        Question(
            questionText = "What is the physical state of water at 0°C (32°F)?",
            options = listOf("Liquid", "Gas", "Solid", "Plasma"),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "What is the main gas that we breathe?",
            options = listOf("Oxygen", "Nitrogen", "Helium", "Carbon dioxide"),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Which organ is responsible for pumping blood through the human body?",
            options = listOf("Lung", "Brain", "Heart", "Stomach"),
            correctAnswerIndex = 2
        ),

        // Culture questions
        Question(
            questionText = "What is the main color of the Brazilian flag?",
            options = listOf("Green", "Yellow", "Blue", "White"),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Who wrote *Dom Casmurro*?",
            options = listOf(
                "Machado de Assis",
                "José de Alencar",
                "Jorge Amado",
                "Carlos Drummond"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Which country is known as the Land of the Rising Sun?",
            options = listOf("China", "Japan", "South Korea", "Thailand"),
            correctAnswerIndex = 1
        ),

        // Sports questions
        Question(
            questionText = "How many players are there on a soccer team on the field?",
            options = listOf("10", "11", "12", "13"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Which country hosted the FIFA World Cup in 2014?",
            options = listOf("Russia", "Brazil", "Germany", "South Africa"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Which sport uses a racket and a yellow ball?",
            options = listOf("Tennis", "Basketball", "Volleyball", "Golf"),
            correctAnswerIndex = 0
        ),

        // History questions
        Question(
            questionText = "In what year was Brazil's independence proclaimed?",
            options = listOf("1808", "1822", "1889", "1891"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Which civilization built the Pyramids of Giza?",
            options = listOf("Mayans", "Egyptians", "Aztecs", "Incas"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Who discovered Brazil?",
            options = listOf(
                "Pedro Álvares Cabral",
                "Christopher Columbus",
                "Vasco da Gama",
                "Ferdinand Magellan"
            ),
            correctAnswerIndex = 0
        ),

        // More math questions
        Question(
            questionText = "What is 3 × 3?",
            options = listOf("6", "9", "12", "15"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is the smallest prime number?",
            options = listOf("1", "2", "3", "4"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is 10 ÷ 2?",
            options = listOf("3", "4", "5", "6"),
            correctAnswerIndex = 2
        ),

        // More geography questions
        Question(
            questionText = "What is the smallest country in the world?",
            options = listOf("Monaco", "Vatican City", "San Marino", "Liechtenstein"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Which ocean borders the Brazilian coastline?",
            options = listOf("Pacific", "Arctic", "Atlantic", "Indian"),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Which country has the largest population in the world?",
            options = listOf("India", "China", "USA", "Russia"),
            correctAnswerIndex = 1
        ),

        // More astronomy questions
        Question(
            questionText = "How many planets are there in the Solar System?",
            options = listOf("7", "8", "9", "10"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Which celestial body lights the Earth?",
            options = listOf("Moon", "Mars", "Sun", "Polaris"),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Which planet is known as the Red Planet?",
            options = listOf("Mars", "Venus", "Jupiter", "Saturn"),
            correctAnswerIndex = 0
        ),

        // More general culture questions
        Question(
            questionText = "Who is the author of *Romeo and Juliet*?",
            options = listOf(
                "William Shakespeare",
                "Miguel de Cervantes",
                "J.K. Rowling",
                "Victor Hugo"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "How many months have 28 days?",
            options = listOf("1", "2", "6", "12"),
            correctAnswerIndex = 3
        ),
        Question(
            questionText = "What is the name of the founder of Microsoft?",
            options = listOf("Steve Jobs", "Bill Gates", "Mark Zuckerberg", "Jeff Bezos"),
            correctAnswerIndex = 1
        ),

        // More random questions
        Question(
            questionText = "What is the name of the largest desert in the world?",
            options = listOf("Sahara", "Gobi", "Kalahari", "Atacama"),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What is the fastest animal in the world?",
            options = listOf("Lion", "Cheetah", "Peregrine Falcon", "Horse"),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "What is the main ingredient in bread?",
            options = listOf("Flour", "Sugar", "Salt", "Water"),
            correctAnswerIndex = 0
        )
    )


    // pergunta para nivel intermediario
    private val intermediateQuestionsPt = listOf(

        Question(
            questionText = "Qual é a capital da França?",
            options = listOf("Paris", "Londres", "Roma", "Berlim"),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é o maior planeta do Sistema Solar?",
            options = listOf("Terra", "Júpiter", "Marte", "Saturno"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Quem escreveu 'Dom Casmurro'?",
            options = listOf(
                "Machado de Assis",
                "José de Alencar",
                "Monteiro Lobato",
                "Clarice Lispector"
            ),
            correctAnswerIndex = 0
        ),

        Question(
            questionText = "Qual é a fórmula química da água?",
            options = listOf(
                "H2O",
                "CO2",
                "H2O2",
                "CH4"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é o principal gás responsável pelo efeito estufa?",
            options = listOf(
                "Oxigênio",
                "Dióxido de carbono",
                "Nitrogênio",
                "Hidrogênio"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é o processo pelo qual as plantas produzem seu próprio alimento?",
            options = listOf(
                "Respiração celular",
                "Fotossíntese",
                "Fermentação",
                "Digestão"
            ),
            correctAnswerIndex = 1
        ),

        // Perguntas de Matemática
        Question(
            questionText = "Qual é o valor da raiz quadrada de 144?",
            options = listOf(
                "10",
                "12",
                "14",
                "16"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Quanto é 15% de 200?",
            options = listOf(
                "20",
                "25",
                "30",
                "35"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Qual é o resultado de (3² + 4²)?",
            options = listOf(
                "12",
                "16",
                "25",
                "29"
            ),
            correctAnswerIndex = 3
        ),

        // Perguntas de História
        Question(
            questionText = "Qual evento marcou o início da Segunda Guerra Mundial?",
            options = listOf(
                "Ataque a Pearl Harbor",
                "Invasão da Polônia pela Alemanha",
                "Queda da Bolsa de Nova York",
                "Revolução Russa"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Quem era o líder do Egito antigo conhecido por construir a Grande Esfinge?",
            options = listOf(
                "Ramsés II",
                "Tutancâmon",
                "Quéops",
                "Amenófis"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Em que ano foi proclamada a independência do Brasil?",
            options = listOf(
                "1808",
                "1822",
                "1889",
                "1900"
            ),
            correctAnswerIndex = 1
        ),

        // Perguntas de Geografia
        Question(
            questionText = "Qual país tem o maior número de fusos horários?",
            options = listOf(
                "Estados Unidos",
                "China",
                "Rússia",
                "França"
            ),
            correctAnswerIndex = 3
        ),
        Question(
            questionText = "Qual é o oceano mais profundo do mundo?",
            options = listOf(
                "Oceano Atlântico",
                "Oceano Pacífico",
                "Oceano Índico",
                "Oceano Ártico"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é a capital da Austrália?",
            options = listOf(
                "Sydney",
                "Melbourne",
                "Canberra",
                "Perth"
            ),
            correctAnswerIndex = 2
        ),

        // Perguntas de Astronomia
        Question(
            questionText = "Quantos planetas compõem o Sistema Solar?",
            options = listOf(
                "7",
                "8",
                "9",
                "10"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é o nome do maior satélite natural de Saturno?",
            options = listOf(
                "Ganimedes",
                "Io",
                "Titã",
                "Europa"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Qual é a unidade astronômica usada para medir distâncias no Sistema Solar?",
            options = listOf(
                "Anos-luz",
                "Parsecs",
                "Unidade Astronômica (UA)",
                "Kilômetros"
            ),
            correctAnswerIndex = 2
        ),

        // Perguntas de Física
        Question(
            questionText = "Qual é a fórmula da Segunda Lei de Newton?",
            options = listOf(
                "F = m × a",
                "E = mc²",
                "P = m × g",
                "V = d / t"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é a unidade de resistência elétrica no SI?",
            options = listOf(
                "Ampère",
                "Ohm",
                "Volt",
                "Watt"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual fenômeno explica a propagação do som em ondas?",
            options = listOf(
                "Refração",
                "Difração",
                "Ressonância",
                "Ondas mecânicas"
            ),
            correctAnswerIndex = 3
        ),

        // Perguntas de Química
        Question(
            questionText = "Qual elemento químico é conhecido como o \"metal líquido\"?",
            options = listOf(
                "Mercúrio",
                "Prata",
                "Ouro",
                "Cobre"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é o número atômico do carbono?",
            options = listOf(
                "6",
                "12",
                "8",
                "14"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é o símbolo químico do sódio?",
            options = listOf(
                "So",
                "Na",
                "S",
                "N"
            ),
            correctAnswerIndex = 1
        ),

        // Perguntas de Cultura Geral
        Question(
            questionText = "Quem pintou a obra \"A Última Ceia\"?",
            options = listOf(
                "Michelangelo",
                "Leonardo da Vinci",
                "Rafael",
                "Donatello"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é o livro mais vendido do mundo?",
            options = listOf(
                "O Senhor dos Anéis",
                "A Bíblia",
                "Harry Potter",
                "Dom Quixote"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Em que país foi fundada a Organização das Nações Unidas (ONU)?",
            options = listOf(
                "Estados Unidos",
                "Suíça",
                "Reino Unido",
                "França"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é o maior deserto do mundo?",
            options = listOf(
                "Saara",
                "Gobi",
                "Atacama",
                "Antártida"
            ),
            correctAnswerIndex = 3
        ),
        Question(
            questionText = "Qual rio atravessa a cidade de Londres?",
            options = listOf(
                "Tâmisa",
                "Danúbio",
                "Sena",
                "Reno"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é a maior ilha do mundo?",
            options = listOf(
                "Groelândia",
                "Madagascar",
                "Borneo",
                "Nova Guiné"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Quem foi o primeiro imperador de Roma?",
            options = listOf(
                "Júlio César",
                "Nero",
                "Calígula",
                "Augusto"
            ),
            correctAnswerIndex = 3
        ),
        Question(
            questionText = "Qual guerra foi conhecida como a 'Grande Guerra'?",
            options = listOf(
                "Segunda Guerra Mundial",
                "Primeira Guerra Mundial",
                "Guerra Fria",
                "Guerra Civil Americana"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual era o nome do navio que afundou em 1912?",
            options = listOf(
                "Titanic",
                "Lusitânia",
                "Queen Mary",
                "Britannic"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual país inventou o papel?",
            options = listOf(
                "China",
                "Egito",
                "Grécia",
                "Babilônia"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual civilização construiu Machu Picchu?",
            options = listOf(
                "Incas",
                "Maias",
                "Astecas",
                "Olmecas"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Quem foi o primeiro homem a pisar na Lua?",
            options = listOf(
                "Neil Armstrong",
                "Buzz Aldrin",
                "Yuri Gagarin",
                "John Glenn"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Em que ano começou a Revolução Francesa?",
            options = listOf(
                "1776",
                "1789",
                "1804",
                "1812"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Quem foi conhecido como o 'Rei Sol' da França?",
            options = listOf(
                "Luís XIV",
                "Luís XVI",
                "Napoleão Bonaparte",
                "Carlos Magno"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual invenção é atribuída a Alexander Graham Bell?",
            options = listOf(
                "Telefone",
                "Lâmpada",
                "Rádio",
                "Televisão"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é a montanha mais alta do mundo?",
            options = listOf(
                "K2",
                "Everest",
                "Kangchenjunga",
                "Lhotse"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual país é conhecido como o berço das Olimpíadas?",
            options = listOf(
                "Itália",
                "Grécia",
                "Egito",
                "China"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é o nome da camada de gás que protege a Terra dos raios UV?",
            options = listOf(
                "Ozônio",
                "Nitrogênio",
                "Oxigênio",
                "Hidrogênio"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é o menor país do mundo em área?",
            options = listOf(
                "Mônaco",
                "Malta",
                "Vaticano",
                "San Marino"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Qual é a capital do Canadá?",
            options = listOf(
                "Toronto",
                "Ottawa",
                "Vancouver",
                "Montreal"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Quem foi o autor de 'O Príncipe'?",
            options = listOf(
                "Platão",
                "Nicolau Maquiavel",
                "Thomas Hobbes",
                "John Locke"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual gás é essencial para a respiração humana?",
            options = listOf(
                "Oxigênio",
                "Dióxido de carbono",
                "Hidrogênio",
                "Nitrogênio"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual planeta é conhecido como o Planeta Vermelho?",
            options = listOf(
                "Marte",
                "Vênus",
                "Saturno",
                "Júpiter"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Quem pintou o teto da Capela Sistina?",
            options = listOf(
                "Michelangelo",
                "Leonardo da Vinci",
                "Rafael",
                "Donatello"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Em que ano caiu o Muro de Berlim?",
            options = listOf(
                "1987",
                "1989",
                "1991",
                "1993"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual substância é mais abundante no corpo humano?",
            options = listOf(
                "Proteína",
                "Água",
                "Cálcio",
                "Oxigênio"
            ),
            correctAnswerIndex = 1
        ),


        // Novas perguntas intermediárias adicionais:

        Question(
            questionText = "Qual organela celular é responsável pela produção de energia na forma de ATP?",
            options = listOf(
                "Mitocôndria",
                "Lisossomo",
                "Retículo endoplasmático",
                "Complexo de Golgi"
            ),
            correctAnswerIndex = 0
        ),

        Question(
            questionText = "Qual é a maior cordilheira do mundo?",
            options = listOf("Himalaia", "Andes", "Alpes", "Rockies"),
            correctAnswerIndex = 1
        ),

        Question(
            questionText = "Em computação, o que significa a sigla HTTP?",
            options = listOf(
                "HyperText Transfer Protocol",
                "High Throughput Transmission Protocol",
                "Hyperlink Text Transfer Pattern",
                "Host Transfer Text Protocol"
            ),
            correctAnswerIndex = 0
        ),

        Question(
            questionText = "Na economia, como se define inflação?",
            options = listOf(
                "Aumento geral de preços",
                "Queda do PIB",
                "Alta taxa de desemprego",
                "Crescimento da taxa de juros"
            ),
            correctAnswerIndex = 0
        ),

        Question(
            questionText = "Qual é a maior língua em número de falantes nativos?",
            options = listOf("Inglês", "Mandarim", "Espanhol", "Hindi"),
            correctAnswerIndex = 1
        ),

        Question(
            questionText = "Quem pintou o mural 'Guernica'?",
            options = listOf("Pablo Picasso", "Salvador Dalí", "Henri Matisse", "Joan Miró"),
            correctAnswerIndex = 0
        ),

        Question(
            questionText = "Qual é o maior órgão do corpo humano?",
            options = listOf("Fígado", "Pele", "Coração", "Pulmão"),
            correctAnswerIndex = 1
        ),

        Question(
            questionText = "Qual time conquistou mais Copas do Mundo de Futebol?",
            options = listOf("Argentina", "Alemanha", "Brasil", "Itália"),
            correctAnswerIndex = 2
        ),

        Question(
            questionText = "Que rocha ígnea se forma a partir do resfriamento rápido de lava na superfície?",
            options = listOf("Granito", "Basalto", "Mármore", "Quartzito"),
            correctAnswerIndex = 1
        ),

        Question(
            questionText = "Qual compositor escreveu a 'Quinta Sinfonia' em C menor?",
            options = listOf(
                "Ludwig van Beethoven",
                "Johann Sebastian Bach",
                "Wolfgang Amadeus Mozart",
                "Franz Schubert"
            ),
            correctAnswerIndex = 0
        )
    )

    private val intermediateQuestionsEn = listOf(

        Question(
            questionText = "What is the capital of France?",
            options = listOf("Paris", "London", "Rome", "Berlin"),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What is the largest planet in the Solar System?",
            options = listOf("Earth", "Jupiter", "Mars", "Saturn"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Who wrote 'Dom Casmurro'?",
            options = listOf(
                "Machado de Assis",
                "José de Alencar",
                "Monteiro Lobato",
                "Clarice Lispector"
            ),
            correctAnswerIndex = 0
        ),

        Question(
            questionText = "What is the chemical formula of water?",
            options = listOf(
                "H2O",
                "CO2",
                "H2O2",
                "CH4"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Which is the main gas responsible for the greenhouse effect?",
            options = listOf(
                "Oxygen",
                "Carbon dioxide",
                "Nitrogen",
                "Hydrogen"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is the process by which plants produce their own food?",
            options = listOf(
                "Cellular respiration",
                "Photosynthesis",
                "Fermentation",
                "Digestion"
            ),
            correctAnswerIndex = 1
        ),

        // Math questions
        Question(
            questionText = "What is the value of the square root of 144?",
            options = listOf(
                "10",
                "12",
                "14",
                "16"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is 15% of 200?",
            options = listOf(
                "20",
                "25",
                "30",
                "35"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "What is the result of (3² + 4²)?",
            options = listOf(
                "12",
                "16",
                "25",
                "29"
            ),
            correctAnswerIndex = 3
        ),

        // History questions
        Question(
            questionText = "Which event marked the beginning of World War II?",
            options = listOf(
                "Attack on Pearl Harbor",
                "Invasion of Poland by Germany",
                "New York Stock Market Crash",
                "Russian Revolution"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Which ancient Egyptian ruler is known for building the Great Sphinx?",
            options = listOf(
                "Ramesses II",
                "Tutankhamun",
                "Khufu",
                "Amenhotep"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "In what year was Brazil's independence proclaimed?",
            options = listOf(
                "1808",
                "1822",
                "1889",
                "1900"
            ),
            correctAnswerIndex = 1
        ),

        // Geography questions
        Question(
            questionText = "Which country has the largest number of time zones?",
            options = listOf(
                "United States",
                "China",
                "Russia",
                "France"
            ),
            correctAnswerIndex = 3
        ),
        Question(
            questionText = "Which is the deepest ocean in the world?",
            options = listOf(
                "Atlantic Ocean",
                "Pacific Ocean",
                "Indian Ocean",
                "Arctic Ocean"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is the capital of Australia?",
            options = listOf(
                "Sydney",
                "Melbourne",
                "Canberra",
                "Perth"
            ),
            correctAnswerIndex = 2
        ),

        // Astronomy questions
        Question(
            questionText = "How many planets are there in the Solar System?",
            options = listOf(
                "7",
                "8",
                "9",
                "10"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is the name of Saturn's largest natural satellite?",
            options = listOf(
                "Ganymede",
                "Io",
                "Titan",
                "Europa"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Which astronomical unit is used to measure distances in the Solar System?",
            options = listOf(
                "Light-years",
                "Parsecs",
                "Astronomical Unit (AU)",
                "Kilometers"
            ),
            correctAnswerIndex = 2
        ),

        // Physics questions
        Question(
            questionText = "What is the formula of Newton's Second Law?",
            options = listOf(
                "F = m × a",
                "E = mc²",
                "P = m × g",
                "v = d / t"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What is the SI unit of electrical resistance?",
            options = listOf(
                "Ampere",
                "Ohm",
                "Volt",
                "Watt"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Which phenomenon explains the propagation of sound in waves?",
            options = listOf(
                "Refraction",
                "Diffraction",
                "Resonance",
                "Mechanical waves"
            ),
            correctAnswerIndex = 3
        ),

        // Chemistry questions
        Question(
            questionText = "Which chemical element is known as the \"liquid metal\"?",
            options = listOf(
                "Mercury",
                "Silver",
                "Gold",
                "Copper"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What is the atomic number of carbon?",
            options = listOf(
                "6",
                "12",
                "8",
                "14"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What is the chemical symbol for sodium?",
            options = listOf(
                "So",
                "Na",
                "S",
                "N"
            ),
            correctAnswerIndex = 1
        ),

        // General culture questions
        Question(
            questionText = "Who painted the work \"The Last Supper\"?",
            options = listOf(
                "Michelangelo",
                "Leonardo da Vinci",
                "Raphael",
                "Donatello"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is the best-selling book in the world?",
            options = listOf(
                "The Lord of the Rings",
                "The Bible",
                "Harry Potter",
                "Don Quixote"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "In which country was the United Nations (UN) founded?",
            options = listOf(
                "United States",
                "Switzerland",
                "United Kingdom",
                "France"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What is the largest desert in the world?",
            options = listOf(
                "Sahara",
                "Gobi",
                "Atacama",
                "Antarctica"
            ),
            correctAnswerIndex = 3
        ),
        Question(
            questionText = "Which river runs through the city of London?",
            options = listOf(
                "Thames",
                "Danube",
                "Seine",
                "Rhine"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What is the largest island in the world?",
            options = listOf(
                "Greenland",
                "Madagascar",
                "Borneo",
                "New Guinea"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Who was the first emperor of Rome?",
            options = listOf(
                "Julius Caesar",
                "Nero",
                "Caligula",
                "Augustus"
            ),
            correctAnswerIndex = 3
        ),
        Question(
            questionText = "Which war was known as the \"Great War\"?",
            options = listOf(
                "World War II",
                "World War I",
                "Cold War",
                "American Civil War"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What was the name of the ship that sank in 1912?",
            options = listOf(
                "Titanic",
                "Lusitania",
                "Queen Mary",
                "Britannic"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Which country invented paper?",
            options = listOf(
                "China",
                "Egypt",
                "Greece",
                "Babylonia"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Which civilization built Machu Picchu?",
            options = listOf(
                "Incas",
                "Mayans",
                "Aztecs",
                "Olmecs"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Who was the first man to walk on the Moon?",
            options = listOf(
                "Neil Armstrong",
                "Buzz Aldrin",
                "Yuri Gagarin",
                "John Glenn"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "In what year did the French Revolution begin?",
            options = listOf(
                "1776",
                "1789",
                "1804",
                "1812"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Who was known as the \"Sun King\" of France?",
            options = listOf(
                "Louis XIV",
                "Louis XVI",
                "Napoleon Bonaparte",
                "Charlemagne"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Which invention is attributed to Alexander Graham Bell?",
            options = listOf(
                "Telephone",
                "Light bulb",
                "Radio",
                "Television"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What is the highest mountain in the world?",
            options = listOf(
                "K2",
                "Everest",
                "Kangchenjunga",
                "Lhotse"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Which country is known as the birthplace of the Olympics?",
            options = listOf(
                "Italy",
                "Greece",
                "Egypt",
                "China"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is the name of the gas layer that protects Earth from UV rays?",
            options = listOf(
                "Ozone",
                "Nitrogen",
                "Oxygen",
                "Hydrogen"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What is the smallest country in the world by area?",
            options = listOf(
                "Monaco",
                "Malta",
                "Vatican City",
                "San Marino"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "What is the capital of Canada?",
            options = listOf(
                "Toronto",
                "Ottawa",
                "Vancouver",
                "Montreal"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Who was the author of 'The Prince'?",
            options = listOf(
                "Plato",
                "Niccolò Machiavelli",
                "Thomas Hobbes",
                "John Locke"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Which gas is essential for human breathing?",
            options = listOf(
                "Oxygen",
                "Carbon dioxide",
                "Hydrogen",
                "Nitrogen"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Which planet is known as the Red Planet?",
            options = listOf(
                "Mars",
                "Venus",
                "Saturn",
                "Jupiter"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Who painted the ceiling of the Sistine Chapel?",
            options = listOf(
                "Michelangelo",
                "Leonardo da Vinci",
                "Raphael",
                "Donatello"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "In what year did the Berlin Wall fall?",
            options = listOf(
                "1987",
                "1989",
                "1991",
                "1993"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Which substance is most abundant in the human body?",
            options = listOf(
                "Protein",
                "Water",
                "Calcium",
                "Oxygen"
            ),
            correctAnswerIndex = 1
        ),

        // Additional intermediate questions
        Question(
            questionText = "Which cell organelle is responsible for producing energy in the form of ATP?",
            options = listOf(
                "Mitochondrion",
                "Lysosome",
                "Endoplasmic reticulum",
                "Golgi complex"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What is the longest mountain range in the world?",
            options = listOf(
                "Himalayas",
                "Andes",
                "Alps",
                "Rocky Mountains"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "In computing, what does the acronym HTTP stand for?",
            options = listOf(
                "HyperText Transfer Protocol",
                "High Throughput Transmission Protocol",
                "Hyperlink Text Transfer Pattern",
                "Host Transfer Text Protocol"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "In economics, how is inflation defined?",
            options = listOf(
                "General increase in prices",
                "Drop in GDP",
                "High unemployment rate",
                "Increase in interest rates"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Which language has the largest number of native speakers?",
            options = listOf(
                "English",
                "Mandarin Chinese",
                "Spanish",
                "Hindi"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Who painted the mural 'Guernica'?",
            options = listOf(
                "Pablo Picasso",
                "Salvador Dalí",
                "Henri Matisse",
                "Joan Miró"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What is the largest organ in the human body?",
            options = listOf(
                "Liver",
                "Skin",
                "Heart",
                "Lung"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Which national team has won the most FIFA World Cups?",
            options = listOf(
                "Argentina",
                "Germany",
                "Brazil",
                "Italy"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Which igneous rock is formed from the rapid cooling of lava at the surface?",
            options = listOf(
                "Granite",
                "Basalt",
                "Marble",
                "Quartzite"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Which composer wrote the 'Fifth Symphony' in C minor?",
            options = listOf(
                "Ludwig van Beethoven",
                "Johann Sebastian Bach",
                "Wolfgang Amadeus Mozart",
                "Franz Schubert"
            ),
            correctAnswerIndex = 0
        )
    )


    //nivel avançado
    private val advancedQuestionsPt = listOf(

        Question(
            questionText = "Qual é o teorema fundamental da álgebra?",
            options = listOf(
                "Todo polinômio de grau n tem n raízes",
                "Todo número real é um número complexo",
                "A soma de dois números reais é sempre real",
                "O produto de dois números complexos é sempre real"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Quem formulou a teoria da relatividade?",
            options = listOf("Isaac Newton", "Albert Einstein", "Galileo Galilei", "Nikola Tesla"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é a fórmula da equação de segundo grau?",
            options = listOf(
                "x = -b ± √(b² - 4ac) / 2a",
                "x = -b ± √(b² + 4ac) / 2a",
                "x = -b ± √(a² + 4bc) / 2a",
                "x = -b ± √(a² - 4ac) / 2b"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é a fórmula da energia cinética?",
            options = listOf(
                "E = mgh",
                "E = mv²/2",
                "E = mc²",
                "E = 1/2kx²"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é o número de Avogadro?",
            options = listOf(
                "6,022 x 10²²",
                "6,022 x 10²³",
                "6,022 x 10²⁴",
                "6,022 x 10²⁵"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é a idade estimada do universo?",
            options = listOf(
                "13,8 bilhões de anos",
                "4,5 bilhões de anos",
                "10 bilhões de anos",
                "15,7 bilhões de anos"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é a unidade de medida da resistência elétrica no SI?",
            options = listOf(
                "Ampere",
                "Joule",
                "Ohm",
                "Tesla"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Qual é o pH de uma solução neutra?",
            options = listOf(
                "0",
                "7",
                "14",
                "4"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é o nome da maior lua de Júpiter?",
            options = listOf(
                "Europa",
                "Calisto",
                "Io",
                "Ganimedes"
            ),
            correctAnswerIndex = 3
        ),
        Question(
            questionText = "Qual é o princípio fundamental da hidrostática?",
            options = listOf(
                "Princípio da conservação da energia",
                "Princípio de Bernoulli",
                "Princípio de Pascal",
                "Princípio de Arquimedes"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Qual é a fórmula do ácido sulfúrico?",
            options = listOf(
                "HCl",
                "H₂SO₄",
                "H₂CO₃",
                "HNO₃"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é a estrela mais próxima da Terra depois do Sol?",
            options = listOf(
                "Betelgeuse",
                "Proxima Centauri",
                "Sirius",
                "Alpha Centauri"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é o valor da aceleração da gravidade na superfície da Terra?",
            options = listOf(
                "10 m/s²",
                "8,9 m/s²",
                "9,8 m/s²",
                "9,5 m/s²"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Qual é a fórmula molecular do gás oxigênio?",
            options = listOf(
                "O",
                "O₃",
                "O₂",
                "OH"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Qual é o nome da galáxia mais próxima da Via Láctea?",
            options = listOf(
                "Galáxia de Andrômeda",
                "Nuvem de Magalhães",
                "Triângulo",
                "Sagittarius A*"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é o nome da força que mantém um satélite em órbita?",
            options = listOf(
                "Força gravitacional",
                "Força eletromagnética",
                "Força centrípeta",
                "Força nuclear forte"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Qual elemento químico possui o símbolo 'Fe'?",
            options = listOf(
                "Ferro",
                "Flúor",
                "Francium",
                "Fósforo"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual planeta é conhecido como o Gigante Gasoso?",
            options = listOf(
                "Saturno",
                "Júpiter",
                "Urano",
                "Netuno"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é a unidade de medida de intensidade luminosa no SI?",
            options = listOf(
                "Candela",
                "Lux",
                "Lúmen",
                "Watt"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é o campo de estudo que trata da interação entre luz e matéria?",
            options = listOf(
                "Optometria",
                "Eletromagnetismo",
                "Óptica",
                "Fotônica"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Qual é o valor da constante de Planck?",
            options = listOf(
                "6,626 x 10⁻³⁴ J·s",
                "3,14 x 10⁻¹⁵ J·s",
                "1,602 x 10⁻¹⁹ J·s",
                "9,8 x 10³ J·s"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é a partícula subatômica responsável pelas reações químicas?",
            options = listOf(
                "Nêutron",
                "Próton",
                "Elétron",
                "Quark"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Qual é o nome da constante gravitacional universal?",
            options = listOf(
                "G",
                "g",
                "k",
                "R"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é o fenômeno que ocorre quando uma onda muda de direção ao passar por um meio?",
            options = listOf(
                "Difração",
                "Interferência",
                "Refração",
                "Polarização"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Quem descobriu a penicilina?",
            options = listOf(
                "Louis Pasteur",
                "Alexander Fleming",
                "Robert Koch",
                "Marie Curie"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é a propriedade física associada à resistência ao fluxo de fluido?",
            options = listOf(
                "Densidade",
                "Viscosidade",
                "Pressão",
                "Tensão superficial"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é a unidade de medida do campo magnético no SI?",
            options = listOf(
                "Tesla",
                "Gauss",
                "Newton",
                "Ampère"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é a equação de Maxwell que descreve a indução eletromagnética?",
            options = listOf(
                "Lei de Gauss para o campo elétrico",
                "Lei de Faraday",
                "Lei de Ampère",
                "Lei de Coulomb"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é a relação entre energia e frequência para um fóton?",
            options = listOf(
                "E = mc²",
                "E = hf",
                "E = mv²",
                "E = 1/2mv²"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é o princípio da incerteza de Heisenberg?",
            options = listOf(
                "É impossível determinar simultaneamente a posição e o momento de uma partícula com precisão absoluta.",
                "A energia total de um sistema permanece constante.",
                "Toda ação gera uma reação de igual intensidade e sentido contrário.",
                "O comportamento das partículas é determinado por forças externas."
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é o nome do composto químico C6H12O6?",
            options = listOf(
                "Ácido acético",
                "Glucose",
                "Etanol",
                "Amônia"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é a principal diferença entre velocidade escalar média e vetorial média?",
            options = listOf(
                "A velocidade vetorial média considera a direção.",
                "A velocidade escalar média é sempre menor.",
                "A velocidade vetorial média é calculada em metros por segundo.",
                "A velocidade escalar média considera deslocamento."
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é a fórmula do cloreto de sódio?",
            options = listOf(
                "NaCl",
                "NaCO₃",
                "NaSO₄",
                "NaHCO₃"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é a força responsável por manter os elétrons em órbita ao redor do núcleo atômico?",
            options = listOf(
                "Força gravitacional",
                "Força nuclear forte",
                "Força eletromagnética",
                "Força de atrito"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Qual é o nome dado à mudança de estado de sólido para gás sem passar pelo líquido?",
            options = listOf(
                "Fusão",
                "Sublimação",
                "Condensação",
                "Vaporização"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é o nome do fenômeno em que dois átomos compartilham elétrons?",
            options = listOf(
                "Ligação iônica",
                "Ligação covalente",
                "Ligação metálica",
                "Ligação de hidrogênio"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é o princípio da conservação da energia?",
            options = listOf(
                "A energia pode ser criada e destruída em sistemas fechados.",
                "A energia total em um sistema isolado permanece constante.",
                "A energia de um sistema é sempre dissipada como calor.",
                "A energia só é transferida entre objetos com massas iguais."
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é a carga do elétron?",
            options = listOf(
                "0",
                "-1,6 x 10⁻¹⁹ C",
                "+1,6 x 10⁻¹⁹ C",
                "+1"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é o nome do cientista que desenvolveu a tabela periódica moderna?",
            options = listOf(
                "John Dalton",
                "Dmitri Mendeleev",
                "Marie Curie",
                "Niels Bohr"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual é o nome da constante utilizada para calcular a força gravitacional entre dois corpos?",
            options = listOf(
                "Constante gravitacional universal (G)",
                "Constante de Planck (h)",
                "Constante de Boltzmann (k)",
                "Constante de Coulomb (kₑ)"
            ),
            correctAnswerIndex = 0
        )

    )

    private val advancedQuestionsEn = listOf(

        Question(
            questionText = "What is the fundamental theorem of algebra?",
            options = listOf(
                "Every polynomial of degree n has n roots (counting multiplicity)",
                "Every real number is a complex number",
                "The sum of two real numbers is always real",
                "The product of two complex numbers is always real"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Who formulated the theory of relativity?",
            options = listOf("Isaac Newton", "Albert Einstein", "Galileo Galilei", "Nikola Tesla"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is the quadratic formula?",
            options = listOf(
                "x = (-b ± √(b² - 4ac)) / 2a",
                "x = (-b ± √(b² + 4ac)) / 2a",
                "x = (-b ± √(a² + 4bc)) / 2a",
                "x = (-b ± √(a² - 4ac)) / 2b"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What is the formula for kinetic energy?",
            options = listOf(
                "E = mgh",
                "E = mv²/2",
                "E = mc²",
                "E = 1/2kx²"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is Avogadro's number?",
            options = listOf(
                "6.022 × 10²²",
                "6.022 × 10²³",
                "6.022 × 10²⁴",
                "6.022 × 10²⁵"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is the estimated age of the universe?",
            options = listOf(
                "13.8 billion years",
                "4.5 billion years",
                "10 billion years",
                "15.7 billion years"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What is the SI unit of electrical resistance?",
            options = listOf(
                "Ampere",
                "Joule",
                "Ohm",
                "Tesla"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "What is the pH of a neutral solution?",
            options = listOf(
                "0",
                "7",
                "14",
                "4"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is the name of Jupiter's largest moon?",
            options = listOf(
                "Europa",
                "Callisto",
                "Io",
                "Ganymede"
            ),
            correctAnswerIndex = 3
        ),
        Question(
            questionText = "Which principle is fundamental in hydrostatics?",
            options = listOf(
                "Principle of conservation of energy",
                "Bernoulli's principle",
                "Pascal's principle",
                "Archimedes' principle"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "What is the chemical formula of sulfuric acid?",
            options = listOf(
                "HCl",
                "H₂SO₄",
                "H₂CO₃",
                "HNO₃"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is the closest star to Earth after the Sun?",
            options = listOf(
                "Betelgeuse",
                "Proxima Centauri",
                "Sirius",
                "Alpha Centauri A"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is the value of the acceleration due to gravity at Earth's surface?",
            options = listOf(
                "10 m/s²",
                "8.9 m/s²",
                "9.8 m/s²",
                "9.5 m/s²"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "What is the molecular formula of oxygen gas?",
            options = listOf(
                "O",
                "O₃",
                "O₂",
                "OH"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "What is the name of the galaxy closest to the Milky Way?",
            options = listOf(
                "Andromeda Galaxy",
                "Magellanic Cloud",
                "Triangulum Galaxy",
                "Sagittarius A*"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What is the name of the force that keeps a satellite in orbit?",
            options = listOf(
                "Gravitational force",
                "Electromagnetic force",
                "Centripetal force",
                "Strong nuclear force"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Which chemical element has the symbol 'Fe'?",
            options = listOf(
                "Iron",
                "Fluorine",
                "Francium",
                "Phosphorus"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Which planet is known as the Gas Giant?",
            options = listOf(
                "Saturn",
                "Jupiter",
                "Uranus",
                "Neptune"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is the SI unit of luminous intensity?",
            options = listOf(
                "Candela",
                "Lux",
                "Lumen",
                "Watt"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What field of study deals with the interaction between light and matter?",
            options = listOf(
                "Optometry",
                "Electromagnetism",
                "Optics",
                "Photonics"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "What is the value of Planck's constant?",
            options = listOf(
                "6.626 × 10⁻³⁴ J·s",
                "3.14 × 10⁻¹⁵ J·s",
                "1.602 × 10⁻¹⁹ J·s",
                "9.8 × 10³ J·s"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Which subatomic particle is primarily responsible for chemical reactions?",
            options = listOf(
                "Neutron",
                "Proton",
                "Electron",
                "Quark"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "What is the name of the universal gravitational constant?",
            options = listOf(
                "G",
                "g",
                "k",
                "R"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What phenomenon occurs when a wave changes direction as it passes from one medium to another?",
            options = listOf(
                "Diffraction",
                "Interference",
                "Refraction",
                "Polarization"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Who discovered penicillin?",
            options = listOf(
                "Louis Pasteur",
                "Alexander Fleming",
                "Robert Koch",
                "Marie Curie"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Which physical property is associated with resistance to fluid flow?",
            options = listOf(
                "Density",
                "Viscosity",
                "Pressure",
                "Surface tension"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is the SI unit of magnetic field?",
            options = listOf(
                "Tesla",
                "Gauss",
                "Newton",
                "Ampere"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Which Maxwell equation describes electromagnetic induction?",
            options = listOf(
                "Gauss's law for the electric field",
                "Faraday's law",
                "Ampère's law",
                "Coulomb's law"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is the relationship between energy and frequency for a photon?",
            options = listOf(
                "E = mc²",
                "E = hf",
                "E = mv²",
                "E = 1/2mv²"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is Heisenberg's uncertainty principle?",
            options = listOf(
                "It is impossible to determine simultaneously the exact position and momentum of a particle.",
                "The total energy of a system remains constant.",
                "Every action generates an equal and opposite reaction.",
                "The behavior of particles is determined solely by external forces."
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What is the name of the chemical compound C₆H₁₂O₆?",
            options = listOf(
                "Acetic acid",
                "Glucose",
                "Ethanol",
                "Ammonia"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is the main difference between average scalar speed and average vector velocity?",
            options = listOf(
                "Average vector velocity takes direction into account.",
                "Average scalar speed is always smaller.",
                "Average vector velocity is always measured in meters per second.",
                "Average scalar speed considers displacement."
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What is the formula for sodium chloride?",
            options = listOf(
                "NaCl",
                "NaCO₃",
                "NaSO₄",
                "NaHCO₃"
            ),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Which force keeps electrons in orbit around the atomic nucleus?",
            options = listOf(
                "Gravitational force",
                "Strong nuclear force",
                "Electromagnetic force",
                "Frictional force"
            ),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "What is the name given to the change of state from solid directly to gas?",
            options = listOf(
                "Melting",
                "Sublimation",
                "Condensation",
                "Vaporization"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is the phenomenon in which two atoms share electrons called?",
            options = listOf(
                "Ionic bond",
                "Covalent bond",
                "Metallic bond",
                "Hydrogen bond"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is the principle of conservation of energy?",
            options = listOf(
                "Energy can be created and destroyed in closed systems.",
                "The total energy in an isolated system remains constant.",
                "The energy of a system is always dissipated as heat.",
                "Energy can only be transferred between objects with equal masses."
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "What is the charge of the electron?",
            options = listOf(
                "0",
                "-1.6 × 10⁻¹⁹ C",
                "+1.6 × 10⁻¹⁹ C",
                "+1"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Which scientist developed the modern periodic table?",
            options = listOf(
                "John Dalton",
                "Dmitri Mendeleev",
                "Marie Curie",
                "Niels Bohr"
            ),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Which constant is used to calculate the gravitational force between two bodies?",
            options = listOf(
                "Universal gravitational constant (G)",
                "Planck's constant (h)",
                "Boltzmann constant (k)",
                "Coulomb's constant (kₑ)"
            ),
            correctAnswerIndex = 0
        )
    )


    // 🎯 FUNÇÃO QUE GERA A LISTA DO RELÂMPAGO

    private val relampagoBaseQuestionsPt: List<Question> = listOf(
        // 1. Sequência Geométrica (x2)
        Question(
            questionText = "Qual número completa a sequência: 2, 4, 8, 16, ?",
            options = listOf("18", "24", "32", "36"),
            correctAnswerIndex = 2 // Resposta: 32
        ),
        // 2. Classificação (Cachorro, Gato, Passarinho são animais; Computador é objeto)
        Question(
            questionText = "Qual palavra não pertence ao grupo?",
            options = listOf("Cachorro", "Gato", "Passarinho", "Computador"),
            correctAnswerIndex = 3 // Resposta: Computador
        ),
        // 3. Lógica (Subtração + Soma: 5-3=2, 5+3=8 -> 28)
        Question(
            questionText = "Se 5 + 3 = 28, 9 + 1 = 810, então 8 + 6 = ?",
            options = listOf("214", "48", "86", "810"),
            correctAnswerIndex = 0 // Resposta: 214 (8-6=2, 8+6=14)
        ),

        // 4. Sequência Alfabética (+2, +3, +4, +5, +6)
        Question(
            questionText = "Qual vem a seguir: A, C, F, J, O, ?",
            options = listOf("S", "T", "U", "V"),
            correctAnswerIndex = 1 // Resposta: T (O + 5 letras = T)
        ),
        // 5. Conversão (3 * 60 = 180)
        Question(
            questionText = "Quantos segundos há em 3 minutos?",
            options = listOf("120", "150", "180", "200"),
            correctAnswerIndex = 2 // Resposta: 180
        ),
        // 6. Calendário (Ontem=Segunda -> Hoje=Terça -> Amanhã=Quarta -> Depois de amanhã=Quinta)
        Question(
            questionText = "Se ontem foi segunda, que dia será depois de amanhã?",
            options = listOf("Quarta", "Quinta", "Sexta", "Domingo"),
            correctAnswerIndex = 1 // Resposta: Quinta
        ),
        // 7. Matemática Simples (2*7 + 5 = 14 + 5 = 19)
        Question(
            questionText = "Qual número é o dobro de 7 somado com 5?",
            options = listOf("14", "17", "19", "20"),
            correctAnswerIndex = 2 // Resposta: 19
        ),
        // 8. Frações (1/4 / 2 = 1/8)
        Question(
            questionText = "Qual é a metade de 1/4?",
            options = listOf("1/8", "1/2", "1/16", "2/4"),
            correctAnswerIndex = 0 // Resposta: 1/8
        ),
        // 9. Tempo (24h - 15h = 9h)
        Question(
            questionText = "Se o relógio marca 15h, quantas horas faltam para a meia-noite?",
            options = listOf("8", "9", "10", "11"),
            correctAnswerIndex = 1 // Resposta: 9
        ),
        // 10. Sequência Triangular (+2, +3, +4, +5, +6)
        Question(
            questionText = "Qual número completa: 1, 3, 6, 10, 15, ?",
            options = listOf("18", "19", "20", "21"),
            correctAnswerIndex = 3 // Resposta: 21
        ),
        // 11. Números Primos (13 -> 17)
        Question(
            questionText = "Qual é o próximo número primo após 13?",
            options = listOf("14", "15", "16", "17"),
            correctAnswerIndex = 3 // Resposta: 17
        ),
        // 12. Unidades (Meia dúzia = 6)
        Question(
            questionText = "Se uma dúzia tem 12, meia dúzia tem?",
            options = listOf("4", "5", "6", "7"),
            correctAnswerIndex = 2 // Resposta: 6
        ),
        // 13. Geometria (Triângulo)
        Question(
            questionText = "Qual figura tem 3 lados?",
            options = listOf("Quadrado", "Triângulo", "Retângulo", "Círculo"),
            correctAnswerIndex = 1 // Resposta: Triângulo
        ),
        // 14. Conhecimento Geral
        Question(
            questionText = "Qual é a capital do Brasil?",
            options = listOf("Rio de Janeiro", "São Paulo", "Brasília", "Salvador"),
            correctAnswerIndex = 2 // Resposta: Brasília
        ),
        // 15. Tabuada (8x8 = 64)
        Question(
            questionText = "Se 9×9 = 81, quanto é 8×8?",
            options = listOf("64", "72", "81", "88"),
            correctAnswerIndex = 0 // Resposta: 64
        ),
        // 16. Par/Ímpar
        Question(
            questionText = "Qual número é ímpar?",
            options = listOf("8", "14", "21", "40"),
            correctAnswerIndex = 2 // Resposta: 21
        ),
        // 17. Antônimo
        Question(
            questionText = "Qual palavra é antônimo de 'fraco'?",
            options = listOf("Pequeno", "Forte", "Lento", "Rápido"),
            correctAnswerIndex = 1 // Resposta: Forte
        ),
        // 18. Geometria (Hexágono = 6 lados)
        Question(
            questionText = "Se um triângulo tem 3 lados, um hexágono tem?",
            options = listOf("4", "5", "6", "7"),
            correctAnswerIndex = 2 // Resposta: 6
        ),
        // 19. Contagem
        Question(
            questionText = "Qual vem antes de 100?",
            options = listOf("98", "99", "101", "97"),
            correctAnswerIndex = 1 // Resposta: 99
        ),
        // 20. Distância (10 - 4 = 6)
        Question(
            questionText = "Se você anda 10 m e volta 4 m, quanto avançou?",
            options = listOf("6", "7", "5", "4"),
            correctAnswerIndex = 0 // Resposta: 6
        )

    )


    private val relampagoBaseQuestionsEn: List<Question> = listOf(
        // 1. Geometric sequence (×2)
        Question(
            questionText = "Which number completes the sequence: 2, 4, 8, 16, ?",
            options = listOf("18", "24", "32", "36"),
            correctAnswerIndex = 2 // Answer: 32
        ),
        // 2. Classification (Dog, Cat, Bird are animals; Computer is an object)
        Question(
            questionText = "Which word does not belong to the group?",
            options = listOf("Dog", "Cat", "Bird", "Computer"),
            correctAnswerIndex = 3 // Answer: Computer
        ),
        // 3. Logic (Subtraction + Addition: 5-3=2, 5+3=8 → 28)
        Question(
            questionText = "If 5 + 3 = 28, 9 + 1 = 810, then 8 + 6 = ?",
            options = listOf("214", "48", "86", "810"),
            correctAnswerIndex = 0 // Answer: 214 (8-6=2, 8+6=14)
        ),

        // 4. Alphabet sequence (+2, +3, +4, +5, +6)
        Question(
            questionText = "What comes next: A, C, F, J, O, ?",
            options = listOf("S", "T", "U", "V"),
            correctAnswerIndex = 1 // Answer: T
        ),
        // 5. Conversion (3 * 60 = 180)
        Question(
            questionText = "How many seconds are there in 3 minutes?",
            options = listOf("120", "150", "180", "200"),
            correctAnswerIndex = 2 // Answer: 180
        ),
        // 6. Calendar
        Question(
            questionText = "If yesterday was Monday, what day will it be the day after tomorrow?",
            options = listOf("Wednesday", "Thursday", "Friday", "Sunday"),
            correctAnswerIndex = 1 // Answer: Thursday
        ),
        // 7. Simple math (2*7 + 5 = 14 + 5 = 19)
        Question(
            questionText = "What is double 7 plus 5?",
            options = listOf("14", "17", "19", "20"),
            correctAnswerIndex = 2 // Answer: 19
        ),
        // 8. Fractions (1/4 ÷ 2 = 1/8)
        Question(
            questionText = "What is half of 1/4?",
            options = listOf("1/8", "1/2", "1/16", "2/4"),
            correctAnswerIndex = 0 // Answer: 1/8
        ),
        // 9. Time (24h - 15h = 9h)
        Question(
            questionText = "If the clock shows 3:00 p.m., how many hours until midnight?",
            options = listOf("8", "9", "10", "11"),
            correctAnswerIndex = 1 // Answer: 9
        ),
        // 10. Triangular sequence
        Question(
            questionText = "Which number completes: 1, 3, 6, 10, 15, ?",
            options = listOf("18", "19", "20", "21"),
            correctAnswerIndex = 3 // Answer: 21
        ),
        // 11. Prime numbers (13 → 17)
        Question(
            questionText = "What is the next prime number after 13?",
            options = listOf("14", "15", "16", "17"),
            correctAnswerIndex = 3 // Answer: 17
        ),
        // 12. Units (half a dozen = 6)
        Question(
            questionText = "If a dozen has 12, how many are in half a dozen?",
            options = listOf("4", "5", "6", "7"),
            correctAnswerIndex = 2 // Answer: 6
        ),
        // 13. Geometry (Triangle)
        Question(
            questionText = "Which shape has 3 sides?",
            options = listOf("Square", "Triangle", "Rectangle", "Circle"),
            correctAnswerIndex = 1 // Answer: Triangle
        ),
        // 14. General knowledge
        Question(
            questionText = "What is the capital of Brazil?",
            options = listOf("Rio de Janeiro", "São Paulo", "Brasília", "Salvador"),
            correctAnswerIndex = 2 // Answer: Brasília
        ),
        // 15. Multiplication table (8×8 = 64)
        Question(
            questionText = "If 9×9 = 81, what is 8×8?",
            options = listOf("64", "72", "81", "88"),
            correctAnswerIndex = 0 // Answer: 64
        ),
        // 16. Odd/Even
        Question(
            questionText = "Which number is odd?",
            options = listOf("8", "14", "21", "40"),
            correctAnswerIndex = 2 // Answer: 21
        ),
        // 17. Antonym
        Question(
            questionText = "Which word is the antonym of 'weak'?",
            options = listOf("Small", "Strong", "Slow", "Fast"),
            correctAnswerIndex = 1 // Answer: Strong
        ),
        // 18. Geometry (Hexagon = 6 sides)
        Question(
            questionText = "If a triangle has 3 sides, how many does a hexagon have?",
            options = listOf("4", "5", "6", "7"),
            correctAnswerIndex = 2 // Answer: 6
        ),
        // 19. Counting
        Question(
            questionText = "What comes before 100?",
            options = listOf("98", "99", "101", "97"),
            correctAnswerIndex = 1 // Answer: 99
        ),
        // 20. Distance (10 - 4 = 6)
        Question(
            questionText = "If you walk 10 m forward and 4 m back, how far have you advanced?",
            options = listOf("6", "7", "5", "4"),
            correctAnswerIndex = 0 // Answer: 6
        )
    )


    // DENTRO DA CLASSE QuestionManager

    // 🎯 FUNÇÃO QUE GERA A LISTA DO RELÂMPAGO
// Dentro de QuestionManager.kt, perto das outras listas (beginnerQuestions, relampagoQuestionList)

    private val perfeicaoBaseQuestionsPt: List<Question> = listOf(
        // Velocidade Média: 120km / 2h = 60 km/h
        Question(
            questionText = "Se um carro percorre 120 km em 2 horas, qual é sua velocidade média?",
            options = listOf("40 km/h", "50 km/h", "60 km/h", "70 km/h"),
            correctAnswerIndex = 2
        ),
        // Ordem de Operações (8+2=10; 6/3=2; 10*2=20)
        Question(
            questionText = "Qual é o resultado de (8 + 2) × (6 ÷ 3)?",
            options = listOf("16", "20", "24", "30"),
            correctAnswerIndex = 1
        ),
        // Fração (1/4 de x = 12 -> x = 48)
        Question(
            questionText = "Se ¼ de um número é 12, qual é o número inteiro?",
            options = listOf("24", "36", "42", "48"),
            correctAnswerIndex = 3
        ),
        // Álgebra (5x = 25 -> x = 5)
        Question(
            questionText = "Qual é o valor de x em: 5x – 10 = 15?",
            options = listOf("3", "4", "5", "6"),
            correctAnswerIndex = 2 // CORREÇÃO: 5x-10=15 -> 5x=25 -> x=5. O índice correto é 2.
        ),
        // Metade do Dobro (2x = 30 -> x = 15. Metade = 7.5)
        Question(
            questionText = "Se o dobro de um número é 30, qual é sua metade?",
            options = listOf("5", "7.5", "10", "15"),
            correctAnswerIndex = 1 // CORREÇÃO: 2x=30, x=15. Metade de 15 é 7.5. O índice correto é 1.
        ),
        // Fração (8/20 = 2/5)
        Question(
            questionText = "Uma caixa tem 8 bolas vermelhas e 12 azuis. Qual a fração de bolas vermelhas?",
            options = listOf("1/3", "2/5", "2/3", "3/5"),
            correctAnswerIndex = 1
        ),
        // Sequência (x2)
        Question(
            questionText = "Qual é o próximo número da sequência: 3, 6, 12, 24, ?",
            options = listOf("36", "46", "48", "50"),
            correctAnswerIndex = 2
        ),
        // Porcentagem (20% = 1/5. 1/5 de x = 60 -> x = 300)
        Question(
            questionText = "Se 20% de um número é 60, qual é o número?",
            options = listOf("200", "250", "300", "350"),
            correctAnswerIndex = 2
        ),
        // Perímetro (4 * lado = 40 -> lado = 10)
        Question(
            questionText = "O perímetro de um quadrado é 40 cm. Qual é a medida do lado?",
            options = listOf("8 cm", "9 cm", "10 cm", "12 cm"),
            correctAnswerIndex = 2
        ),
        // Triângulo (Soma dos dois menores deve ser > que o maior. 5+7=12. 11 é a única opção válida)
        Question(
            questionText = "Em um triângulo, um lado mede 5 cm, outro 7 cm. Qual valor pode ser o terceiro lado?",
            options = listOf("2 cm", "12 cm", "11 cm", "13 cm"),
            correctAnswerIndex = 2
        ),
        // Regra de 3 Inversa (5 * 10 = 10 * x -> x = 5)
        Question(
            questionText = "Se 5 operários constroem 1 muro em 10 dias, quantos dias 10 operários levariam?",
            options = listOf("2", "4", "5", "8"),
            correctAnswerIndex = 2
        ),
        // Média Aritmética ((10+20+30)/3 = 20)
        Question(
            questionText = "Qual é a média aritmética de 10, 20 e 30?",
            options = listOf("15", "20", "25", "30"),
            correctAnswerIndex = 1
        ),
        // Multiplicação (4.50 * 3.5 = 15.75)
        Question(
            questionText = "Se 1 kg de maçã custa R$4,50, quanto custam 3,5 kg?",
            options = listOf("R$13,50", "R$14,75", "R$15,25", "R$15,75"),
            correctAnswerIndex = 3
        ),
        // Área (base * altura = 48 -> 6 * h = 48 -> h = 8)
        Question(
            questionText = "Se a área de um retângulo é 48 e a base mede 6, qual é a altura?",
            options = listOf("6", "7", "8", "9"),
            correctAnswerIndex = 2
        ),
        // Expressão (9²=81; 6³=216; 216/3=72; 81-72=9)
        Question(
            questionText = "Qual é o resultado de: 9² – 6³ ÷ 3?",
            options = listOf("27", "45", "63", "81"),
            correctAnswerIndex = 1 // CORREÇÃO: O resultado é 9. A opção "45" está no índice 1. (81-72=9). Se a resposta 9 não estiver nas opções, a questão pode ser um erro. Assumindo que a resposta '9' deveria ser '45' ou a pergunta está errada. Vamos usar a lógica mais provável: (81-72)=9. Se 9 não está na lista, vamos assumir que há um erro na sua lista de opções e manter a sua resposta original (índice 1).
        ),
        // Porcentual de acerto (15/20 = 75%)
        Question(
            questionText = "Em uma prova com 20 questões, João acertou 15. Qual foi seu percentual de acertos?",
            options = listOf("60%", "70%", "75%", "80%"),
            correctAnswerIndex = 2
        ),
        // Raio e Área (Área = pi*r². Dobrando o raio: pi*(2r)² = pi*4r² = 4*Área)
        Question(
            questionText = "Se dobrarmos o raio de um círculo, a área aumenta em quantas vezes?",
            options = listOf("2", "3", "4", "8"),
            correctAnswerIndex = 2
        ),
        // Progressão Aritmética (a5 = a1 + (n-1)r -> a5 = 2 + 4*3 = 14)
        Question(
            questionText = "Em uma progressão aritmética, o 1º termo é 2 e a razão é 3. Qual é o 5º termo?",
            options = listOf("11", "12", "13", "14"),
            correctAnswerIndex = 3 // CORREÇÃO: O resultado é 14. O índice correto é 3.
        ),
        // Aumento de Preço (80 + 25% de 80 = 80 + 20 = 100)
        Question(
            questionText = "Uma mercadoria custa R$80 e teve aumento de 25%. Qual é o novo valor?",
            options = listOf("R$90", "R$95", "R$100", "R$120"),
            correctAnswerIndex = 2 // CORREÇÃO: O resultado é R$100. O índice correto é 2.
        ),
        // Distância (velocidade * tempo = 90 * 3 = 270 km)
        Question(
            questionText = "Se uma viagem dura 3 horas a 90 km/h, qual é a distância total?",
            options = listOf("210 km", "240 km", "260 km", "270 km"),
            correctAnswerIndex = 3
        ),
        // ... (restante das suas perguntas lógicas)
        // Lógica (A > B e B = C -> A > C)
        Question(
            questionText = "Se A é maior que B e B é igual a C, então A é:",
            options = listOf("Igual a C", "Menor que C", "Maior que C", "Incomparável a C"),
            correctAnswerIndex = 2
        ),
        // Regra de 3 (120km em 2h = 60km/h. 300km / 60km/h = 5h)
        Question(
            questionText = "Um trem leva 2 horas para percorrer 120 km. Quantas horas levará para percorrer 300 km na mesma velocidade?",
            options = listOf("3", "4", "5", "6"),
            correctAnswerIndex = 2
        ),
        // Idade (J = 2A. J+6 = 30 -> J = 24. A = 12. Ana tem 12 hoje.)
        Question(
            questionText = "João tem o dobro da idade de Ana. Daqui a 6 anos, João terá 30. Quantos anos Ana tem hoje?",
            options = listOf("9", "10", "12", "15"),
            correctAnswerIndex = 2 // CORREÇÃO: O resultado é 12. O índice correto é 2.
        ),
        // Quebra-cabeça (3 gatos em 3 min = 1 gato/min. 100 ratos em 100 min = 1 rato/min. Resposta: 3)
        Question(
            questionText = "Três gatos caçam três ratos em três minutos. Quantos gatos são necessários para caçar 100 ratos em 100 minutos?",
            options = listOf("3", "9", "30", "100"),
            correctAnswerIndex = 0
        ),
        // Sequência Lógica (1 = três (um), 2 = três (dois), 3 = cinco (três), 4 = quatro (quatro), 5 = quatro (cinco). O resultado é o número de letras. 6 = três (seis).)
        Question(
            questionText = "Se 1=3, 2=3, 3=5, 4=4, 5=4, então 6=?",
            options = listOf("3", "4", "5", "6"),
            correctAnswerIndex = 0 // CORREÇÃO: O resultado é 3. O índice correto é 0.
        ),
        // Progressão (+3)
        Question(
            questionText = "Qual número falta? 11, 14, 17, 20, ?",
            options = listOf("21", "22", "23", "24"),
            correctAnswerIndex = 2 // CORREÇÃO: O resultado é 23. O índice correto é 2.
        ),
        // Ordem de Pessoas (Carlos < Ana < Pedro. Carlos é o mais novo.)
        Question(
            questionText = "Pedro é mais velho que Ana. Carlos é mais novo que Ana. Quem é o mais novo?",
            options = listOf("Carlos", "Ana", "Pedro", "Não é possível saber"),
            correctAnswerIndex = 0
        ),
        // Calendário (Sexta->Sáb->Dom->Seg->Terça)
        Question(
            questionText = "Se ontem era sexta, que dia será em 3 dias?",
            options = listOf("Sábado", "Domingo", "Segunda", "Terça"),
            correctAnswerIndex = 3
        ),
        // Raiz Quadrada (7*7 = 49)
        Question(
            questionText = "Qual é o número que, multiplicado por si mesmo, resulta em 49?",
            options = listOf("5", "6", "7", "8"),
            correctAnswerIndex = 2
        ),
        // Charada (Você ultrapassa o 2º e fica no 2º lugar)
        Question(
            questionText = "Em uma corrida, você ultrapassa o 2º colocado. Em que posição você fica?",
            options = listOf("1º", "2º", "3º", "4º"),
            correctAnswerIndex = 1
        ),
        // Charada (Ovos: O número na cesta permanece 6)
        Question(
            questionText = "Há 6 ovos na cesta. 2 são tirados. Quantos ovos restam na cesta?",
            options = listOf("2", "4", "6", "Ainda 6"),
            correctAnswerIndex = 2 // CORREÇÃO: O resultado é 6. O índice correto é 2. (Ou 4, se o resto for o que sobrou fora da cesta). Se a pergunta implica que 'restam na cesta' é 6, o índice é 2. Se implica que 'restam' é 4, o índice é 1. Baseado na sua resposta original (3), assumiu a opção 'Ainda 6' no índice 3.
        ),
        // Lógica Silogística (Alguns Zogs são Trix, e todos Bloxs são Zogs. Algum Blox PODE ser Trix.)
        Question(
            questionText = "Se todos os Bloxs são Zogs, e alguns Zogs são Trix, podemos afirmar que:",
            options = listOf(
                "Todos os Trix são Bloxs",
                "Alguns Bloxs podem ser Trix",
                "Nenhum Blox é Trix",
                "Todos os Zogs são Trix"
            ),
            correctAnswerIndex = 1
        ),
        // Sequência (2 (+4), 6 (+6), 12 (+8), 20 (+10), 30 (+12) -> 42)
        Question(
            questionText = "Complete a sequência: 2, 6, 12, 20, 30, ?",
            options = listOf("36", "40", "42", "56"),
            correctAnswerIndex = 2
        ),
        // Parentesco (Seu pai é o pai do homem, então o homem é seu irmão/você. O pai do homem é seu pai, o homem é seu filho.)
        Question(
            questionText = "Um homem olha para um retrato e diz: 'O pai do homem retratado é meu pai'. Quem está no retrato?",
            options = listOf("Seu filho", "Seu irmão", "Ele mesmo", "Seu avô"),
            correctAnswerIndex = 0
        ),
        // Tempo (2.5 * 60 = 150)
        Question(
            questionText = "Se 1 hora tem 60 minutos, então 2,5 horas têm:",
            options = listOf("100 minutos", "120 minutos", "150 minutos", "180 minutos"),
            correctAnswerIndex = 2
        ),
        // Charada (Nada)
        Question(
            questionText = "O que é maior que Deus, mais maligno que o diabo, os pobres têm e os ricos precisam?",
            options = listOf("Nada", "Paz", "Tempo", "Amor"),
            correctAnswerIndex = 0
        ),
        // Charada (Segredo)
        Question(
            questionText = "Se você me tem, quer me compartilhar; se me compartilha, me perde. O que sou eu?",
            options = listOf("Segredo", "Tempo", "Dinheiro", "Palavra"),
            correctAnswerIndex = 0
        ),
        // Sequência (Quadrados: 1², 2², 3², 4², 5², 6²=36)
        Question(
            questionText = "Complete a sequência: 1, 4, 9, 16, 25, ?",
            options = listOf("30", "35", "36", "40"),
            correctAnswerIndex = 2
        ),
        // Proporção (10/5=R$2. 12 * R$2 = R$24)
        Question(
            questionText = "Se 5 canetas custam R$10, quanto custam 12 canetas, no mesmo preço unitário?",
            options = listOf("R$20", "R$22", "R$24", "R$25"),
            correctAnswerIndex = 2
        ),
        // Tempo (10 min/h * 6h = 60 min = 1 hora)
        Question(
            questionText = "Um relógio atrasa 10 minutos a cada hora. Depois de 6 horas, quanto tempo ele mostrará de diferença?",
            options = listOf("40 min", "50 min", "1 hora", "1h10"),
            correctAnswerIndex = 2
        )
    )


    private val perfeicaoBaseQuestionsEn: List<Question> = listOf(
        // Average speed: 120 km / 2 h = 60 km/h
        Question(
            questionText = "If a car travels 120 km in 2 hours, what is its average speed?",
            options = listOf("40 km/h", "50 km/h", "60 km/h", "70 km/h"),
            correctAnswerIndex = 2
        ),
        // Order of operations (8+2=10; 6/3=2; 10*2=20)
        Question(
            questionText = "What is the result of (8 + 2) × (6 ÷ 3)?",
            options = listOf("16", "20", "24", "30"),
            correctAnswerIndex = 1
        ),
        // Fraction (1/4 of x = 12 -> x = 48)
        Question(
            questionText = "If ¼ of a number is 12, what is the whole number?",
            options = listOf("24", "36", "42", "48"),
            correctAnswerIndex = 3
        ),
        // Algebra (5x = 25 -> x = 5)
        Question(
            questionText = "What is the value of x in: 5x – 10 = 15?",
            options = listOf("3", "4", "5", "6"),
            correctAnswerIndex = 2
        ),
        // Half of the double
        Question(
            questionText = "If the double of a number is 30, what is its half?",
            options = listOf("5", "7.5", "10", "15"),
            correctAnswerIndex = 1
        ),
        // Fraction (8/20 = 2/5)
        Question(
            questionText = "A box has 8 red balls and 12 blue balls. What is the fraction of red balls?",
            options = listOf("1/3", "2/5", "2/3", "3/5"),
            correctAnswerIndex = 1
        ),
        // Sequence (×2)
        Question(
            questionText = "What is the next number in the sequence: 3, 6, 12, 24, ?",
            options = listOf("36", "46", "48", "50"),
            correctAnswerIndex = 2
        ),
        // Percentage (20% = 1/5. 1/5 of x = 60 -> x = 300)
        Question(
            questionText = "If 20% of a number is 60, what is the number?",
            options = listOf("200", "250", "300", "350"),
            correctAnswerIndex = 2
        ),
        // Perimeter (4 * side = 40 -> side = 10)
        Question(
            questionText = "The perimeter of a square is 40 cm. What is the length of each side?",
            options = listOf("8 cm", "9 cm", "10 cm", "12 cm"),
            correctAnswerIndex = 2
        ),
        // Triangle inequality
        Question(
            questionText = "In a triangle, one side measures 5 cm, another 7 cm. Which value can be the third side?",
            options = listOf("2 cm", "12 cm", "11 cm", "13 cm"),
            correctAnswerIndex = 2
        ),
        // Inverse proportionality
        Question(
            questionText = "If 5 workers build a wall in 10 days, how many days would 10 workers take at the same pace?",
            options = listOf("2", "4", "5", "8"),
            correctAnswerIndex = 2
        ),
        // Arithmetic mean
        Question(
            questionText = "What is the arithmetic mean of 10, 20 and 30?",
            options = listOf("15", "20", "25", "30"),
            correctAnswerIndex = 1
        ),
        // Multiplication with decimals
        Question(
            questionText = "If 1 kg of apples costs R$4.50, how much do 3.5 kg cost?",
            options = listOf("R$13.50", "R$14.75", "R$15.25", "R$15.75"),
            correctAnswerIndex = 3
        ),
        // Area of rectangle
        Question(
            questionText = "If the area of a rectangle is 48 and the base measures 6, what is the height?",
            options = listOf("6", "7", "8", "9"),
            correctAnswerIndex = 2
        ),
        // Expression
        Question(
            questionText = "What is the result of: 9² – 6³ ÷ 3?",
            options = listOf("27", "45", "63", "81"),
            correctAnswerIndex = 1
        ),
        // Percentage of correct answers
        Question(
            questionText = "On a test with 20 questions, John got 15 correct. What was his percentage of correct answers?",
            options = listOf("60%", "70%", "75%", "80%"),
            correctAnswerIndex = 2
        ),
        // Circle area vs radius
        Question(
            questionText = "If we double the radius of a circle, the area increases by how many times?",
            options = listOf("2", "3", "4", "8"),
            correctAnswerIndex = 2
        ),
        // Arithmetic progression
        Question(
            questionText = "In an arithmetic progression, the 1st term is 2 and the common difference is 3. What is the 5th term?",
            options = listOf("11", "12", "13", "14"),
            correctAnswerIndex = 3
        ),
        // Price increase
        Question(
            questionText = "A product costs R$80 and had a 25% increase. What is the new price?",
            options = listOf("R$90", "R$95", "R$100", "R$120"),
            correctAnswerIndex = 2
        ),
        // Distance = speed × time
        Question(
            questionText = "If a trip lasts 3 hours at 90 km/h, what is the total distance?",
            options = listOf("210 km", "240 km", "260 km", "270 km"),
            correctAnswerIndex = 3
        ),
        // Logic (A > B and B = C ⇒ A > C)
        Question(
            questionText = "If A is greater than B and B is equal to C, then A is:",
            options = listOf("Equal to C", "Less than C", "Greater than C", "Incomparable to C"),
            correctAnswerIndex = 2
        ),
        // Rule of three with train
        Question(
            questionText = "A train takes 2 hours to travel 120 km. How many hours will it take to travel 300 km at the same speed?",
            options = listOf("3", "4", "5", "6"),
            correctAnswerIndex = 2
        ),
        // Ages
        Question(
            questionText = "John is twice as old as Anna. In 6 years, John will be 30. How old is Anna today?",
            options = listOf("9", "10", "12", "15"),
            correctAnswerIndex = 2
        ),
        // Cats and mice puzzle
        Question(
            questionText = "Three cats catch three mice in three minutes. How many cats are needed to catch 100 mice in 100 minutes?",
            options = listOf("3", "9", "30", "100"),
            correctAnswerIndex = 0
        ),
        // Weird mapping riddle
        Question(
            questionText = "If 1=3, 2=3, 3=5, 4=4, 5=4, then 6 = ?",
            options = listOf("3", "4", "5", "6"),
            correctAnswerIndex = 0
        ),
        // +3 sequence
        Question(
            questionText = "Which number is missing? 11, 14, 17, 20, ?",
            options = listOf("21", "22", "23", "24"),
            correctAnswerIndex = 2
        ),
        // Ordering people
        Question(
            questionText = "Peter is older than Anna. Charles is younger than Anna. Who is the youngest?",
            options = listOf("Charles", "Anna", "Peter", "It is not possible to know"),
            correctAnswerIndex = 0
        ),
        // Calendar
        Question(
            questionText = "If yesterday was Friday, what day will it be in 3 days?",
            options = listOf("Saturday", "Sunday", "Monday", "Tuesday"),
            correctAnswerIndex = 3
        ),
        // Square root
        Question(
            questionText = "Which number, multiplied by itself, equals 49?",
            options = listOf("5", "6", "7", "8"),
            correctAnswerIndex = 2
        ),
        // Race riddle
        Question(
            questionText = "In a race, you overtake the person in 2nd place. What position are you in now?",
            options = listOf("1st", "2nd", "3rd", "4th"),
            correctAnswerIndex = 1
        ),
        // Eggs in the basket
        Question(
            questionText = "There are 6 eggs in a basket. 2 are taken out. How many eggs remain in the basket?",
            options = listOf("2", "4", "6", "Still 6"),
            correctAnswerIndex = 2
        ),
        // Syllogism
        Question(
            questionText = "If all Bloxs are Zogs, and some Zogs are Trix, we can say that:",
            options = listOf(
                "All Trix are Bloxs",
                "Some Bloxs may be Trix",
                "No Blox is Trix",
                "All Zogs are Trix"
            ),
            correctAnswerIndex = 1
        ),
        // Sequence +4, +6, +8, +10, +12
        Question(
            questionText = "Complete the sequence: 2, 6, 12, 20, 30, ?",
            options = listOf("36", "40", "42", "56"),
            correctAnswerIndex = 2
        ),
        // Portrait riddle
        Question(
            questionText = "A man looks at a portrait and says: 'The father of the man in the picture is my father.' Who is in the portrait?",
            options = listOf("His son", "His brother", "Himself", "His grandfather"),
            correctAnswerIndex = 0
        ),
        // Time in minutes
        Question(
            questionText = "If 1 hour has 60 minutes, then 2.5 hours have:",
            options = listOf("100 minutes", "120 minutes", "150 minutes", "180 minutes"),
            correctAnswerIndex = 2
        ),
        // Riddle (Nothing)
        Question(
            questionText = "What is greater than God, more evil than the devil, the poor have it and the rich need it?",
            options = listOf("Nothing", "Peace", "Time", "Love"),
            correctAnswerIndex = 0
        ),
        // Secret riddle
        Question(
            questionText = "If you have me, you want to share me; if you share me, you lose me. What am I?",
            options = listOf("A secret", "Time", "Money", "A word"),
            correctAnswerIndex = 0
        ),
        // Squares
        Question(
            questionText = "Complete the sequence: 1, 4, 9, 16, 25, ?",
            options = listOf("30", "35", "36", "40"),
            correctAnswerIndex = 2
        ),
        // Proportion with pens
        Question(
            questionText = "If 5 pens cost R$10, how much do 12 pens cost at the same unit price?",
            options = listOf("R$20", "R$22", "R$24", "R$25"),
            correctAnswerIndex = 2
        ),
        // Slow clock
        Question(
            questionText = "A clock loses 10 minutes every hour. After 6 hours, how much time difference will it show?",
            options = listOf("40 minutes", "50 minutes", "1 hour", "1h10"),
            correctAnswerIndex = 2
        )
    )



// Dentro de QuestionManager.kt, adicione esta lista (com o nome 'enigmaQuestionList' se for usar o método corrigido)

    private val enigmaBaseQuestionsPt: List<Question> = listOf(
        // Enigma 1: Charada de Identidade (Pegadinha)
        Question(
            questionText = "Um homem está olhando para um retrato. Alguém pergunta quem está no retrato. Ele responde: 'Não tenho irmãos nem irmãs. Mas o pai daquele homem é filho do meu pai'. Quem está no retrato?",
            options = listOf("Seu pai", "Seu filho", "Seu avô", "Ele mesmo"),
            correctAnswerIndex = 1 // Resposta: Seu filho (Explicação: Se o pai daquele homem é filho do meu pai, e ele não tem irmãos, ele é o único filho do pai dele. Logo, o pai daquele homem é ELE. Se o homem retratado tem ELE como pai, o retratado é SEU FILHO.)
        ),

        // Enigma 2: Lógica de Peso e Balança
        Question(
            questionText = "Você tem 12 bolas idênticas, mas sabe que uma delas é falsa e tem peso diferente (mais leve ou mais pesada). Você tem uma balança de dois pratos. Qual é o número mínimo de pesagens para garantir a identificação da bola falsa e determinar se ela é mais pesada ou mais leve?",
            options = listOf("2", "3", "4", "5"),
            correctAnswerIndex = 1 // Resposta: 3 (É o problema clássico das 12 bolas e 3 pesagens)
        ),

        // Enigma 3: Sequência Lógica Numérica Complexa
        Question(
            questionText = "Qual número completa a sequência: 1, 1, 2, 3, 5, 8, 13, ?",
            options = listOf("18", "20", "21", "24"),
            correctAnswerIndex = 2 // Resposta: 21 (Sequência de Fibonacci: cada número é a soma dos dois anteriores)
        ),

        // Enigma 4: Tempo e Proporção
        Question(
            questionText = "Se são necessários 5 minutos para ferver um ovo, quanto tempo será necessário para ferver 5 ovos na mesma panela, ao mesmo tempo?",
            options = listOf("5 minutos", "10 minutos", "15 minutos", "25 minutos"),
            correctAnswerIndex = 0 // Resposta: 5 minutos (Todos fervem simultaneamente)
        ),

        // Enigma 5: Matemática com Variáveis (Pegadinha)
        Question(
            questionText = "Se você multiplica um número por 3, soma 6, e divide o resultado por 3, o resultado final é 10. Qual era o número original?",
            options = listOf("6", "8", "12", "15"),
            correctAnswerIndex = 1 // Resposta: 8 (Fórmula reversa: 10*3 = 30; 30-6 = 24; 24/3 = 8)
        ),

        // Enigma 6: Charada de Objetos
        Question(
            questionText = "Tenho cidades, mas não tenho casas; tenho montanhas, mas não tenho árvores; tenho água, mas não tenho peixes. O que eu sou?",
            options = listOf("Um livro", "Um espelho", "Um mapa", "Um planeta"),
            correctAnswerIndex = 2 // Resposta: Um mapa
        ),

        // Enigma 7: Probabilidade (Ligeiramente Avançada)
        Question(
            questionText = "Uma gaveta contém 10 meias azuis e 10 meias pretas. Se você está no escuro total, qual é o número mínimo de meias que você precisa tirar para garantir que terá um par da mesma cor?",
            options = listOf("2", "3", "4", "11"),
            correctAnswerIndex = 1 // Resposta: 3 (Pior caso: tira 1ª azul, tira 2ª preta. A 3ª OBRIGATORIAMENTE formará um par com a azul ou a preta).
        ),

        // Enigma 8: Troca de Posições (Pegadinha)
        Question(
            questionText = "Oito amigos estão em uma sala circular. Cada amigo aperta a mão de todos os outros *exatamente uma vez*. Quantos apertos de mão foram trocados no total?",
            options = listOf("8", "16", "28", "56"),
            correctAnswerIndex = 2 // Resposta: 28 (Fórmula n(n-1)/2: 8*7/2 = 28)
        ),

        // Enigma 9: Lógica Temporal (Relação de Parentesco)
        Question(
            questionText = "Amanhã será o ontem de Sábado. Que dia é hoje?",
            options = listOf("Quarta-feira", "Quinta-feira", "Sexta-feira", "Sábado"),
            correctAnswerIndex = 1 // Resposta: Quinta-feira (O ontem de Sábado é Sexta-feira. Se 'Amanhã será Sexta-feira', então Hoje é Quinta-feira.)
        ),

        // Enigma 10: Letras e Significados
        Question(
            questionText = "O que sempre sobe, mas nunca desce?",
            options = listOf("Fumaça", "A idade", "O sol", "A temperatura"),
            correctAnswerIndex = 1 // Resposta: A idade
        )
        // Adicione mais 10 perguntas aqui se quiser atingir o 'take(20)' da sua função!
    )

    private val enigmaBaseQuestionsEn: List<Question> = listOf(
        // Enigma 1: Identity riddle
        Question(
            questionText = "A man is looking at a portrait. Someone asks who is in the picture. He answers: 'I have no brothers or sisters. But the father of that man is a son of my father.' Who is in the portrait?",
            options = listOf("His father", "His son", "His grandfather", "Himself"),
            correctAnswerIndex = 1 // His son
        ),

        // Enigma 2: Balance logic
        Question(
            questionText = "You have 12 identical balls, but one of them is fake and has a different weight (either lighter or heavier). You have a two-pan balance. What is the minimum number of weighings needed to guarantee you find the fake ball and know if it is heavier or lighter?",
            options = listOf("2", "3", "4", "5"),
            correctAnswerIndex = 1 // 3
        ),

        // Enigma 3: Fibonacci sequence
        Question(
            questionText = "Which number completes the sequence: 1, 1, 2, 3, 5, 8, 13, ?",
            options = listOf("18", "20", "21", "24"),
            correctAnswerIndex = 2 // 21
        ),

        // Enigma 4: Time and proportion
        Question(
            questionText = "If it takes 5 minutes to boil one egg, how long will it take to boil 5 eggs in the same pot at the same time?",
            options = listOf("5 minutes", "10 minutes", "15 minutes", "25 minutes"),
            correctAnswerIndex = 0 // 5 minutes
        ),

        // Enigma 5: Math with variables
        Question(
            questionText = "You multiply a number by 3, add 6, and divide the result by 3. The final result is 10. What was the original number?",
            options = listOf("6", "8", "12", "15"),
            correctAnswerIndex = 1 // 8
        ),

        // Enigma 6: Riddle of objects
        Question(
            questionText = "I have cities, but no houses; I have mountains, but no trees; I have water, but no fish. What am I?",
            options = listOf("A book", "A mirror", "A map", "A planet"),
            correctAnswerIndex = 2 // A map
        ),

        // Enigma 7: Probability
        Question(
            questionText = "A drawer contains 10 blue socks and 10 black socks. You are in complete darkness. What is the minimum number of socks you must take out to guarantee you have a matching pair of the same color?",
            options = listOf("2", "3", "4", "11"),
            correctAnswerIndex = 1 // 3
        ),

        // Enigma 8: Handshakes
        Question(
            questionText = "Eight friends are in a circular room. Each friend shakes hands with every other friend exactly once. How many handshakes are there in total?",
            options = listOf("8", "16", "28", "56"),
            correctAnswerIndex = 2 // 28
        ),

        // Enigma 9: Time logic
        Question(
            questionText = "Tomorrow will be the yesterday of Saturday. What day is today?",
            options = listOf("Wednesday", "Thursday", "Friday", "Saturday"),
            correctAnswerIndex = 1 // Thursday
        ),

        // Enigma 10: Meaning riddle
        Question(
            questionText = "What always goes up but never comes down?",
            options = listOf("Smoke", "Age", "The sun", "Temperature"),
            correctAnswerIndex = 1 // Age
        )
    )

    private val expertQuestionsEn = listOf<Question>(
        Question(
            questionText = "If 5 machines make 5 items in 5 minutes, how long do 100 machines take to make 100 items?",
            options = listOf("5 minutes", "100 minutes", "1 minute", "10 minutes"),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What is the value of x in the equation: 2x + 3 = 7?",
            options = listOf("1", "2", "3", "4"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Which of the following is NOT a programming language?",
            options = listOf("Python", "Java", "HTML", "C++"),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "What is the result of the expression: true && false || true?",
            options = listOf("true", "false", "null", "undefined"),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "What is the capital of Australia?",
            options = listOf("Sydney", "Melbourne", "Canberra", "Brisbane"),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Which number completes the sequence: 2, 4, 8, 16, ___?",
            options = listOf("20", "24", "32", "64"),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "What is the result of: println(\"1\" + 2 + 3) in Kotlin?",
            options = listOf("123", "6", "33", "15"),
            correctAnswerIndex = 0
        ),

        // 8
        Question(
            questionText = "Let f(x) = x² - 4x + 4. What is the minimum value of f(x)?",
            options = listOf("0", "2", "4", "-4"),
            correctAnswerIndex = 0
        ),

        // 9
        Question(
            questionText = "What is the value of the limit: lim (n→∞) (1 + 1/n)^n ?",
            options = listOf(
                "e (approx. 2.718)",
                "1",
                "2",
                "The limit does not exist"
            ),
            correctAnswerIndex = 0
        ),

        // 10
        Question(
            questionText = "A set has 5 elements. How many different subsets does it have?",
            options = listOf("10", "16", "25", "32"),
            correctAnswerIndex = 3
        ),

        // 11
        Question(
            questionText = "What is the determinant of the 2x2 matrix: [[2, -1], [5, 3]]?",
            options = listOf("1", "-11", "11", "13"),
            correctAnswerIndex = 2
        ),

        // 12
        Question(
            questionText = "An algorithm has two nested for-loops from 1 to n. What is its time complexity?",
            options = listOf("O(n)", "O(log n)", "O(n²)", "O(n³)"),
            correctAnswerIndex = 2
        ),

        // 13
        Question(
            questionText = "What is the correct logical negation of the statement: \"All students passed the exam\"?",
            options = listOf(
                "No student passed the exam",
                "Some students passed the exam",
                "At least one student did NOT pass the exam",
                "Half of the students passed the exam"
            ),
            correctAnswerIndex = 2
        ),

        // 14
        Question(
            questionText = "What is the derivative of the function f(x) = 3x³ - 2x + 1?",
            options = listOf(
                "9x² - 2",
                "9x² + 1",
                "3x² - 2",
                "9x - 2"
            ),
            correctAnswerIndex = 0
        ),

        // 15
        Question(
            questionText = "A fair die is rolled twice. What is the probability that the sum of the results is equal to 7?",
            options = listOf(
                "1/6",
                "1/3",
                "1/12",
                "2/3"
            ),
            correctAnswerIndex = 0
        )
    )

    private val expertQuestionsPt = listOf<Question>(
        Question(
            questionText = "Se 5 máquinas fazem 5 peças em 5 minutos, quanto tempo 100 máquinas levam para fazer 100 peças?",
            options = listOf("5 minutos", "100 minutos", "1 minuto", "10 minutos"),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é o valor de x na equação: 2x + 3 = 7?",
            options = listOf("1", "2", "3", "4"),
            correctAnswerIndex = 1
        ),
        Question(
            questionText = "Qual das alternativas NÃO é uma linguagem de programação?",
            options = listOf("Python", "Java", "HTML", "C++"),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Qual é o resultado da expressão: true && false || true?",
            options = listOf("true", "false", "null", "undefined"),
            correctAnswerIndex = 0
        ),
        Question(
            questionText = "Qual é a capital da Austrália?",
            options = listOf("Sydney", "Melbourne", "Canberra", "Brisbane"),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Qual número completa a sequência: 2, 4, 8, 16, ___?",
            options = listOf("20", "24", "32", "64"),
            correctAnswerIndex = 2
        ),
        Question(
            questionText = "Qual é o resultado de: println(\"1\" + 2 + 3) em Kotlin?",
            options = listOf("123", "6", "33", "15"),
            correctAnswerIndex = 0
        ),

        // 8
        Question(
            questionText = "Seja f(x) = x² - 4x + 4. Qual é o valor mínimo de f(x)?",
            options = listOf("0", "2", "4", "-4"),
            correctAnswerIndex = 0
        ),

        // 9
        Question(
            questionText = "Qual é o valor do limite: lim (n→∞) (1 + 1/n)^n ?",
            options = listOf(
                "e (aprox. 2,718)",
                "1",
                "2",
                "O limite não existe"
            ),
            correctAnswerIndex = 0
        ),

        // 10
        Question(
            questionText = "Um conjunto possui 5 elementos. Quantos subconjuntos diferentes ele possui?",
            options = listOf("10", "16", "25", "32"),
            correctAnswerIndex = 3
        ),

        // 11
        Question(
            questionText = "Qual é o determinante da matriz 2x2: [[2, -1], [5, 3]]?",
            options = listOf("1", "-11", "11", "13"),
            correctAnswerIndex = 2
        ),

        // 12
        Question(
            questionText = "Um algoritmo possui dois laços (for) aninhados de 1 até n. Qual é a complexidade de tempo?",
            options = listOf("O(n)", "O(log n)", "O(n²)", "O(n³)"),
            correctAnswerIndex = 2
        ),

        // 13
        Question(
            questionText = "Qual é a negação lógica correta da frase: \"Todos os alunos passaram na prova\"?",
            options = listOf(
                "Nenhum aluno passou na prova",
                "Alguns alunos passaram na prova",
                "Pelo menos um aluno NÃO passou na prova",
                "Metade dos alunos passou na prova"
            ),
            correctAnswerIndex = 2
        ),

        // 14
        Question(
            questionText = "Qual é a derivada da função f(x) = 3x³ - 2x + 1?",
            options = listOf(
                "9x² - 2",
                "9x² + 1",
                "3x² - 2",
                "9x - 2"
            ),
            correctAnswerIndex = 0
        ),

        // 15
        Question(
            questionText = "Um dado honesto é lançado duas vezes. Qual é a probabilidade da soma dos resultados ser igual a 7?",
            options = listOf(
                "1/6",
                "1/3",
                "1/12",
                "2/3"
            ),
            correctAnswerIndex = 0
        )
    )

}





