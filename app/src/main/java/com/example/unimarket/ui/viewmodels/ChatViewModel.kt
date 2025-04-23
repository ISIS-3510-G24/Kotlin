package com.example.unimarket.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChatOverview(
    val chatId: String,
    val participants: List<String>,
    val lastMessage: String,
    val lastMessageTime: Long
)

class ChatViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _chats = MutableStateFlow<List<ChatOverview>>(emptyList())
    val chats: StateFlow<List<ChatOverview>> = _chats.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchChats()
    }

    fun fetchChats() {
        val userId = auth.currentUser?.uid ?: return
        _loading.value = true
        _error.value = null

        firestore.collection("chats")
            .whereArrayContains("participants", userId)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { doc ->
                    try {
                        val id = doc.id
                        val participantsRaw = doc.get("participants") as? List<*>
                        val participants = participantsRaw
                            ?.filterIsInstance<String>()
                            ?: emptyList()
                        val lastMsg = doc.getString("lastMessage") ?: ""
                        val lastTime = doc.getTimestamp("lastMessageTime")
                            ?.toDate()
                            ?.time
                            ?: 0L
                        ChatOverview(
                            chatId = id,
                            participants = participants,
                            lastMessage = lastMsg,
                            lastMessageTime = lastTime
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.lastMessageTime }

                _chats.value = list
                _loading.value = false
            }
            .addOnFailureListener { ex ->
                _error.value = "Failed to load chats: ${ex.localizedMessage}"
                _loading.value = false
            }
    }
}
