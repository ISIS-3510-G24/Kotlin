import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { BigQuery } from "@google-cloud/bigquery";

admin.initializeApp();
const PROJECT = "unimarketyay";
const bq = new BigQuery({ projectId: PROJECT });
const db = admin.firestore();

/**
 * 1) Sincroniza recomendaciones diarias de BigQuery → Firestore.
 */
export const syncRecs = functions
  .region("us-central1")
  .pubsub
  .schedule("0 3 * * *")             // a las 3:00 AM
  .timeZone("America/Bogota")
  .onRun(async () => {
    // 1. lanzamos el job y esperamos resultados
    const query = `
      SELECT user_id, recs
      FROM \`${PROJECT}.recs.user_recommendations\`
    `;
    const [job] = await bq.createQueryJob({ query, location: "US" });
    const [rows] = await job.getQueryResults();

    // 2. casteamos a nuestro tipo
    type Row = { user_id: string; recs: Array<{ product_id: string }> };
    const typedRows = rows as Row[];

    // 3. empacamos en batch y subimos a Firestore
    const batch = db.batch();
    typedRows.forEach((row) => {
      const ref = db.collection("recommendations").doc(row.user_id);
      batch.set(ref, {
        products: row.recs.map((r) => r.product_id),
      });
    });
    await batch.commit();
    console.log("✅ Recs sincronizadas:", typedRows.length);
  });

/**
 * 2) Notifica push (via FCM) al crearse un producto nuevo.
 */
export const onNewProduct = functions
  .region("us-central1")
  .firestore
  .document("Product/{productId}")
  .onCreate(async (snap) => {
    const product = snap.data()!;
    const tags = Array.isArray(product.labels) ? product.labels : [];
    const major = product.majorID as string;

    // 1. busca prefs de usuario que tengan alguna etiqueta o majorID
    const usersSnap = await db
      .collection("user_prefs")
      .where("tags", "array-contains-any", [...tags, major])
      .get();

    // 2. acumula tokens y dispara FCM
    const tokens = usersSnap.docs
      .map((d) => d.data().fcmToken as string | undefined)
      .filter((t): t is string => !!t);

    if (!tokens.length) return;
    await admin.messaging().sendToDevice(tokens, {
      notification: {
        title: "New product available!",
        body: `Take a look at "${product.title}". It matches your interests.`,
      },
      data: { productId: snap.id },
    });
  });
