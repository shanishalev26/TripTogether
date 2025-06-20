package com.example.triptogether.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.triptogether.R
import com.example.triptogether.TripSummaryActivity
import com.example.triptogether.model.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class TripAdapter(
    private val tripList: List<Trip>
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tripImage: ImageView = itemView.findViewById(R.id.ivTripImage)
        val tripName: TextView = itemView.findViewById(R.id.tvTripName)
        val tripDates: TextView = itemView.findViewById(R.id.tvTripDates)
        val tripCountry: TextView = itemView.findViewById(R.id.tvCountryName)
        val favoriteIcon: ImageView = itemView.findViewById(R.id.ivFavorite)
        val unreadDot: View = itemView.findViewById(R.id.unreadDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = tripList[position]

        holder.tripName.text = trip.name
        holder.tripDates.text = buildString {
        append(trip.startDate)
        append(" - ")
        append(trip.endDate)
    }
        holder.tripCountry.text = trip.country

        if (!trip.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(trip.imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.tripImage)
        } else {
            holder.tripImage.setImageResource(R.drawable.default_trip_image)
        }



        holder.unreadDot.visibility = View.GONE // Reset visibility due to view recycling

        // Favorites
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val favoriteRef = FirebaseDatabase.getInstance()
            .getReference("favorites")
            .child(uid)
            .child(trip.id)

        favoriteRef.get().addOnSuccessListener { snapshot ->
            val isFavorite = snapshot.exists()
            holder.favoriteIcon.setImageResource(
                if (isFavorite) R.drawable.ic_star_filled else R.drawable.ic_fav
            )
        }

        holder.favoriteIcon.setOnClickListener {
            val tripRef = FirebaseDatabase.getInstance().getReference("trips")
                .child(uid)
                .child(trip.id)

            favoriteRef.get().addOnSuccessListener { snapshot ->
                val isFavorite = snapshot.exists()
                val newFavoriteState = !isFavorite

                tripRef.child("isFavorite").setValue(newFavoriteState)

                if (newFavoriteState) {
                    favoriteRef.setValue(trip)
                } else {
                    favoriteRef.removeValue()
                }

                holder.favoriteIcon.setImageResource(
                    if (newFavoriteState) R.drawable.ic_star_filled else R.drawable.ic_fav
                )
            }
        }

        // Navigate to TripSummary
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, TripSummaryActivity::class.java)
            intent.putExtra("trip", trip)
            context.startActivity(intent)
        }

        // 🔔 Check for unread chat messages
        val lastSeenRef = FirebaseDatabase.getInstance()
            .getReference("lastSeenChat")
            .child(uid)
            .child(trip.id)

        val chatRef = FirebaseDatabase.getInstance()
            .getReference("chats")
            .child(trip.id)

        lastSeenRef.get().addOnSuccessListener { lastSeenSnap ->
            val lastSeen = lastSeenSnap.getValue(Long::class.java) ?: 0

            chatRef.orderByChild("timestamp").limitToLast(1).get()
                .addOnSuccessListener { chatSnap ->
                    var hasNew = false
                    for (msg in chatSnap.children) {
                        val timestamp = msg.child("timestamp").getValue(Long::class.java) ?: 0
                        if (timestamp > lastSeen) {
                            hasNew = true
                            break
                        }
                    }
                    holder.unreadDot.visibility = if (hasNew) View.VISIBLE else View.GONE
                }
                .addOnFailureListener {
                    holder.unreadDot.visibility = View.GONE
                }
        }.addOnFailureListener {
            holder.unreadDot.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = tripList.size
}
