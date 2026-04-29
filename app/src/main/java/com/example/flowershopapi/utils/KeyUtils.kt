package com.example.flowershopapi.utils

import jp.co.soramitsu.iroha2.keyPairFromHex
import java.security.KeyPair

object KeyUtils {
    /**
     * Retire les préfixes Multiformats (ed0120) de la clé publique.
     */
    fun cleanPublicKey(hex: String): String = hex.removePrefix("ed0120")

    /**
     * Retire les préfixes Multiformats (802620) de la clé privée.
     */
    fun cleanPrivateKey(hex: String): String = hex.removePrefix("802620")

    /**
     * Crée un KeyPair à partir de chaînes hexadécimales.
     */
    fun keyPairFromHex(publicKeyHex: String, privateKeyHex: String): KeyPair {
        return keyPairFromHex(cleanPublicKey(publicKeyHex), cleanPrivateKey(privateKeyHex))
    }
}
