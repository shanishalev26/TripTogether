package com.example.triptogether

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.triptogether.adapters.EditFriendsAdapter
import com.example.triptogether.model.Friend
import com.example.triptogether.model.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.checkerframework.common.subtyping.qual.Bottom

class EditFriendsActivity : AppCompatActivity() {

    private lateinit var rvEditFriends: RecyclerView
    private lateinit var adapter: EditFriendsAdapter
    private val friendsList = mutableListOf<Friend>()

    private lateinit var tripId: String
    private lateinit var createdBy: String
    private lateinit var currentUserId: String

    private lateinit var btnAddFriend: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_friends)

        rvEditFriends = findViewById(R.id.rvEditFriends)
        btnAddFriend = findViewById(R.id.btnAddFriend)

        rvEditFriends.layoutManager = LinearLayoutManager(this)

        val trip = intent.getSerializableExtra("trip") as? Trip ?: return
        tripId = trip.id
        createdBy = trip.createdBy
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        adapter = EditFriendsAdapter(friendsList, currentUserId) { friend ->
            confirmAndRemoveFriend(friend)
        }

        rvEditFriends.adapter = adapter

        loadFriends()

        btnAddFriend.setOnClickListener {
            showAddFriendDialog()
        }
    }

    private fun loadFriends() {
        val tripRef = FirebaseDatabase.getInstance().getReference("trips")
            .child(createdBy)
            .child(tripId)

        tripRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                friendsList.clear()
                val trip = snapshot.getValue(Trip::class.java)
                trip?.members?.forEach { uid ->
                    fetchUserEmail(uid) { email ->
                        val friend = Friend(uid = uid, name = email)
                        friendsList.add(friend)
                        adapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@EditFriendsActivity, "Failed to load friends", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchUserEmail(uid: String, onResult: (String) -> Unit) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
        userRef.child("email").get().addOnSuccessListener { snapshot ->
            val email = snapshot.value as? String ?: uid
            onResult(email)
        }
    }

    private fun confirmAndRemoveFriend(friend: Friend) {
        AlertDialog.Builder(this)
            .setTitle("Remove Friend")
            .setMessage("Are you sure you want to remove ${friend.name} from the trip?")
            .setPositiveButton("Yes") { _, _ -> removeFriend(friend.name) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeFriend(email: String) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.get().addOnSuccessListener { snapshot ->
            val userUid = snapshot.children.firstOrNull {
                it.child("email").value == email
            }?.key

            if (userUid != null) {
                val tripRef = FirebaseDatabase.getInstance().getReference("trips")
                    .child(createdBy)
                    .child(tripId)
                    .child("members")

                tripRef.get().addOnSuccessListener { membersSnap ->
                    val currentList = membersSnap.children.mapNotNull { it.getValue(String::class.java) }.toMutableList()
                    currentList.remove(userUid)
                    tripRef.ref.setValue(currentList)

                    // Also remove from userTrips
                    val userTripsRef = FirebaseDatabase.getInstance().getReference("userTrips")
                        .child(userUid)
                        .child(tripId)
                    userTripsRef.removeValue()

                    loadFriends()
                    Toast.makeText(this, "Friend removed", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddFriendDialog() {
        val input = EditText(this)
        input.hint = "Enter friend's email"

        AlertDialog.Builder(this)
            .setTitle("Add Friend")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isNotEmpty()) {
                    addFriendByEmail(email)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addFriendByEmail(email: String) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.get().addOnSuccessListener { snapshot ->
            val userUid = snapshot.children.firstOrNull {
                it.child("email").value == email
            }?.key

            if (userUid != null) {
                val tripRef = FirebaseDatabase.getInstance().getReference("trips")
                    .child(createdBy)
                    .child(tripId)
                    .child("members")

                tripRef.get().addOnSuccessListener { membersSnap ->
                    val currentList = membersSnap.children.mapNotNull { it.getValue(String::class.java) }.toMutableList()
                    if (!currentList.contains(userUid)) {
                        currentList.add(userUid)
                        tripRef.setValue(currentList)

                        // Update /userTrips
                        val userTripsRef = FirebaseDatabase.getInstance().getReference("userTrips")
                            .child(userUid)
                            .child(tripId)
                        userTripsRef.setValue(true)

                        loadFriends()
                        Toast.makeText(this, "Friend added", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "User already in trip", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
