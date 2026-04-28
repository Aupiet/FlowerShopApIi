package com.example.flowershopapi.iroha

import jp.co.soramitsu.iroha2.asAccountId
import jp.co.soramitsu.iroha2.asAssetDefinitionId
import jp.co.soramitsu.iroha2.asDomainId
import jp.co.soramitsu.iroha2.generated.AssetId
import jp.co.soramitsu.iroha2.generated.AssetType
import jp.co.soramitsu.iroha2.generated.AssetValue
import jp.co.soramitsu.iroha2.generated.Json
import jp.co.soramitsu.iroha2.generated.Metadata
import jp.co.soramitsu.iroha2.generated.Mintable
import jp.co.soramitsu.iroha2.generated.Name
import jp.co.soramitsu.iroha2.generated.NumericSpec
import jp.co.soramitsu.iroha2.generated.Permission
import jp.co.soramitsu.iroha2.transaction.Burn
import jp.co.soramitsu.iroha2.transaction.Mint
import jp.co.soramitsu.iroha2.transaction.Register
import jp.co.soramitsu.iroha2.transaction.Unregister
import jp.co.soramitsu.iroha2.transaction.Transfer
import jp.co.soramitsu.iroha2.transaction.Grant
import kotlinx.coroutines.withTimeout
import java.math.BigDecimal

class TransactionService(
    private val irohaClient: IrohaClient,
    private val timeout: Long = 10_000
) {
    suspend fun registerDomain(
        id: String,
        metadata: Map<Name, Json> = mapOf(),
    ) {
        val client = irohaClient.getClient()
        client.submit(Register.domain(id.asDomainId(), metadata)).also {
            withTimeout(timeout) { it.await() }
        }
    }

    suspend fun unregisterDomain(id: String) {
        val client = irohaClient.getClient()
        client.submit(Unregister.domain(id.asDomainId())).also {
            withTimeout(timeout) { it.await() }
        }
    }

    suspend fun registerAccount(
        id: String,
        metadata: Map<Name, Json> = mapOf(),
    ) {
        val client = irohaClient.getClient()
        client.submit(Register.account(id.asAccountId(), metadata = Metadata(metadata))).also {
            withTimeout(timeout) { it.await() }
        }
    }

    suspend fun registerCustomer(accountIdStr: String) {
        val client = irohaClient.getClient()
        val accountId = accountIdStr.asAccountId()

        // On enregistre le compte et on lui donne la permission de se supprimer lui-même
        // Note: Dans Iroha 2, on peut soumettre plusieurs instructions
        client.submit(
            Register.account(accountId),

        ).also {
            withTimeout(timeout) { it.await() }
        }
    }

    suspend fun unregisterAccount(id: String) {
        val client = irohaClient.getClient()
        client.submit(Unregister.account(id.asAccountId())).also {
            withTimeout(timeout) { it.await() }
        }
    }

    suspend fun registerAssetDefinition(
        id: String,
        type: AssetType = AssetType.Numeric(NumericSpec(0L)),
        metadata: Map<Name, Json> = mapOf(),
        mintable: Mintable = Mintable.Infinitely(),
    ) {
        val client = irohaClient.getClient()
        client.submit(
            Register.assetDefinition(
                id.asAssetDefinitionId(),
                type,
                mintable,
                metadata = Metadata(metadata)
            )
        ).also {
            withTimeout(timeout) { it.await() }
        }
    }

    suspend fun registerAsset(
        id: AssetId,
        value: AssetValue,
    ) {
        val client = irohaClient.getClient()
        client.submit(Register.asset(id, value)).also {
            withTimeout(timeout) { it.await() }
        }
    }

    suspend fun unregisterAsset(id: AssetId) {
        val client = irohaClient.getClient()
        client.submit(Unregister.asset(id)).also {
            withTimeout(timeout) { it.await() }
        }
    }

    suspend fun transferAsset(
        from: AssetId,
        value: BigDecimal,
        toAccountId: String,
    ) {
        val client = irohaClient.getClient()
        client.submit(Transfer.asset(from, value, toAccountId.asAccountId())).also {
            withTimeout(timeout) { it.await() }
        }
    }

    suspend fun mintAsset(
        assetId: AssetId,
        value: BigDecimal,
    ) {
        val client = irohaClient.getClient()
        client.submit(Mint.asset(assetId, value)).also {
            withTimeout(timeout) { it.await() }
        }
    }

    suspend fun burnAssets(
        assetId: AssetId,
        value: BigDecimal,
    ) {
        val client = irohaClient.getClient()
        client.submit(Burn.asset(assetId, value)).also {
            withTimeout(timeout) { it.await() }
        }
    }
}
