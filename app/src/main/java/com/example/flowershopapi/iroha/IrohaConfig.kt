package com.example.flowershopapi.iroha

import java.net.URL
import java.util.UUID

data class IrohaConfig(
    val apiUrl: URL = URL("http://10.0.2.2:8080"), // 10.0.2.2 pour l'hôte depuis l'émulateur
    val chainId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    val adminAccount: String = "ed012032F0C017884729DD1E4F0DE3359F53444AB03FA18D64C3A31C02D2787D5B9CBA",
    val adminDomain: String = "warehouse",
    val adminPublicKey: String = "ed012032F0C017884729DD1E4F0DE3359F53444AB03FA18D64C3A31C02D2787D5B9CBA",
    val adminPrivateKey: String = "80262076D6468D63CD0FD0F40BBCB495DA09620BA6F1148CD7A8BFDD075F0F787D7A18"
)
