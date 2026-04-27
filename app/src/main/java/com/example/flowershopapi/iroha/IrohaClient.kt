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

class IrohaClient(private val config: IrohaConfig) {

    private lateinit var _client: AdminIroha2Client
    private lateinit var _admin: AccountId
    private lateinit var _keyPair: java.security.KeyPair

    suspend fun init() {
        if (::_client.isInitialized) return
        withContext(Dispatchers.IO) {
            _keyPair = keyPairFromHex(config.adminPublicKey, config.adminPrivateKey)

            _admin = AccountId(
                config.adminDomain.asDomainId(),
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

    suspend fun getClient(): AdminIroha2Client {
        init()
        return _client
    }

    fun getAdmin(): AccountId = _admin
    fun getKeyPair(): java.security.KeyPair = _keyPair
}