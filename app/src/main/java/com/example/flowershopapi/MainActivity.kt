package com.example.flowershopapi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.flowershopapi.iroha.*
import jp.co.soramitsu.iroha2.asString
import kotlinx.coroutines.launch
import java.net.URL
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = IrohaConfig(
            apiUrl = URL("http://127.0.0.1:8080"),
            chainId = UUID.fromString("00000000-0000-0000-0000-000000000000"),
            adminPublicKey = "ed012032F0C017884729DD1E4F0DE3359F53444AB03FA18D64C3A31C02D2787D5B9CBA",
            adminPrivateKey = "80262076D6468D63CD0FD0F40BBCB495DA09620BA6F1148CD7A8BFDD075F0F787D7A18"
        )

        val irohaClient = IrohaClient(config)
        val queryService = QueryService(irohaClient)
        val transactionService = TransactionService(irohaClient)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    IrohaDashboard(queryService, transactionService)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IrohaDashboard(queryService: QueryService, transactionService: TransactionService) {
    val scope = rememberCoroutineScope()
    var domains by remember { mutableStateOf(listOf<String>()) }
    var statusMessage by remember { mutableStateOf("Prêt") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Flower Shop Iroha 2", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Statut: $statusMessage", style = MaterialTheme.typography.bodySmall)
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = {
                scope.launch {
                    statusMessage = "Récupération des domaines..."
                    try {
                        val result = queryService.findAllDomains()
                        domains = result.map { it.id.asString() }
                        statusMessage = "Domaines récupérés"
                    } catch (e: Exception) {
                        statusMessage = "Erreur: ${e.message}"
                        e.printStackTrace()
                    }
                }
            }) {
                Text("Lister Domaines")
            }

            Button(onClick = {
                scope.launch {
                    statusMessage = "Création du domaine..."
                    try {
                        val newDomain = "shop_${System.currentTimeMillis()}"
                        transactionService.registerDomain(newDomain)
                        statusMessage = "Domaine $newDomain créé !"
                    } catch (e: Exception) {
                        statusMessage = "Erreur: ${e.message}"
                        e.printStackTrace()
                    }
                }
            }) {
                Text("Créer Domaine")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Liste des Domaines:", style = MaterialTheme.typography.titleMedium)
        LazyColumn {
            items(domains) { domain ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(text = domain, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}
