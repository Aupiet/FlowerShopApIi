package com.example.flowershopapi.iroha

import jp.co.soramitsu.iroha2.AdminIroha2Client
import jp.co.soramitsu.iroha2.asDomainId
import jp.co.soramitsu.iroha2.generated.AccountId
import jp.co.soramitsu.iroha2.keyPairFromHex
import jp.co.soramitsu.iroha2.publicKeyFromHex
import jp.co.soramitsu.iroha2.toIrohaPublicKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.security.KeyPair

class IrohaClient(private var config: IrohaConfig) {

    private var _client: AdminIroha2Client? = null
    private lateinit var _admin: AccountId
    private lateinit var _keyPair: KeyPair

    suspend fun init() {
        if (_client != null) return
        forceInit()
    }

    private suspend fun forceInit() {
        withContext(Dispatchers.IO) {
            _keyPair = keyPairFromHex(config.adminPublicKey, config.adminPrivateKey)

            val domain = config.adminDomain.asDomainId()
            _admin = AccountId(
                domain,
                publicKeyFromHex(config.adminPublicKey).toIrohaPublicKey()
            )

            _client = AdminIroha2Client(
                listOf(URI(config.apiUrl.toString()).toURL()),
                config.chainId,
                _admin,
                _keyPair
            )
        }
    }

    /**
     * Met à jour la configuration et réinitialise le client.
     */
    suspend fun updateConfig(newConfig: IrohaConfig) {
        config = newConfig
        _client = null // Force la réinitialisation au prochain init()
        forceInit()
    }

    suspend fun getClient(): AdminIroha2Client {
        if (_client == null) init()
        return _client!!
    }

    fun getAdmin(): AccountId = _admin
    fun getKeyPair(): KeyPair = _keyPair
}
