package com.example.flowershopapi.iroha

import jp.co.soramitsu.iroha2.client.Iroha2Client
import jp.co.soramitsu.iroha2.generated.AccountId
import jp.co.soramitsu.iroha2.asDomainId
import jp.co.soramitsu.iroha2.keyPairFromHex
import jp.co.soramitsu.iroha2.publicKeyFromHex
import jp.co.soramitsu.iroha2.toIrohaPublicKey
import java.net.URI
import java.security.KeyPair

class IrohaClient(private val config: IrohaConfig) {

    val adminKeyPair: KeyPair by lazy {
        keyPairFromHex(config.adminPublicKey, config.adminPrivateKey)
    }

    val adminAccountId: AccountId by lazy {
        AccountId(
            config.adminDomain.asDomainId(),
            publicKeyFromHex(config.adminPublicKey).toIrohaPublicKey()
        )
    }

    val client: Iroha2Client by lazy {
        Iroha2Client(
            listOf(URI(config.apiUrl.toString()).toURL()),
            config.chainId,
            adminAccountId,
            adminKeyPair
        )
    }
}
