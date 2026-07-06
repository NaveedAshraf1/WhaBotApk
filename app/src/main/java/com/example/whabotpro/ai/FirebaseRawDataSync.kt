package com.example.whabotpro.ai

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Syncs raw data from Firestore collection "raw_data".
 *
 * Workflow:
 *   1. User adds a document to "raw_data" collection in Firebase Console
 *      with field "content" containing the raw text.
 *   2. App calls fetchPending() to get unprocessed documents.
 *   3. Each document is processed by RawDataProcessor.
 *   4. Document is marked as "processed = true" after success.
 */
class FirebaseRawDataSync {

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("raw_data")

    data class RawDataDoc(
        val id: String,
        val content: String,
        val label: String,
        val processed: Boolean
    )

    /** Fetch unprocessed raw data documents from Firestore. */
    suspend fun fetchPending(): List<RawDataDoc> = try {
        val snapshot = collection
            .whereEqualTo("processed", false)
            .get()
            .await()

        snapshot.documents.mapNotNull { doc ->
            val content = doc.getString("content") ?: return@mapNotNull null
            RawDataDoc(
                id = doc.id,
                content = content,
                label = doc.getString("label") ?: "Untitled",
                processed = false
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    /** Fetch ALL raw data documents (including processed). */
    suspend fun fetchAll(): List<RawDataDoc> = try {
        val snapshot = collection
            .get()
            .await()

        snapshot.documents.mapNotNull { doc ->
            val content = doc.getString("content") ?: return@mapNotNull null
            RawDataDoc(
                id = doc.id,
                content = content,
                label = doc.getString("label") ?: "Untitled",
                processed = doc.getBoolean("processed") ?: false
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    /** Mark a document as processed. */
    suspend fun markProcessed(docId: String, savedCount: Int) {
        try {
            collection.document(docId)
                .update(
                    mapOf(
                        "processed" to true,
                        "savedCount" to savedCount,
                        "processedAt" to System.currentTimeMillis()
                    )
                )
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Check if Firestore is reachable. */
    suspend fun isAvailable(): Boolean = try {
        collection.limit(1).get().await()
        true
    } catch (e: Exception) {
        false
    }
}
