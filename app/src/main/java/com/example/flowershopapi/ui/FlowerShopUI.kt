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
import com.example.flowershopapi.utils.Interaction
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
    interaction: Interaction,
    currentAccountId: String,
    onLogout: () -> Unit
) {
    val role = remember { inferRole(currentAccountId) }
    var userRole by remember { mutableStateOf(role) }
    var statusMessage by remember { mutableStateOf("Welcome") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Flower Shop - Marketplace", style = MaterialTheme.typography.headlineMedium)
                Text("Connected : $currentAccountId", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = {
                scope.launch {
                    identification.deconnexion()
                    onLogout()
                }
            }) {
                Text("Logout")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Only Warehouse (Admin) can switch views
        if (role == "Warehouse") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { userRole = "Customer" }, modifier = Modifier.weight(1f)) { Text("Customer") }
                Button(onClick = { userRole = "Shop" }, modifier = Modifier.weight(1f)) { Text("Shop") }
                Button(onClick = { userRole = "Warehouse" }, modifier = Modifier.weight(1f)) { Text("Warehouse") }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text("Interface : $userRole", style = MaterialTheme.typography.titleMedium)
        Text("Status : $statusMessage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(16.dp))

        when (userRole) {
            "Customer" -> CustomerView(queryService, transactionService, interaction, currentAccountId) { statusMessage = it }
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
fun CustomerView(
    queryService: QueryService,
    transactionService: TransactionService,
    interaction: Interaction,
    accountId: String,
    onStatus: (String) -> Unit
) {
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
            onStatus("Error : ${e.message}")
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { refreshAssets() }

    Column {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("My Wallet", style = MaterialTheme.typography.titleSmall)
                if (myAssets.isEmpty()) Text("No assets found", style = MaterialTheme.typography.bodySmall)
                myAssets.forEach { asset ->
                    val amount = (asset.value as? AssetValue.Numeric)?.numeric?.mantissa ?: BigInteger.ZERO
                    Text("${asset.id.definition.asString().split("#")[0]}: $amount", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Text("Shops", style = MaterialTheme.typography.titleSmall)
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
                                    val shopAccountId = asset.id.account.asString()
                                    withContext(Dispatchers.Main) { onStatus("Purchasing from $shopAccountId...") }
                                    
                                    // Use the interaction service to buy the flower
                                    interaction.buyFlower(shopAccountId)
                                    
                                    refreshAssets()
                                    withContext(Dispatchers.Main) { onStatus("Purchase successful!") }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) { onStatus("Error : ${e.message}") }
                                }
                            }
                        }) { Text("Buy") }
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
        } catch (e: Exception) { onStatus("Error : ${e.message}") }
    }

    LaunchedEffect(Unit) { refreshData() }

    Column {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("My Stock (Shop)", style = MaterialTheme.typography.titleSmall)
                myAssets.forEach { asset ->
                    val amount = (asset.value as? AssetValue.Numeric)?.numeric?.mantissa ?: BigInteger.ZERO
                    Text("${asset.id.definition.asString().split("#")[0]}: $amount", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Text("Client Wallets", style = MaterialTheme.typography.titleSmall)
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
                    Text("My Stock (Warehouse)", style = MaterialTheme.typography.titleSmall)
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
                    Text("Blockchain Administration", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(value = newDomainId, onValueChange = { newDomainId = it }, label = { Text("New Domain") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { scope.launch(Dispatchers.IO) { 
                        try { transactionService.registerDomain(newDomainId); withContext(Dispatchers.Main) { onStatus("Domain created"); newDomainId = "" } } 
                        catch (e: Exception) { withContext(Dispatchers.Main) { onStatus(e.message ?: "Error") } }
                    }}) { Text("Create Domain") }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    OutlinedTextField(value = newAccountId, onValueChange = { newAccountId = it }, label = { Text("New Account (id@domain)") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { scope.launch(Dispatchers.IO) { 
                        try { transactionService.registerAccount(newAccountId); withContext(Dispatchers.Main) { onStatus("Account created"); newAccountId = ""; refreshData() } } 
                        catch (e: Exception) { withContext(Dispatchers.Main) { onStatus(e.message ?: "Error") } }
                    }}) { Text("Create Account") }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Mint & Transfer", style = MaterialTheme.typography.titleSmall)
                    
                    Text("Mint (Mine Assets)", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(value = mintTargetAccount, onValueChange = { mintTargetAccount = it }, label = { Text("Target Account") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mintAssetName, onValueChange = { mintAssetName = it }, label = { Text("Asset (e.g., rose#flower)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mintAmount, onValueChange = { mintAmount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { scope.launch(Dispatchers.IO) { 
                        try { 
                            val assetId = "$mintAssetName#$mintTargetAccount".asAssetId()
                            transactionService.mintAsset(assetId, BigDecimal(mintAmount))
                            refreshData()
                            withContext(Dispatchers.Main) { onStatus("Mint successful !") }
                        } catch (e: Exception) { withContext(Dispatchers.Main) { onStatus(e.message ?: "Error") } }
                    }}) { Text("Mint (Mine)") }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("Transfer (Ship Assets)", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(value = transferFromAssetId, onValueChange = { transferFromAssetId = it }, label = { Text("Asset to send (e.g., rose#flower#$currentAccountId)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = transferToAccount, onValueChange = { transferToAccount = it }, label = { Text("Recipient Account") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = transferAmount, onValueChange = { transferAmount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { scope.launch(Dispatchers.IO) { 
                        try { 
                            transactionService.transferAsset(transferFromAssetId.asAssetId(), BigDecimal(transferAmount), transferToAccount)
                            refreshData()
                            withContext(Dispatchers.Main) { onStatus("Transfer successful !") }
                        } catch (e: Exception) { withContext(Dispatchers.Main) { onStatus(e.message ?: "Error") } }
                    }}) { Text("Transfer") }
                }
            }
        }

        item { Text("Global Network View", style = MaterialTheme.typography.titleSmall) }
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
