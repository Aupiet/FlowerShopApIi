package com.example.flowershopapi.utils

import com.example.flowershopapi.iroha.IrohaClient
import com.example.flowershopapi.iroha.QueryService
import com.example.flowershopapi.iroha.TransactionService
import java.math.BigDecimal
import jp.co.soramitsu.iroha2.asAccountId
import jp.co.soramitsu.iroha2.asAssetDefinitionId
import jp.co.soramitsu.iroha2.asAssetId
import jp.co.soramitsu.iroha2.asString

class Interaction(
    private val irohaClient: IrohaClient,
    private val transactionService: TransactionService,
    private val queryService: QueryService
) {
    private val moneyAssetDef = "money#shop"

    /**
     * Transfers money#shop from the connected client account to a specific shop account.
     * The blockchain WASM trigger automatically sends a flower back.
     */
    suspend fun buyFlower(shopAccountIdStr: String, quantity: BigDecimal = BigDecimal(10)) {
        val clientAccountId = irohaClient.getAdmin()
        val moneyAssetId = "$moneyAssetDef#${clientAccountId.asString()}".asAssetId()

        // Verify balance before transfer
        val balance = try {
            queryService.getAccountAmount(clientAccountId, moneyAssetDef.asAssetDefinitionId())
        } catch (e: Exception) {
            java.math.BigInteger.ZERO
        }

        if (balance < quantity.toBigInteger()) {
            throw Exception("Insufficient balance: current balance is $balance, required $quantity")
        }

        transactionService.transferAsset(moneyAssetId, quantity, shopAccountIdStr)
    }

    /**
     * Returns the money#shop balance and the number of flowers for the connected client account.
     */
    suspend fun getCustomerBalance(flowerAssetDef: String = "flower#warehouse"): Pair<BigDecimal, BigDecimal> {
        val clientAccountId = irohaClient.getAdmin()
        
        val moneyBalance = try {
            queryService.getAccountAmount(clientAccountId, moneyAssetDef.asAssetDefinitionId()).toBigDecimal()
        } catch (e: Exception) {
            BigDecimal.ZERO
        }

        val flowerBalance = try {
            queryService.getAccountAmount(clientAccountId, flowerAssetDef.asAssetDefinitionId()).toBigDecimal()
        } catch (e: Exception) {
            BigDecimal.ZERO
        }

        return Pair(moneyBalance, flowerBalance)
    }

    /**
     * Returns the number of flowers available in a specific shop.
     */
    suspend fun getShopStock(shopAccountIdStr: String, flowerAssetDef: String = "flower#warehouse"): BigDecimal {
        return try {
            queryService.getAccountAmount(
                shopAccountIdStr.asAccountId(), 
                flowerAssetDef.asAssetDefinitionId()
            ).toBigDecimal()
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }
}
