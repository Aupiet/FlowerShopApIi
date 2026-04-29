package com.example.flowershopapi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.flowershopapi.iroha.QueryService
import com.example.flowershopapi.iroha.TransactionService
import com.example.flowershopapi.utils.Identification
import jp.co.soramitsu.iroha2.asAccountId
import jp.co.soramitsu.iroha2.asAssetId
import jp.co.soramitsu.iroha2.asString
import jp.co.soramitsu.iroha2.generated.Account
import jp.co.soramitsu.iroha2.generated.Asset
import jp.co.soramitsu.iroha2.generated.AssetValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger

@Composable
fun FlowerShopApp(
    queryService: QueryService,
    transactionService: TransactionService,
    identification: Identification,
    currentAccountId: String,
    onLogout: () -> Unit
) {
    val role = remember { inferRole(currentAccountId) }
    var userRole by remember { mutableStateOf(role) }
    var statusMessage by remember { mutableStateOf("Bienvenue") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Flower Shop - Marketplace", style = MaterialTheme.typography.headlineMedium)
                Text("Connecté : $currentAccountId", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = {
                scope.launch {
                    identification.deconnexion()
                    onLogout()
                }
            }) {
                Text("Déconnexion")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Seul le rôle Warehouse (Admin) peut changer de vue
        if (role == "Warehouse") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { userRole = "Customer" }, modifier = Modifier.weight(1f)) { Text("Vue Client") }
                Button(onClick = { userRole = "Shop" }, modifier = Modifier.weight(1f)) { Text("Boutique") }
                Button(onClick = { userRole = "Warehouse" }, modifier = Modifier.weight(1f)) { Text("Entrepôt") }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text("Interface : $userRole", style = MaterialTheme.typography.titleMedium)
        Text("Statut : $statusMessage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
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
        accountId.contains("warehouse", ignoreCase = true) -> "Warehouse"
        accountId.contains("shop", ignoreCase = true) -> "Shop"
        else -> "Customer"
    }
}

@Composable
fun CustomerView(queryService: QueryService, transactionService: TransactionService, accountId: String, onStatus: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var shopAssets by remember { mutableStateOf<List<Asset>>(emptyList()) }
    var myAssets by remember { mutableStateOf<List<Asset>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val refreshAssets = suspend {
        isLoading = true
        try {
            val allAssets = queryService.findAllAssets(null)
            shopAssets = allAssets.filter { it.id.account.asString().contains("shop", ignoreCase = true) }
            myAssets = allAssets.filter { it.id.account.asString() == accountId }
        } catch (e: Exception) {
            onStatus("Erreur : ${e.message}")
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { refreshAssets() }

    Column {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Mon Portefeuille", style = MaterialTheme.typography.titleSmall)
                if (myAssets.isEmpty()) Text("Aucun asset trouvé", style = MaterialTheme.typography.bodySmall)
                myAssets.forEach { asset ->
                    val amount = (asset.value as? AssetValue.Numeric)?.numeric?.mantissa ?: BigInteger.ZERO
                    Text("${asset.id.definition.asString().split("#")[0]}: $amount", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Text("Boutiques", style = MaterialTheme.typography.titleSmall)
        if (isLoading) CircularProgressIndicator()
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shopAssets) { asset ->
                val amount = (asset.value as? AssetValue.Numeric)?.numeric?.mantissa ?: BigInteger.ZERO
                val assetName = asset.id.definition.asString().split("#")[0]
                val shopOwner = asset.id.account.asString()
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(assetName, style = MaterialTheme.typography.titleMedium)
                            Text("Shop: $shopOwner", style = MaterialTheme.typography.bodySmall)
                            Text("Stock: $amount")
                        }
                        Button(onClick = {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    transactionService.transferAsset(asset.id, BigDecimal(1), accountId)
                                    refreshAssets()
                                    withContext(Dispatchers.Main) { onStatus("Achat réussi !") }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) { onStatus("Erreur : ${e.message}") }
                                }
                            }
                        }) { Text("Acheter") }
                    }
                }
            }
        }
    }
}

@Composable
fun ShopView(queryService: QueryService, transactionService: TransactionService, currentAccountId: String, onStatus: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var clients by remember { mutableStateOf<List<Account>>(emptyList()) }
    var myAssets by remember { mutableStateOf<List<Asset>>(emptyList()) }
    var allAssets by remember { mutableStateOf<List<Asset>>(emptyList()) }

    val refreshData = suspend {
        try {
            val accounts = queryService.findAllAccounts()
            clients = accounts.filter { it.id.asString().contains("customer", ignoreCase = true) }
            allAssets = queryService.findAllAssets(null)
            myAssets = allAssets.filter { it.id.account.asString() == currentAccountId }
        } catch (e: Exception) { onStatus("Erreur : ${e.message}") }
    }

    LaunchedEffect(Unit) { refreshData() }

    Column {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Mon Stock (Boutique)", style = MaterialTheme.typography.titleSmall)
                myAssets.forEach { asset ->
                    val amount = (asset.value as? AssetValue.Numeric)?.numeric?.mantissa ?: BigInteger.ZERO
                    Text("${asset.id.definition.asString().split("#")[0]}: $amount", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Text("Portefeuille des Clients", style = MaterialTheme.typography.titleSmall)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(clients) { client ->
                val clientAssets = allAssets.filter { it.id.account == client.id }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Client: ${client.id.asString()}", style = MaterialTheme.typography.labelLarge)
                        clientAssets.forEach { asset ->
                            val amount = (asset.value as? AssetValue.Numeric)?.numeric?.mantissa ?: BigInteger.ZERO
                            Text("- ${asset.id.definition.asString().split("#")[0]}: $amount", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WarehouseView(queryService: QueryService, transactionService: TransactionService, currentAccountId: String, onStatus: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }
    var myAssets by remember { mutableStateOf<List<Asset>>(emptyList()) }
    var allAssets by remember { mutableStateOf<List<Asset>>(emptyList()) }
    
    // Form states
    var newDomainId by remember { mutableStateOf("") }
    var newAccountId by remember { mutableStateOf("") }
    var mintTargetAccount by remember { mutableStateOf("") }
    var mintAssetName by remember { mutableStateOf("rose#flower") }
    var mintAmount by remember { mutableStateOf("100") }
    
    var transferFromAssetId by remember { mutableStateOf("") }
    var transferAmount by remember { mutableStateOf("10") }
    var transferToAccount by remember { mutableStateOf("shop@flower") }

    val refreshData = suspend {
        try { 
            accounts = queryService.findAllAccounts()
            allAssets = queryService.findAllAssets(null)
            myAssets = allAssets.filter { it.id.account.asString() == currentAccountId }
        } catch (e: Exception) { onStatus(e.message ?: "Error") }
    }

    LaunchedEffect(Unit) { refreshData() }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Mon Stock (Warehouse)", style = MaterialTheme.typography.titleSmall)
                    myAssets.forEach { asset ->
                        val amount = (asset.value as? AssetValue.Numeric)?.numeric?.mantissa ?: BigInteger.ZERO
                        Text("${asset.id.definition.asString().split("#")[0]}: $amount", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Administration Blockchain", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(value = newDomainId, onValueChange = { newDomainId = it }, label = { Text("Nouveau Domaine") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { scope.launch(Dispatchers.IO) { 
                        try { transactionService.registerDomain(newDomainId); withContext(Dispatchers.Main) { onStatus("Domaine créé"); newDomainId = "" } } 
                        catch (e: Exception) { withContext(Dispatchers.Main) { onStatus(e.message ?: "Err") } }
                    }}) { Text("Créer Domaine") }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    OutlinedTextField(value = newAccountId, onValueChange = { newAccountId = it }, label = { Text("Nouveau Compte (id@domaine)") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { scope.launch(Dispatchers.IO) { 
                        try { transactionService.registerAccount(newAccountId); withContext(Dispatchers.Main) { onStatus("Compte créé"); newAccountId = ""; refreshData() } } 
                        catch (e: Exception) { withContext(Dispatchers.Main) { onStatus(e.message ?: "Err") } }
                    }}) { Text("Créer Compte") }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Mint & Transfert", style = MaterialTheme.typography.titleSmall)
                    
                    Text("Mint (Miner)", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(value = mintTargetAccount, onValueChange = { mintTargetAccount = it }, label = { Text("Compte cible") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mintAssetName, onValueChange = { mintAssetName = it }, label = { Text("Asset (ex: rose#flower)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mintAmount, onValueChange = { mintAmount = it }, label = { Text("Quantité") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { scope.launch(Dispatchers.IO) { 
                        try { 
                            val assetId = "$mintAssetName#$mintTargetAccount".asAssetId()
                            transactionService.mintAsset(assetId, BigDecimal(mintAmount))
                            refreshData()
                            withContext(Dispatchers.Main) { onStatus("Mint réussi !") }
                        } catch (e: Exception) { withContext(Dispatchers.Main) { onStatus(e.message ?: "Err") } }
                    }}) { Text("Miner (Mint)") }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("Transfert (Expédier)", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(value = transferFromAssetId, onValueChange = { transferFromAssetId = it }, label = { Text("Asset à envoyer (ex: rose#flower#$currentAccountId)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = transferToAccount, onValueChange = { transferToAccount = it }, label = { Text("Compte destinataire") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = transferAmount, onValueChange = { transferAmount = it }, label = { Text("Quantité") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { scope.launch(Dispatchers.IO) { 
                        try { 
                            transactionService.transferAsset(transferFromAssetId.asAssetId(), BigDecimal(transferAmount), transferToAccount)
                            refreshData()
                            withContext(Dispatchers.Main) { onStatus("Transfert réussi !") }
                        } catch (e: Exception) { withContext(Dispatchers.Main) { onStatus(e.message ?: "Err") } }
                    }}) { Text("Transférer") }
                }
            }
        }

        item { Text("Visualisation Globale", style = MaterialTheme.typography.titleSmall) }
        items(accounts) { account ->
            val accountAssets = allAssets.filter { it.id.account == account.id }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(account.id.asString(), style = MaterialTheme.typography.labelLarge)
                    accountAssets.forEach { asset ->
                        val amount = (asset.value as? AssetValue.Numeric)?.numeric?.mantissa ?: BigInteger.ZERO
                        Text("- ${asset.id.definition.asString().split("#")[0]}: $amount", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
