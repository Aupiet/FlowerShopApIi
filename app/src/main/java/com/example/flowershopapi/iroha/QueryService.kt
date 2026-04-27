package com.example.flowershopapi.iroha

import jp.co.soramitsu.iroha2.generated.AccountId
import jp.co.soramitsu.iroha2.generated.AccountIdPredicateAtom
import jp.co.soramitsu.iroha2.generated.AccountIdProjectionOfPredicateMarker
import jp.co.soramitsu.iroha2.generated.AssetDefinitionId
import jp.co.soramitsu.iroha2.generated.AssetIdProjectionOfPredicateMarker
import jp.co.soramitsu.iroha2.generated.AssetProjectionOfPredicateMarker
import jp.co.soramitsu.iroha2.generated.AssetValue
import jp.co.soramitsu.iroha2.generated.CompoundPredicateOfAccount
import jp.co.soramitsu.iroha2.generated.CompoundPredicateOfAsset
import jp.co.soramitsu.iroha2.generated.CompoundPredicateOfDomain
import jp.co.soramitsu.iroha2.query.QueryBuilder
import jp.co.soramitsu.iroha2.cast
import java.math.BigInteger

class QueryService(private val irohaClient: IrohaClient) {

    suspend fun findAllDomains(filter: CompoundPredicateOfDomain? = null) =
        irohaClient.client.submit(   // submit pas sendQuery
            QueryBuilder
                .findDomains(filter)
                .signAs(irohaClient.adminAccountId, irohaClient.adminKeyPair)
        )

    suspend fun findAllAccounts(filter: CompoundPredicateOfAccount? = null) =
        irohaClient.client.submit(
            QueryBuilder
                .findAccounts(filter)
                .signAs(irohaClient.adminAccountId, irohaClient.adminKeyPair)
        )

    suspend fun findAllAssets(filter: CompoundPredicateOfAsset? = null) =
        irohaClient.client.submit(
            QueryBuilder
                .findAssets(filter)
                .signAs(irohaClient.adminAccountId, irohaClient.adminKeyPair)
        )

    suspend fun getAccountAmount(
        accountId: AccountId,
        assetDefinitionId: AssetDefinitionId,
    ): BigInteger {
        val filter = CompoundPredicateOfAsset.Atom(
            AssetProjectionOfPredicateMarker.Id(
                AssetIdProjectionOfPredicateMarker.Account(
                    AccountIdProjectionOfPredicateMarker.Atom(
                        AccountIdPredicateAtom.Equals(accountId)
                    )
                )
            )
        )
        return irohaClient.client
            .submit(QueryBuilder.findAssets(filter).signAs(irohaClient.adminAccountId, irohaClient.adminKeyPair))
            .find { it.id.definition == assetDefinitionId }?.value
            ?.cast<AssetValue.Numeric>()?.numeric?.mantissa
            ?: throw RuntimeException("NOT FOUND")
    }
}