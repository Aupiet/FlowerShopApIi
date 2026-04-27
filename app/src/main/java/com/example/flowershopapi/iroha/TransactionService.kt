package com.example.flowershopapi.iroha

import jp.co.soramitsu.iroha2.asDomainId
import jp.co.soramitsu.iroha2.transaction.Register
import jp.co.soramitsu.iroha2.generated.Json
import jp.co.soramitsu.iroha2.generated.Name
import kotlinx.coroutines.withTimeout

class TransactionService(
    private val irohaClient: IrohaClient,
    private val timeout: Long = 10_000
) {
    suspend fun registerDomain(id: String, metadata: Map<Name, Json> = mapOf()) {
        val client = irohaClient.getClient()
        client.submit(Register.domain(id.asDomainId(), metadata)).also {
            withTimeout(timeout) { it.await() }
        }
    }
}