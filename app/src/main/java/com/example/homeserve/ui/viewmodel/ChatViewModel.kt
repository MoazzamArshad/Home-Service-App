package com.example.homeserve.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeserve.data.FirestoreRepository
import com.example.homeserve.data.model.Booking
import com.example.homeserve.data.model.ChatMessage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatViewModel : ViewModel() {
    companion object {
        var activeChatBookingId: String? = null
    }

    private val repository = FirestoreRepository()
    private val db = FirebaseFirestore.getInstance()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _bookingInfo = MutableStateFlow<Booking?>(null)
    val bookingInfo: StateFlow<Booking?> = _bookingInfo.asStateFlow()

    private val _otherPartyPhotoUrl = MutableStateFlow<String>("")
    val otherPartyPhotoUrl: StateFlow<String> = _otherPartyPhotoUrl.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var chatListenerRegistration: ListenerRegistration? = null
    private var bookingListenerRegistration: ListenerRegistration? = null

    private var currentBookingId = ""
    private var currentSenderRole = "" // "customer" or "provider"

    fun initChat(bookingId: String, senderRole: String) {
        if (currentBookingId == bookingId && currentSenderRole == senderRole) return
        
        currentBookingId = bookingId
        currentSenderRole = senderRole
        activeChatBookingId = bookingId

        listenToBookingDetails(bookingId)
        listenToMessages(bookingId)
        markAsRead()
    }

    private fun listenToBookingDetails(bookingId: String) {
        bookingListenerRegistration?.remove()
        bookingListenerRegistration = db.collection("bookings")
            .document(bookingId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val booking = snapshot.toObject(Booking::class.java)?.copy(bookingId = snapshot.id)
                    _bookingInfo.value = booking
                    if (booking != null) {
                        viewModelScope.launch {
                            val otherPhoto = if (currentSenderRole == "customer") {
                                try {
                                    db.collection("providers").document(booking.providerId).get().await().getString("profilePhotoUrl") ?: ""
                                } catch (e: Exception) { "" }
                            } else {
                                try {
                                    db.collection("users").document(booking.userId).get().await().getString("profilePhotoUrl") ?: ""
                                } catch (e: Exception) { "" }
                            }
                            _otherPartyPhotoUrl.value = otherPhoto
                        }
                    }
                }
            }
    }

    private fun listenToMessages(bookingId: String) {
        chatListenerRegistration?.remove()
        _isLoading.value = true

        chatListenerRegistration = db.collection("bookings")
            .document(bookingId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatMessage::class.java)?.copy(messageId = doc.id)
                    }
                    _messages.value = list
                    markAsRead()
                }
            }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || currentBookingId.isEmpty()) return

        val booking = _bookingInfo.value ?: return
        val senderId = if (currentSenderRole == "customer") booking.userId else booking.providerId
        val senderName = if (currentSenderRole == "customer") booking.customerName else booking.providerName

        val newMessage = ChatMessage(
            senderId = senderId,
            senderName = senderName,
            senderRole = currentSenderRole,
            text = text,
            timestamp = Timestamp.now()
        )

        viewModelScope.launch {
            repository.sendChatMessage(currentBookingId, newMessage)
        }
    }

    fun sendVoiceMessage(audioUrl: String, durationSec: Int) {
        if (audioUrl.isEmpty() || currentBookingId.isEmpty()) return

        val booking = _bookingInfo.value ?: return
        val senderId = if (currentSenderRole == "customer") booking.userId else booking.providerId
        val senderName = if (currentSenderRole == "customer") booking.customerName else booking.providerName

        val newMessage = ChatMessage(
            senderId = senderId,
            senderName = senderName,
            senderRole = currentSenderRole,
            text = "[Voice Message]",
            timestamp = Timestamp.now(),
            voiceUrl = audioUrl,
            voiceDuration = durationSec
        )

        viewModelScope.launch {
            repository.sendChatMessage(currentBookingId, newMessage)
        }
    }

    fun sendImageMessage(imageUrl: String) {
        if (imageUrl.isEmpty() || currentBookingId.isEmpty()) return

        val booking = _bookingInfo.value ?: return
        val senderId = if (currentSenderRole == "customer") booking.userId else booking.providerId
        val senderName = if (currentSenderRole == "customer") booking.customerName else booking.providerName

        val newMessage = ChatMessage(
            senderId = senderId,
            senderName = senderName,
            senderRole = currentSenderRole,
            text = "[Image Message]",
            timestamp = Timestamp.now(),
            imageUrl = imageUrl
        )

        viewModelScope.launch {
            repository.sendChatMessage(currentBookingId, newMessage)
        }
    }

    private var lastTypingState = false

    fun setTyping(isTyping: Boolean) {
        val bookingId = currentBookingId
        if (bookingId.isEmpty() || lastTypingState == isTyping) return
        lastTypingState = isTyping
        val field = if (currentSenderRole == "customer") "customerTyping" else "providerTyping"
        viewModelScope.launch {
            try {
                db.collection("bookings").document(bookingId)
                    .update(field, isTyping)
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markAsRead() {
        val bookingId = currentBookingId
        if (bookingId.isEmpty()) return
        val field = if (currentSenderRole == "customer") "customerLastRead" else "providerLastRead"
        viewModelScope.launch {
            try {
                db.collection("bookings").document(bookingId)
                    .update(field, Timestamp.now())
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateBookingPrice(price: Int, onComplete: (Boolean) -> Unit = {}) {
        val bookingId = currentBookingId
        if (bookingId.isEmpty() || price < 0) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            val success = repository.updateBookingPrice(bookingId, price)
            if (success) {
                val booking = _bookingInfo.value
                val senderId = booking?.providerId ?: ""
                val senderName = booking?.providerName ?: "Provider"
                val systemMsg = ChatMessage(
                    senderId = senderId,
                    senderName = senderName,
                    senderRole = "provider",
                    text = "System: The service price was updated to Rs. $price.",
                    timestamp = Timestamp.now()
                )
                repository.sendChatMessage(bookingId, systemMsg)
            }
            onComplete(success)
        }
    }

    override fun onCleared() {
        super.onCleared()
        setTyping(false)
        chatListenerRegistration?.remove()
        bookingListenerRegistration?.remove()
        if (activeChatBookingId == currentBookingId) {
            activeChatBookingId = null
        }
    }
}
