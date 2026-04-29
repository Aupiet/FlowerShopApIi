package com.example.flowershopapi.iroha

import com.example.flowershopapi.BuildConfig
import com.example.flowershopapi.utils.KeyUtils
import java.net.URL
import java.util.UUID

data class IrohaConfig(
    val apiUrl: URL = URL("http://192.168.1.68:8080"),
    val chainId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    // Compte WarehouseAccount — admin avec CanRegisterDomain + CanSetParameters
    val adminDomain: String = "customer",
    val adminPublicKey: String  = KeyUtils.cleanPublicKey(BuildConfig.REGISTRAR_PUBLIC_KEY),
    val adminPrivateKey: String = KeyUtils.cleanPrivateKey(BuildConfig.REGISTRAR_PRIVATE_KEY),
)
