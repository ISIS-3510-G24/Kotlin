package com.example.unimarket.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

data class ChatOverview(
    val chatId: String,
    val title: String,
    val lastMessage: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    onNavigateToChat: (String) -> Unit
) {
    val chats = listOf(
        ChatOverview("chat1", "Vendedor Yamaha", "¡Hola! ¿Todavía disponible?"),
        ChatOverview("chat2", "Comprador Scalpel", "Gracias por la venta."),
        ChatOverview("chat3", "Soporte Unimarket", "¿En qué podemos ayudarte?")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Nueva conversación */ }) {
                        Icon(Icons.Filled.ChatBubble, contentDescription = "Nuevo chat")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(chats) { chat ->
                ChatRow(chat = chat, onClick = { onNavigateToChat(chat.chatId) })
                Divider()
            }
        }
    }
}

@Composable
private fun ChatRow(chat: ChatOverview, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.ChatBubble,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(chat.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(chat.lastMessage, style = MaterialTheme.typography.bodySmall)
        }
    }
}
