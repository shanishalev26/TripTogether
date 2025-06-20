package com.example.triptogether.utilities   // התאימי לשם-החבילה שלך

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

object FirebaseSyncUtils {

    /**
     * כותב את המשתמש אל ‎/users/{uid}‎ אם הוא עדיין לא קיים
     */
    fun syncCurrentUserToDatabase() {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return           // אין משתמש – צאי

        val userInfo = mapOf(
            "email"    to user.email,
            "name"     to (user.displayName ?: ""),
            "photoUrl" to (user.photoUrl?.toString() ?: "")
        )

        val userRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(user.uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) userRef.setValue(userInfo)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SyncUser", "Failed to sync user", error.toException())
            }
        })
    }
}
