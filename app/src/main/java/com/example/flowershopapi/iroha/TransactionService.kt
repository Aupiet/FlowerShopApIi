package com.example.flowershopapi.iroha

import jp.co.soramitsu.iroha2.generated.AssetId
import jp.co.soramitsu.iroha2.generated.AssetType
import jp.co.soramitsu.iroha2.generated.AssetValue
import jp.co.soramitsu.iroha2.generated.Json
import jp.co.soramitsu.iroha2.generated.Metadata
import jp.co.soramitsu.iroha2.generated.Mintable
import jp.co.soramitsu.iroha2.generated.Name
import jp.co.soramitsu.iroha2.transaction.Burn
import jp.co.soramitsu.iroha2.transaction.Register
import jp.co.soramitsu.iroha2.transaction.Transfer
import jp.co.soramitsu.iroha2.asDomainId
import jp.co.soramitsu.iroha2.asAccountId
import jp.co.soramitsu.iroha2.asAssetDefinitionId
import kotlinx.coroutines.withTimeout
import java.math.BigDecimal

class TransactionService(
    private val irohaClient: IrohaClient,
    private val timeout: Long = 10000,
) {
    suspend fun registerDomain(id: String, metadata: Map<Name, Json> = mapOf()) {
        irohaClient.client.submit(Register.domain(id.asDomainId(), metadata))
            .also { withTimeout(timeout) { it.await() } }
    }

    suspend fun registerAccount(id: String, metadata: Map<Name, Json> = mapOf()) {
        irohaClient.client.submit(Register.account(id.asAccountId(), Metadata(metadata)))
            .also { withTimeout(timeout) { it.await() } }
    }

    suspend fun registerAssetDefinition(
        id: String,
        type: AssetType = AssetType.Store(),
        metadata: Map<Name, Json> = mapOf(),
        mintable: Mintable = Mintable.Infinitely(),
    ) {
        irohaClient.client.submit(
            Register.assetDefinition(id.asAssetDefinitionId(), type, mintable, metadata = Metadata(metadata))
        ).also { withTimeout(timeout) { it.await() } }
    }

    suspend fun registerAsset(id: AssetId, value: AssetValue) {
        irohaClient.client.submit(Register.asset(id, value))
            .also { withTimeout(timeout) { it.await() } }
    }

    suspend fun transferAsset(from: AssetId, value: BigDecimal, to: String) {
        irohaClient.client.submit(Transfer.asset(from, value, to.asAccountId()))
            .also { withTimeout(timeout) { it.await() } }
    }

    suspend fun burnAssets(assetId: AssetId, value: BigDecimal) {
        irohaClient.client.submit(Burn.asset(assetId, value))
            .also { withTimeout(timeout) { it.await() } }
    }
}