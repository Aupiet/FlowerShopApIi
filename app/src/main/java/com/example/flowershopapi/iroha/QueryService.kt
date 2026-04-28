package com.example.flowershopapi.iroha

import jp.co.soramitsu.iroha2.cast
import jp.co.soramitsu.iroha2.generated.Account
import jp.co.soramitsu.iroha2.generated.AccountId
import jp.co.soramitsu.iroha2.generated.AccountIdPredicateAtom
import jp.co.soramitsu.iroha2.generated.AccountIdProjectionOfPredicateMarker
import jp.co.soramitsu.iroha2.generated.Asset
import jp.co.soramitsu.iroha2.generated.AssetDefinitionId
import jp.co.soramitsu.iroha2.generated.AssetIdProjectionOfPredicateMarker
import jp.co.soramitsu.iroha2.generated.AssetProjectionOfPredicateMarker
import jp.co.soramitsu.iroha2.generated.AssetValue
import jp.co.soramitsu.iroha2.generated.CompoundPredicateOfAccount
import jp.co.soramitsu.iroha2.generated.CompoundPredicateOfAsset
import jp.co.soramitsu.iroha2.generated.CompoundPredicateOfDomain
import jp.co.soramitsu.iroha2.generated.Domain
import jp.co.soramitsu.iroha2.query.QueryBuilder
import java.math.BigInteger

class QueryService(private val irohaClient: IrohaClient) {

    suspend fun findAllDomains(filter: CompoundPredicateOfDomain? = null): List<Domain> {
        val client = irohaClient.getClient()
        val admin = irohaClient.getAdmin()
        val keyPair = irohaClient.getKeyPair()
        return client.submit(
            QueryBuilder
                .findDomains(filter)
                .signAs(admin, keyPair),
        )
    }

    suspend fun findAllAccounts(filter: CompoundPredicateOfAccount? = null): List<Account> {
        val client = irohaClient.getClient()
        val admin = irohaClient.getAdmin()
        val keyPair = irohaClient.getKeyPair()
        return client.submit(
            QueryBuilder
                .findAccounts(filter)
                .signAs(admin, keyPair),
        )
    }

    suspend fun findAllAssets(filter: CompoundPredicateOfAsset? = null): List<Asset> {
        val client = irohaClient.getClient()
        val admin = irohaClient.getAdmin()
        val keyPair = irohaClient.getKeyPair()
        return client.submit(
            QueryBuilder
                .findAssets(filter)
                .signAs(admin, keyPair),
        )
    }

    suspend fun getAccountAmount(
        accountId: AccountId,
        assetDefinitionId: AssetDefinitionId,
    ): BigInteger {
        val client = irohaClient.getClient()
        val admin = irohaClient.getAdmin()
        val keyPair = irohaClient.getKeyPair()

        val byAccountIdFilter =
            CompoundPredicateOfAsset.Atom(
                AssetProjectionOfPredicateMarker.Id(
                    AssetIdProjectionOfPredicateMarker.Account(
                        AccountIdProjectionOfPredicateMarker.Atom(
                            AccountIdPredicateAtom.Equals(accountId),
                        ),
                    ),
                ),
            )
        return client
            .submit(QueryBuilder.findAssets(byAccountIdFilter).signAs(admin, keyPair))
            .let { assets ->
                assets.find { it.id.definition == assetDefinitionId }?.value
            }?.let { value ->
                value.cast<AssetValue.Numeric>().numeric.mantissa
            } ?: throw RuntimeException("Asset not found for account $accountId")
    }
}
