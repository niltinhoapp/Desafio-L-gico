package com.desafiolgico.model

class QuestionManager {
    //lista de perguntas para nivel iniciante
    private val beginnerQuestions = listOf(

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
            ),
            correctAnswerIndex = 1
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

        // Perguntas anteriores...

        // Continuando com perguntas fáceis de diferentes categorias

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


    // pergunta para nivel intermediario
    private val intermediateQuestions = listOf(

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


        )


    //nivel avançado
    private val advancedQuestions = listOf(

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


    fun getQuestions(level: String): List<Question> {
        return when (level) {
            "Iniciante" -> beginnerQuestions
            "Intermediário" -> intermediateQuestions
            "Avançado" -> advancedQuestions
            else -> listOf()
        }
    }

}
