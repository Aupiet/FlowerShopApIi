package com.example.flowershopapi.utils

import jp.co.soramitsu.iroha2.keyPairFromHex
import java.security.KeyPair

object KeyUtils {
    /**
     * Crée un KeyPair à partir de chaînes hexadécimales.
     * Retire les préfixes Multiformats (ed0120, 802620) s'ils sont présents.
     */
    fun keyPairFromHex(publicKeyHex: String, privateKeyHex: String): KeyPair {
        val cleanPublic = publicKeyHex.removePrefix("ed0120")
        val cleanPrivate = privateKeyHex.removePrefix("802620")
        
        return keyPairFromHex(cleanPublic, cleanPrivate)
    }
}
