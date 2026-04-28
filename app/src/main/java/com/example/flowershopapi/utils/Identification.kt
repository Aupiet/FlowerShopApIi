package com.example.flowershopapi.utils

import com.example.flowershopapi.iroha.IrohaClient
import com.example.flowershopapi.iroha.IrohaConfig
import com.example.flowershopapi.iroha.TransactionService
import jp.co.soramitsu.iroha2.keyPairFromHex
import jp.co.soramitsu.iroha2.toHex
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
     * Dérive un KeyPair Iroha à partir du pseudo et mot de passe.
     */
    fun deriveKeyPair(username: String, password: String): KeyPair {
        val salt = username.toByteArray()
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val seed = factory.generateSecret(spec).encoded // 32 bytes

        val privKeyParams = Ed25519PrivateKeyParameters(seed, 0)
        val pubKeyParams = privKeyParams.generatePublicKey()

        val privHex = Hex.toHexString(privKeyParams.encoded)
        val pubHex = Hex.toHexString(pubKeyParams.encoded)

        return keyPairFromHex(privHex, pubHex)
    }

    /**
     * Connecte l'utilisateur en mettant à jour la config avec ses clés dérivées.
     */
    suspend fun connexion(username: String, password: String) {
        val keys = deriveKeyPair(username, password)

        // ✅ .encoded.toHex() — ByteArray.toHex() existe, pas PublicKey.toHex()
        val pubHex = keys.public.encoded.toHex()
        val privHex = keys.private.encoded.toHex()

        val userConfig = IrohaConfig().copy(
            adminDomain = "customer",
            adminPublicKey = pubHex,
            adminPrivateKey = privHex
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
     * Crée le compte customer sur la blockchain.
     * La clé publique est intégrée dans l'AccountId, pas en paramètre séparé.
     */
    suspend fun registerCustomerAccount(username: String, password: String) {
        val keys = deriveKeyPair(username, password)

        // ✅ Pattern de Main.kt : clé publique dans l'AccountId
        val accountId = AccountId(
            "customer".asDomainId(),
            keys.public.toIrohaPublicKey()
        )

        transactionService.registerAccount(
            id = accountId.asString()
            // ← pas de signatories, la clé est dans l'ID
        )
    }
}