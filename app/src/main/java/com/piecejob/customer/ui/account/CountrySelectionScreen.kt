package com.piecejob.customer.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelectionScreen(
    onBack: () -> Unit,
    viewModel: CustomerAccountViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val selectedCountry = user?.country ?: "ZA"

    val countries = listOf(
        "ZA" to "South Africa 🇿🇦",
        "NG" to "Nigeria 🇳🇬",
        "KE" to "Kenya 🇰🇪"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Country", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(countries) { (code, name) ->
                ListItem(
                    headlineContent = { Text(name) },
                    trailingContent = {
                        if (selectedCountry == code) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier.clickable {
                        viewModel.updatePreferences(language = user?.language, country = code)
                    }
                )
            }
        }
    }
}
