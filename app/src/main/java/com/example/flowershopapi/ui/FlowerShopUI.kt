package com.example.flowershopapi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.flowershopapi.iroha.QueryService
import com.example.flowershopapi.iroha.TransactionService
import jp.co.soramitsu.iroha2.asAssetId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

@Composable
fun FlowerShopApp(
    queryService: QueryService,
    transactionService: TransactionService,
    currentAccountId: String
) {
    var userRole by remember { mutableStateOf(inferRole(currentAccountId)) }
    var statusMessage by remember { mutableStateOf("Bienvenue, $currentAccountId") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Flower Shop - Système Iroha 2", style = MaterialTheme.typography.headlineMedium)
        Text("Connecté en tant que : $currentAccountId", style = MaterialTheme.typography.bodySmall)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Navigation par onglets/rôles (pour démo, normalement restreint par le compte)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { userRole = "Customer" }, modifier = Modifier.weight(1f)) { Text("Client") }
            Button(onClick = { userRole = "Shop" }, modifier = Modifier.weight(1f)) { Text("Boutique") }
            Button(onClick = { userRole = "Warehouse" }, modifier = Modifier.weight(1f)) { Text("Entrepôt") }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Interface : $userRole", style = MaterialTheme.typography.titleMedium)
        Text("Statut : $statusMessage", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(16.dp))

        when (userRole) {
            "Customer" -> CustomerView(queryService, transactionService, currentAccountId) { statusMessage = it }
            "Shop" -> ShopView(queryService, transactionService, currentAccountId) { statusMessage = it }
            "Warehouse" -> WarehouseView(queryService, transactionService, currentAccountId) { statusMessage = it }
        }
    }
}

private fun inferRole(accountId: String): String {
    return when {
        accountId.contains("warehouse") -> "Warehouse"
        accountId.contains("shop") -> "Shop"
        else -> "Customer"
    }
}

@Composable
fun CustomerView(queryService: QueryService, transactionService: TransactionService, accountId: String, onStatus: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    Column {
        Text("Mes commandes", style = MaterialTheme.typography.titleSmall)
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch(Dispatchers.IO) {
                    try {
                        withContext(Dispatchers.Main) { onStatus("Achat d'une rose...") }
                        // Simulation de transfert d'argent (si existant) vers le shop
                        // transactionService.transferAsset(...)
                        withContext(Dispatchers.Main) { onStatus("Fleur commandée par $accountId !") }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { onStatus("Erreur : ${e.message}") }
                    }
                }
            }
        ) { Text("Acheter Rose") }
    }
}

@Composable
fun ShopView(queryService: QueryService, transactionService: TransactionService, accountId: String, onStatus: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    Column {
        Text("Commandes à l'Entrepôt", style = MaterialTheme.typography.titleSmall)
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch(Dispatchers.IO) {
                    try {
                        withContext(Dispatchers.Main) { onStatus("Demande de restock en cours...") }
                        // Logique de demande
                        withContext(Dispatchers.Main) { onStatus("Restock demandé par $accountId") }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { onStatus("Erreur : ${e.message}") }
                    }
                }
            }
        ) { Text("Demander 10 Roses au Warehouse") }
    }
}

@Composable
fun WarehouseView(queryService: QueryService, transactionService: TransactionService, accountId: String, onStatus: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    Column {
        Text("Production & Logistique", style = MaterialTheme.typography.titleSmall)
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch(Dispatchers.IO) {
                    try {
                        withContext(Dispatchers.Main) { onStatus("Production de roses...") }
                        val assetId = "rose#flower#$accountId".asAssetId()
                        transactionService.mintAsset(assetId, BigDecimal(20))
                        withContext(Dispatchers.Main) { onStatus("20 Roses produites dans l'entrepôt") }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { onStatus("Erreur : ${e.message}") }
                    }
                }
            }
        ) { Text("Produire 20 Roses") }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch(Dispatchers.IO) {
                    try {
                        withContext(Dispatchers.Main) { onStatus("Livraison à la boutique...") }
                        val assetId = "rose#flower#$accountId".asAssetId()
                        transactionService.transferAsset(assetId, BigDecimal(10), "shop@flower")
                        withContext(Dispatchers.Main) { onStatus("10 Roses livrées au Shop !") }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { onStatus("Erreur : ${e.message}") }
                    }
                }
            }
        ) { Text("Livrer 10 Roses au Shop") }
    }
}
