package com.example.flowershopapi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.flowershopapi.iroha.IrohaClient
import com.example.flowershopapi.iroha.IrohaConfig
import com.example.flowershopapi.iroha.QueryService
import com.example.flowershopapi.iroha.TransactionService
import com.example.flowershopapi.ui.AuthScreen
import com.example.flowershopapi.ui.FlowerShopApp
import com.example.flowershopapi.utils.Identification

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = IrohaConfig()
        val irohaClient = IrohaClient(config)
        val queryService = QueryService(irohaClient)
        val transactionService = TransactionService(irohaClient)
        val identification = Identification(irohaClient, transactionService)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var loggedInAccountId by remember { mutableStateOf<String?>(null) }

                    if (loggedInAccountId == null) {
                        AuthScreen(
                            queryService = queryService,
                            transactionService = transactionService,
                            identification = identification,
                            onAuthSuccess = { accountId ->
                                loggedInAccountId = accountId
                            }
                        )
                    } else {
                        FlowerShopApp(
                            queryService = queryService,
                            transactionService = transactionService,
                            currentAccountId = loggedInAccountId!!
                        )
                    }
                }
            }
        }
    }
}
