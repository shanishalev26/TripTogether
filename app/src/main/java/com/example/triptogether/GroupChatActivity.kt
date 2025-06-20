package com.example.triptogether

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.triptogether.adapters.ChatMessageAdapter
import com.example.triptogether.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class GroupChatActivity : AppCompatActivity() {

    private lateinit var chatRef: DatabaseReference
    private lateinit var rvMessages: RecyclerView
    private lateinit var adapter: ChatMessageAdapter
    private val messageList = mutableListOf<ChatMessage>()

    private lateinit var tripId: String
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat)

        tripId = intent.getStringExtra("tripId") ?: return
        val tripName = intent.getStringExtra("tripName") ?: "Group Chat"
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        findViewById<TextView>(R.id.tvChatTitle).text = tripName

        rvMessages = findViewById(R.id.rvMessages)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)

        adapter = ChatMessageAdapter(messageList)
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = adapter

        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(tripId)

        // Load messages and then update lastSeen
        chatRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messageList.clear()
                for (child in snapshot.children) {
                    val msg = child.getValue(ChatMessage::class.java)
                    msg?.let { messageList.add(it) }
                }
                adapter.notifyDataSetChanged()
                rvMessages.scrollToPosition(messageList.size - 1)

                updateLastSeenTimestamp()
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            val user = FirebaseAuth.getInstance().currentUser
            if (text.isNotEmpty() && user != null) {
                val msg = ChatMessage(user.email ?: "", text)
                chatRef.push().setValue(msg)
                etMessage.text.clear()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::tripId.isInitialized && ::userId.isInitialized) {
            updateLastSeenTimestamp()
        }
    }

    private fun updateLastSeenTimestamp() {
        val lastSeenRef = FirebaseDatabase.getInstance()
            .getReference("lastSeenChat")
            .child(userId)
            .child(tripId)

        lastSeenRef.setValue(System.currentTimeMillis())
    }
}
