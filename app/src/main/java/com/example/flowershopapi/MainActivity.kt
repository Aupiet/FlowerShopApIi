package com.example.flowershopapi

import android.os.Bundle
import android.util.Log
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuration corrigée : IP pour l'émulateur et clés sans préfixes multihash
        val config = IrohaConfig(
            apiUrl = URL("http://10.0.2.2:8080"),
            chainId = UUID.fromString("00000000-0000-0000-0000-000000000000"),
            adminPublicKey = "32F0C017884729DD1E4F0DE3359F53444AB03FA18D64C3A31C02D2787D5B9CBA",
            adminPrivateKey = "76D6468D63CD0FD0F40BBCB495DA09620BA6F1148CD7A8BFDD075F0F787D7A18"
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
                // Utilisation de Dispatchers.IO pour les appels réseau
                scope.launch(Dispatchers.IO) {
                    try {
                        withContext(Dispatchers.Main) { statusMessage = "Récupération des domaines..." }

                        val result = queryService.findAllDomains()
                        val mappedDomains = result.map { it.id.asString() }

                        withContext(Dispatchers.Main) {
                            domains = mappedDomains
                            statusMessage = "Domaines récupérés"
                        }
                    } catch (e: Throwable) { // Capture de Throwable au lieu de Exception
                        Log.e("IrohaApp", "Crash lors de la récupération des domaines", e)
                        withContext(Dispatchers.Main) {
                            statusMessage = "Erreur: ${e.javaClass.simpleName} - ${e.message}"
                        }
                    }
                }
            }) {
                Text("Lister Domaines")
            }

            Button(onClick = {
                scope.launch(Dispatchers.IO) {
                    try {
                        withContext(Dispatchers.Main) { statusMessage = "Création du domaine..." }

                        val newDomain = "shop_${System.currentTimeMillis()}"
                        transactionService.registerDomain(newDomain)

                        withContext(Dispatchers.Main) {
                            statusMessage = "Domaine $newDomain créé !"
                        }
                    } catch (e: Throwable) {
                        Log.e("IrohaApp", "Crash lors de la création", e)
                        withContext(Dispatchers.Main) {
                            statusMessage = "Erreur: ${e.javaClass.simpleName} - ${e.message}"
                        }
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