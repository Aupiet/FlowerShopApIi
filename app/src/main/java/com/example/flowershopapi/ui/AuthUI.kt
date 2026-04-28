package com.example.flowershopapi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.flowershopapi.iroha.QueryService
import com.example.flowershopapi.iroha.TransactionService
import com.example.flowershopapi.utils.Identification
import jp.co.soramitsu.iroha2.asString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AuthScreen(
    queryService: QueryService,
    transactionService: TransactionService,
    identification: Identification,
    onAuthSuccess: (String) -> Unit
) {
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when(authMode) {
                AuthMode.LOGIN -> "Connexion"
                AuthMode.SIGNUP -> "Inscription"
                AuthMode.SIGNUP_CUSTOMER -> "Inscription Client"
            },
            style = MaterialTheme.typography.headlineLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = usernameInput,
            onValueChange = { usernameInput = it },
            label = { Text("Pseudo ou ID (nom@domaine)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            label = { Text("Mot de passe") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch(Dispatchers.IO) {
                    try {
                        withContext(Dispatchers.Main) { statusMessage = "Traitement..." }
                        
                        val finalAccountId = if (usernameInput.contains("@")) {
                            usernameInput 
                        } else if (authMode == AuthMode.SIGNUP_CUSTOMER) {
                            "$usernameInput@customer"
                        } else {
                            "$usernameInput@wonderland" // Domaine par défaut
                        }

                        when (authMode) {
                            AuthMode.LOGIN -> {
                                // 1. Tenter la connexion (dérivation des clés et mise à jour config)
                                identification.connexion(usernameInput, passwordInput)
                                
                                // 2. Vérifier si le compte existe sur la blockchain
                                val accounts = queryService.findAllAccounts()
                                val exists = accounts.any { it.id.asString() == finalAccountId }
                                
                                if (exists) {
                                    withContext(Dispatchers.Main) { onAuthSuccess(finalAccountId) }
                                } else {
                                    // Si échec, on remet la config par défaut
                                    identification.deconnexion()
                                    withContext(Dispatchers.Main) { statusMessage = "Compte introuvable sur le réseau" }
                                }
                            }
                            AuthMode.SIGNUP, AuthMode.SIGNUP_CUSTOMER -> {
                                // 1. Créer le compte sur la blockchain (signé par l'admin actuel)
                                if (authMode == AuthMode.SIGNUP_CUSTOMER) {
                                    identification.registerCustomerAccount(usernameInput, passwordInput)
                                } else {
                                    transactionService.registerAccount(finalAccountId)
                                }
                                
                                // 2. Connecter automatiquement l'utilisateur après création
                                identification.connexion(usernameInput, passwordInput)
                                
                                withContext(Dispatchers.Main) { 
                                    statusMessage = "Compte créé et connecté !"
                                    onAuthSuccess(finalAccountId)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { statusMessage = "Erreur: ${e.message}" }
                    }
                }
            }
        ) {
            Text(if (authMode == AuthMode.LOGIN) "Se connecter" else "Créer et se connecter")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (authMode == AuthMode.LOGIN) {
            TextButton(onClick = { authMode = AuthMode.SIGNUP }) {
                Text("S'inscrire (Général)")
            }
            TextButton(onClick = { authMode = AuthMode.SIGNUP_CUSTOMER }) {
                Text("Devenir Client (Domaine Customer)")
            }
        } else {
            TextButton(onClick = { authMode = AuthMode.LOGIN }) {
                Text("Déjà un compte ? Se connecter")
            }
        }

        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = statusMessage, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

enum class AuthMode {
    LOGIN, SIGNUP, SIGNUP_CUSTOMER
}
