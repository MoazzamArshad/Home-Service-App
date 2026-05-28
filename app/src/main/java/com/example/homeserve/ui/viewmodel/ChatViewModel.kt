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

    override fun onCleared() {
        super.onCleared()
        chatListenerRegistration?.remove()
        bookingListenerRegistration?.remove()
        if (activeChatBookingId == currentBookingId) {
            activeChatBookingId = null
        }
    }
}
