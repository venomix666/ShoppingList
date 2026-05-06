package com.example.shoppinglist

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirestoreRepository (
    private val db: FirebaseFirestore
) {

    fun upsertList(list: ShoppingListEntity) {
        if (list.shareId.isBlank()) return

        db.collection("lists")
            .document(list.shareId)
            .set(
                mapOf(
                    "name" to list.name,
                    "createdAt" to list.createdAt
                )
            )
    }

    suspend fun deleteListCascade(shareId: String) {

        val listRef = db.collection("lists").document(shareId)

        val snapshot = listRef.collection("items").get().await()

        val batch = db.batch()

        for (doc in snapshot.documents) {
            batch.delete(doc.reference)
        }

        batch.delete(listRef)

        batch.commit().await()
    }

    fun upsertItem(shareId: String, item: ShoppingItemEntity) {
        db.collection("lists")
            .document(shareId)
            .collection("items")
            .document(item.id)
            .set(item)
    }

    fun deleteItem(shareId: String, itemId: String) {
        db.collection("lists")
            .document(shareId)
            .collection("items")
            .document(itemId)
            .delete()
    }
    fun observeItems(
        shareId: String,
        onChange: (List<ShoppingItemEntity>) -> Unit
    ) {
        db.collection("lists")
            .document(shareId)
            .collection("items")
            .addSnapshotListener { snap, _ ->

                val items = snap?.documents?.mapNotNull {
                    ShoppingItemEntity(
                        id = it.id,
                        shareId = shareId,
                        name = it.getString("name") ?: "",
                        categoryId = it.getLong("categoryId"),
                        picked = it.getBoolean("picked") ?: false,
                        quantity = (it.getLong("quantity") ?: 0L).toInt()
                    )
                } ?: emptyList()

                onChange(items)
            }
    }

    fun observeLists(onChange: (List<ShoppingListEntity>) -> Unit) {
        db.collection("lists")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val lists = snap?.documents?.mapNotNull {
                    it.toObject(ShoppingListEntity::class.java)?.copy(
                        shareId = it.id
                    )
                } ?: emptyList()

                onChange(lists)
            }
    }
}