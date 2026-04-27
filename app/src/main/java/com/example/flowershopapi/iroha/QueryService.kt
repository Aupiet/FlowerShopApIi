package com.example.flowershopapi.iroha

import jp.co.soramitsu.iroha2.query.QueryBuilder
import jp.co.soramitsu.iroha2.generated.Domain

class QueryService(private val irohaClient: IrohaClient) {

    suspend fun findAllDomains(): List<Domain> {
        val client = irohaClient.getClient()
        val admin  = irohaClient.getAdmin()
        val kp     = irohaClient.getKeyPair()
        return client.submit(
            QueryBuilder.findDomains().signAs(admin, kp)
        )
    }
}