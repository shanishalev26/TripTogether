package com.example.triptogether.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.triptogether.R
import com.example.triptogether.model.Friend

class EditFriendsAdapter(
    private val friends: List<Friend>,
    private val currentUserId: String,
    private val onDeleteClicked: (Friend) -> Unit
) : RecyclerView.Adapter<EditFriendsAdapter.FriendViewHolder>() {

    inner class FriendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProfile: ImageView = view.findViewById(R.id.ivProfile)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvRole: TextView = view.findViewById(R.id.tvRole)
        val ivDelete: ImageView = view.findViewById(R.id.ivDelete)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_edit_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        holder.tvName.text = friend.name
        holder.tvRole.text = ""

        val userRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(friend.uid)
        userRef.get().addOnSuccessListener { snapshot ->
            val role = snapshot.child("role").getValue(String::class.java) ?: ""
            val imageUrl = snapshot.child("imageUrl").getValue(String::class.java)
                ?: snapshot.child("photoUrl").getValue(String::class.java)

            holder.tvRole.text = role
            com.bumptech.glide.Glide.with(holder.itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_user_placeholder)
                .into(holder.ivProfile)
        }

        holder.ivDelete.visibility = if (friend.uid == currentUserId) View.GONE else View.VISIBLE
        holder.ivDelete.setOnClickListener {
            onDeleteClicked(friend)
        }
    }

    override fun getItemCount(): Int = friends.size
}
