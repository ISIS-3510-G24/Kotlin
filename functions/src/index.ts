import {onDocumentCreated} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";

admin.initializeApp();

export const sendProductNotification = onDocumentCreated(
  {document: "Product/{productId}"},
  async (event) => {
    const product = event.data?.data();
    if (!product || !product.title) {
      console.error("Product data is missing or title is undefined.");
      return;
    }

    const payload: admin.messaging.MessagingPayload = {
      notification: {
        title: "New Product Published!",
        body: `Check out the product: ${product.title}`,
        click_action: "KOTLIN_NOTIFICATION_CLICK" // This is the action that will be triggered when the notification is clicked
      }
    };

    try {
      const response = await admin.messaging().sendToTopic("new_products", payload);
      console.log("Notification sent successfully:", response);
    } catch (error) {
      console.error("Error sending notification:", error);
    }
  }
);
