package com.example.flowershopapi.iroha

import java.net.URL
import java.util.UUID

data class IrohaConfig(
    // 10.0.2.2 est l'adresse pour accéder au localhost (Docker) depuis l'émulateur Android
    val apiUrl: URL = URL("http://10.0.2.2:8080"),
    val chainId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000"),

    // Domaine par défaut défini dans ton genesis
    val adminDomain: String = "wonderland",

    // Clés brutes SANS les préfixes multihash (ed0120 et 802620) pour le SDK Java
    val adminPublicKey: String = "32F0C017884729DD1E4F0DE3359F53444AB03FA18D64C3A31C02D2787D5B9CBA",
    val adminPrivateKey: String = "76D6468D63CD0FD0F40BBCB495DA09620BA6F1148CD7A8BFDD075F0F787D7A18"
)