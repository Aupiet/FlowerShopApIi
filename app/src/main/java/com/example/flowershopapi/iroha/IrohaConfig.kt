package com.example.flowershopapi.iroha

import java.net.URL
import java.util.UUID

data class IrohaConfig(
    val apiUrl: URL = URL("http://10.0.2.2:8080"),
    val chainId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    val adminDomain: String = "wonderland",
    val adminPublicKey: String = "CE7FA46C9DCE7EA4B125E2E36BDB63EA33073E7590AC92816AE1E861B7048B03",
    val adminPrivateKey: String = "CCF31D85E3B32A4BEA59987CE0C78E3B8E2DB93881468AB2435FE45D5C9DCD53"
)