/**
 * Firebase Cloud Function (v2) for dispatching FCM Push Notifications
 * whenever a document is created in `notifications_queue` (prayers,
 * 500-Dhikr milestones, new shared Duas, Ameen reactions, or spiritual nudges).
 *
 * Deploy manually via Firebase CLI when FCM device tokens are enabled in Firebase Console:
 *   firebase deploy --only functions
 */
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

const DATABASE_ID = "ai-studio-android-389f0075-9556-420d-8bc2-9a4597a9d143";

exports.dispatchPartnerNotification = onDocumentCreated(
  {
    document: "notifications_queue/{notifId}",
    database: DATABASE_ID,
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const notif = snap.data();
    const { sanctuaryId, recipientRole, title, body, type } = notif;
    if (!sanctuaryId || !recipientRole || !body) return;

    const db = admin.firestore();
    db.settings({ databaseId: DATABASE_ID });

    // Look up recipient's FCM device token stored under fcm_tokens/{sanctuaryId}_{recipientRole}
    const tokenDoc = await db
      .collection("fcm_tokens")
      .doc(`${sanctuaryId}_${recipientRole}`)
      .get();

    const fcmToken = tokenDoc.exists ? tokenDoc.data().token : null;
    if (!fcmToken) {
      console.log(`No FCM token registered for ${sanctuaryId}/${recipientRole}`);
      return;
    }

    await admin.messaging().send({
      token: fcmToken,
      notification: {
        title: title || "طريق الحلال 🤍",
        body,
      },
      data: {
        type: type || "nudge",
        sanctuaryId,
        recipientRole,
      },
      android: {
        priority: "high",
        notification: {
          channelId: "tariq_halal_partner_alerts",
          sound: "default",
        },
      },
    });

    await snap.ref.update({
      delivered: true,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  }
);
