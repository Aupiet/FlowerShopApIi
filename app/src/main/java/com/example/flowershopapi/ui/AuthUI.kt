package com.example.flowershopapi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.flowershopapi.iroha.IrohaClient
import com.example.flowershopapi.iroha.QueryService
import com.example.flowershopapi.iroha.TransactionService
import com.example.flowershopapi.utils.Identification
import jp.co.soramitsu.iroha2.asString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    irohaClient: IrohaClient,
    queryService: QueryService,
    transactionService: TransactionService,
    identification: Identification,
    onAuthSuccess: (String) -> Unit
) {
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    
    var domains by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedDomain by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    // Load domain list on start
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val allDomains = queryService.findAllDomains()
                val domainNames = allDomains.map { it.id.asString() }
                withContext(Dispatchers.Main) {
                    domains = domainNames
                    if (domainNames.isNotEmpty()) {
                        selectedDomain = domainNames.first()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { statusMessage = "Domain error: ${e.message}" }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when(authMode) {
                AuthMode.LOGIN -> "Login"
                AuthMode.SIGNUP -> "Sign Up"
            },
            style = MaterialTheme.typography.headlineLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Dropdown menu for domains
        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = { isExpanded = !isExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedDomain,
                onValueChange = {},
                readOnly = true,
                label = { Text("Domain") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false }
            ) {
                domains.forEach { domain ->
                    DropdownMenuItem(
                        text = { Text(domain) },
                        onClick = {
                            selectedDomain = domain
                            isExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = usernameInput,
            onValueChange = { usernameInput = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch(Dispatchers.IO) {
                    try {
                        if (selectedDomain.isEmpty()) {
                            withContext(Dispatchers.Main) { statusMessage = "Please select a domain" }
                            return@launch
                        }

                        withContext(Dispatchers.Main) { statusMessage = "Processing..." }
                        
                        when (authMode) {
                            AuthMode.LOGIN -> {
                                identification.connexion(usernameInput, passwordInput, selectedDomain)
                                val technicalId = irohaClient.getAdmin().asString()
                                
                                val accounts = queryService.findAllAccounts()
                                val exists = accounts.any { it.id.asString() == technicalId }
                                
                                if (exists) {
                                    withContext(Dispatchers.Main) { onAuthSuccess(technicalId) }
                                } else {
                                    identification.deconnexion()
                                    withContext(Dispatchers.Main) { 
                                        statusMessage = "Account not found in domain $selectedDomain" 
                                    }
                                }
                            }
                            AuthMode.SIGNUP -> {
                                identification.registerAccountInDomain(usernameInput, passwordInput, selectedDomain)
                                identification.connexion(usernameInput, passwordInput, selectedDomain)
                                val technicalId = irohaClient.getAdmin().asString()
                                
                                withContext(Dispatchers.Main) { 
                                    statusMessage = "Account created!"
                                    onAuthSuccess(technicalId)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { statusMessage = "Error: ${e.message}" }
                    }
                }
            }
        ) {
            Text(if (authMode == AuthMode.LOGIN) "Login" else "Sign Up")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = { 
            authMode = if (authMode == AuthMode.LOGIN) AuthMode.SIGNUP else AuthMode.LOGIN 
        }) {
            Text(if (authMode == AuthMode.LOGIN) "No account? Sign Up" else "Already have an account? Login")
        }

        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = statusMessage, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

enum class AuthMode {
    LOGIN, SIGNUP
}
