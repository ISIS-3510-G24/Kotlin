import { onDocumentCreated } from 'firebase-functions/v2/firestore';
import * as admin from 'firebase-admin';

admin.initializeApp();

export const sendProductNotification = onDocumentCreated(
  { document: 'Product/{productId}' },
  async (event) => {
    const product = event.data?.data();
    if (!product || !product.title) {
      console.error('Product data is missing or title is undefined.');
      return;
    }

    const payload: admin.messaging.MessagingPayload = {
      notification: {
        title: 'New Product Published!',
        body: `Check out the product: ${product.title}`,
        // Si no tienes una lógica específica, puedes dejar click_action como placeholder o eliminarlo.
        click_action: 'FLUTTER_NOTIFICATION_CLICK'
      }
    };

    try {
      const response = await admin.messaging().sendToTopic('new_products', payload);
      console.log('Notification sent successfully:', response);
    } catch (error) {
      console.error('Error sending notification:', error);
    }
  }
);
