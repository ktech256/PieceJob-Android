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
fun LanguageSelectionScreen(
    onBack: () -> Unit,
    viewModel: CustomerAccountViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val selectedLanguage = user?.language ?: "en"

    val languages = listOf(
        "en" to "English",
        "zu" to "isiZulu",
        "xh" to "isiXhosa",
        "af" to "Afrikaans",
        "st" to "Sesotho"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Language", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(languages) { (code, name) ->
                ListItem(
                    headlineContent = { Text(name) },
                    trailingContent = {
                        if (selectedLanguage == code) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier.clickable {
                        viewModel.updatePreferences(language = code, country = user?.country)
                    }
                )
            }
        }
    }
}
