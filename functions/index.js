const { onDocumentUpdated, onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * Triggered when a booking document in `/bookings/{bookingId}` is updated.
 * Detects status changes and notifies the customer or provider.
 */
exports.onBookingUpdate = onDocumentUpdated("bookings/{bookingId}", async (event) => {
  const beforeData = event.data.before.data();
  const afterData = event.data.after.data();

  if (!beforeData || !afterData) {
    return null;
  }

  // Detect if status changed
  if (beforeData.status === afterData.status) {
    return null;
  }

  const newStatus = afterData.status;
  const serviceName = afterData.serviceName || "Home Service";
  const userId = afterData.userId;
  const providerId = afterData.providerId;

  let recipientId = null;
  let recipientRole = null; // "user" or "provider"
  let title = "";
  let body = "";
  let targetScreen = "";

  switch (newStatus) {
    case "accepted":
      recipientId = userId;
      recipientRole = "user";
      title = "Booking Accepted 🎉";
      body = `A provider has accepted your request for ${serviceName}!`;
      targetScreen = "bookings";
      break;

    case "in_progress":
      recipientId = userId;
      recipientRole = "user";
      title = "Service Started 🛠️";
      body = `Your provider is now working on your ${serviceName} request.`;
      targetScreen = "bookings";
      break;

    case "completed":
      recipientId = userId;
      recipientRole = "user";
      title = "Service Completed ✅";
      body = `Your booking for ${serviceName} has been marked as completed.`;
      targetScreen = "bookings";
      break;
  }

  if (newStatus === "cancelled") {
    // Notify Customer if customer exists
    if (userId) {
      await sendNotification(
        userId,
        "user",
        "Booking Cancelled ❌",
        `Your booking for ${serviceName} was cancelled.`,
        "bookings"
      );
    }
    // Notify Provider if provider exists
    if (providerId && providerId !== "test_provider_id_123") {
      await sendNotification(
        providerId,
        "provider",
        "Job Cancelled ❌",
        `The job request for ${serviceName} has been cancelled.`,
        "provider_jobs"
      );
    }
    return null;
  }

  if (recipientId) {
    await sendNotification(recipientId, recipientRole, title, body, targetScreen);
  }

  return null;
});

/**
 * Triggered when a new chat message document is added to `/bookings/{bookingId}/messages/{messageId}`.
 * Notifies the opposite party (customer or provider) in the conversation.
 */
exports.onNewChatMessage = onDocumentCreated("bookings/{bookingId}/messages/{messageId}", async (event) => {
  const messageData = event.data.data();
  if (!messageData) return null;

  const bookingId = event.params.bookingId;
  const senderId = messageData.senderId;
  const senderName = messageData.senderName || "Provider";
  const text = messageData.text || "New message";

  // Fetch the booking document to find the customer and provider IDs
  const bookingSnapshot = await admin.firestore().collection("bookings").doc(bookingId).get();
  if (!bookingSnapshot.exists) {
    return null;
  }

  const bookingData = bookingSnapshot.data();
  const userId = bookingData.userId;
  const providerId = bookingData.providerId;

  // Determine recipient based on who sent the message
  let recipientId = null;
  let recipientRole = null; // "user" or "provider"
  let targetScreen = "";

  if (senderId === userId) {
    recipientId = providerId;
    recipientRole = "provider";
    targetScreen = `chat/${bookingId}/provider`;
  } else if (senderId === providerId) {
    recipientId = userId;
    recipientRole = "user";
    targetScreen = `chat/${bookingId}/customer`;
  }

  if (recipientId && recipientId !== "test_provider_id_123") {
    await sendNotification(
      recipientId,
      recipientRole,
      `New Message from ${senderName} 💬`,
      text,
      targetScreen
    );
  }

  return null;
});

/**
 * Helper function to:
 * 1. Insert in-app notification in Firestore `/notifications`
 * 2. Look up the recipient's FCM token in `/users` or `/providers`
 * 3. Send FCM push notification via Firebase Admin Messaging SDK
 */
async function sendNotification(recipientId, role, title, body, targetScreen) {
  try {
    const db = admin.firestore();

    // 1. Write to notifications collection
    const notificationDoc = {
      recipientId: recipientId,
      title: title,
      message: body,
      read: false,
      targetScreen: targetScreen,
      timestamp: admin.firestore.FieldValue.serverTimestamp()
    };
    await db.collection("notifications").add(notificationDoc);
    console.log(`Saved in-app notification document for recipient: ${recipientId}`);

    // 2. Fetch user's FCM token
    const collectionName = role === "provider" ? "providers" : "users";
    const userDoc = await db.collection(collectionName).doc(recipientId).get();
    
    if (!userDoc.exists) {
      console.log(`No user document found for recipient ${recipientId} in collection ${collectionName}`);
      return;
    }

    const fcmToken = userDoc.data().fcmToken;
    if (!fcmToken) {
      console.log(`No FCM token registered for recipient ${recipientId}`);
      return;
    }

    // 3. Send high-priority FCM payload
    const payload = {
      token: fcmToken,
      notification: {
        title: title,
        body: body
      },
      data: {
        title: title,
        body: body,
        targetScreen: targetScreen
      },
      android: {
        priority: "high"
      }
    };

    await admin.messaging().send(payload);
    console.log(`Successfully sent push notification to ${recipientId}`);
  } catch (error) {
    console.error(`Error sending push/in-app notification to ${recipientId}:`, error);
  }
}
