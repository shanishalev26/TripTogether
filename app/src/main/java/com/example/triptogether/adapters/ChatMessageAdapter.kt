package com.example.triptogether.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.triptogether.R
import com.example.triptogether.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class ChatMessageAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSender: TextView = view.findViewById(R.id.tvSender)
        val tvText: TextView = view.findViewById(R.id.tvText)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val msg = messages[position]
        holder.tvSender.text = msg.sender
        holder.tvText.text = msg.message
        holder.tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))

        val currentUser = FirebaseAuth.getInstance().currentUser?.email
        if (msg.sender == currentUser) {
            holder.itemView.setBackgroundResource(R.drawable.message_bubble_sent)
            holder.tvText.setTextColor(Color.WHITE)
            holder.tvSender.setTextColor(Color.WHITE)
            holder.tvTime.setTextColor(Color.WHITE)
        } else {
            holder.itemView.setBackgroundResource(R.drawable.message_bubble_bg)
        }
    }

    override fun getItemCount(): Int = messages.size
}
