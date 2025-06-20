package com.example.triptogether

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.triptogether.adapters.FriendsAdapter
import com.example.triptogether.model.Friend
import com.example.triptogether.model.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FriendsActivity : AppCompatActivity() {

    private lateinit var rvFriends: RecyclerView
    private lateinit var friendAdapter: FriendsAdapter
    private val friendList = mutableListOf<Friend>()

    private lateinit var tvMainName: TextView
    private lateinit var tvMainRole: TextView
    private lateinit var ivMainUser: ImageView
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        rvFriends = findViewById(R.id.rvFriends)
        tvMainName = findViewById(R.id.tvMainName)
        tvMainRole = findViewById(R.id.tvMainRole)
        ivMainUser = findViewById(R.id.ivMainUser)
        rvFriends.layoutManager = LinearLayoutManager(this)

        friendAdapter = FriendsAdapter(friendList)
        rvFriends.adapter = friendAdapter

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        loadCurrentUserInfo(currentUserId)

        val tripId = intent.getStringExtra("tripId") ?: return
        val createdBy = intent.getStringExtra("createdBy") ?: return

        val btnEdit = findViewById<Button>(R.id.btnEditFriends)
        btnEdit.setOnClickListener {
            val trip = Trip(id = tripId, createdBy = createdBy)
            val intent = Intent(this, EditFriendsActivity::class.java)
            intent.putExtra("trip", trip)
            startActivity(intent)
        }

        val btnHomeFriend = findViewById<ImageView>(R.id.btnHomeFriend)
        btnHomeFriend.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

    }


    private fun loadFriendsList() {
        val tripId = intent.getStringExtra("tripId") ?: return
        val createdBy = intent.getStringExtra("createdBy") ?: return

        val dbRef = FirebaseDatabase.getInstance()
            .getReference("trips")
            .child(createdBy)
            .child(tripId)

        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val trip = snapshot.getValue(Trip::class.java)
                val members = trip?.members ?: emptyList()

                friendList.clear()

                for (uid in members) {
                    if (uid == currentUserId) continue
                    usersRef.child(uid).child("email").get().addOnSuccessListener { emailSnap ->
                        val email = emailSnap.getValue(String::class.java)
                        if (email != null) {
                            friendList.add(Friend(uid = uid, name = email))
                            friendAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FriendsActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }




    private fun loadCurrentUserInfo(uid: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                val role = snapshot.child("role").getValue(String::class.java) ?: ""
                val imageUrl = snapshot.child("imageUrl").getValue(String::class.java)

                tvMainName.text = buildString {
        append(firstName)
        append(" ")
        append(lastName)
    }
                tvMainRole.text = role

                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this@FriendsActivity)
                        .load(imageUrl)
                        .placeholder(R.drawable.sample_user_icon)
                        .circleCrop()
                        .into(ivMainUser)
                } else {
                    ivMainUser.setImageResource(R.drawable.sample_user_icon)
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    override fun onResume() {
        super.onResume()
        loadFriendsList()
    }


}