package com.example.triptogether.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.triptogether.R
import com.example.triptogether.model.ActivityItem

class ActivityAdapter(
    private val onEditOrDelete: (ActivityItem, String) -> Unit
) : RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {

    private val activities = mutableListOf<ActivityItem>()
    private val activityKeys = mutableListOf<String>()

    //
    fun updateData(newActivities: List<ActivityItem>, newKeys: List<String>) {
        activities.clear()
        activities.addAll(newActivities)

        activityKeys.clear()
        activityKeys.addAll(newKeys)

        notifyDataSetChanged()
    }

    inner class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeText: TextView = itemView.findViewById(R.id.tvTime)
        val emojiText: TextView = itemView.findViewById(R.id.tvEmoji)
        val descriptionText: TextView = itemView.findViewById(R.id.tvDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity_plan, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val activity = activities[position]
        holder.timeText.text = activity.time
        holder.emojiText.text = activity.emoji
        holder.descriptionText.text = activity.description

        val key = activityKeys.getOrNull(position)
        if (key != null) {
            holder.itemView.setOnLongClickListener {
                onEditOrDelete(activity, key)
                true
            }
        }
    }

    fun getCurrentItems(): List<Pair<ActivityItem, String>> {
        return activities.zip(activityKeys)
    }

    override fun getItemCount(): Int = activities.size
}
