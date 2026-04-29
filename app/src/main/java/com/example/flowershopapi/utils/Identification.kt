package com.example.flowershopapi.utils

import com.example.flowershopapi.iroha.IrohaClient
import com.example.flowershopapi.iroha.IrohaConfig
import com.example.flowershopapi.iroha.TransactionService
import jp.co.soramitsu.iroha2.keyPairFromHex
import jp.co.soramitsu.iroha2.toIrohaPublicKey
import jp.co.soramitsu.iroha2.generated.AccountId
import jp.co.soramitsu.iroha2.asDomainId
import jp.co.soramitsu.iroha2.asString
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.util.encoders.Hex
import java.security.KeyPair
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class Identification(
    private val irohaClient: IrohaClient,
    private val transactionService: TransactionService
) {

    /**
     * Génère la paire de clés brute (hex) à partir du pseudo et mot de passe.
     * Retourne une paire (publicKeyHex, privateKeyHex) sans préfixe.
     */
    private fun deriveRawKeys(username: String, password: String): Pair<String, String> {
        val salt = username.toByteArray()
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val seed = factory.generateSecret(spec).encoded // 32 bytes

        val privKeyParams = Ed25519PrivateKeyParameters(seed, 0)
        val pubKeyParams = privKeyParams.generatePublicKey()

        val pubHex = Hex.toHexString(pubKeyParams.encoded)   // 64 chars
        val privHex = Hex.toHexString(privKeyParams.encoded) // 64 chars
        return pubHex to privHex
    }

    /**
     * Dérive un KeyPair Iroha à partir du pseudo et mot de passe.
     * Utilise les clés brutes (sans préfixe Multihash).
     */
    fun deriveKeyPair(username: String, password: String): KeyPair {
        val (pubHex, privHex) = deriveRawKeys(username, password)
        return keyPairFromHex(pubHex, privHex)
    }

    /**
     * Connecte l'utilisateur en mettant à jour la config avec ses clés dérivées (brutes).
     */
    suspend fun connexion(username: String, password: String, domain: String) {
        val (pubHex, privHex) = deriveRawKeys(username, password)
        val userConfig = IrohaConfig().copy(
            adminDomain = domain,
            adminPublicKey = pubHex,   // clé brute, pas de "ed0120"
            adminPrivateKey = privHex  // clé brute, pas de "802620"
        )

        irohaClient.updateConfig(userConfig)
    }

    /**
     * Réinitialise la config sur le compte registrar par défaut.
     */
    suspend fun deconnexion() {
        irohaClient.updateConfig(IrohaConfig())
    }

    /**
     * Crée le compte sur la blockchain dans un domaine spécifique.
     */
    suspend fun registerAccountInDomain(username: String, password: String, domain: String) {
        val keys = deriveKeyPair(username, password)
        val accountId = AccountId(
            domain.asDomainId(),
            keys.public.toIrohaPublicKey()
        )
        transactionService.registerAccount(
            id = accountId.asString()
        )
    }
}
