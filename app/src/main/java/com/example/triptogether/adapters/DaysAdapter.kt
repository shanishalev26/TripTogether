package com.example.triptogether.adapters

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.triptogether.R

class DaysAdapter(
    private val days: List<Pair<String, String>>, // Pair<"Day 1", "19 Jun">
    private val onDaySelected: (Int) -> Unit
) : RecyclerView.Adapter<DaysAdapter.DayViewHolder>() {

    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val (dayNumber, dayDate) = days[position]
        holder.tvDayNumber.text = dayNumber
        holder.tvDayDate.text = dayDate

        if (position == selectedPosition) {
            holder.tvDayNumber.setTypeface(null, Typeface.BOLD)
            holder.itemView.setBackgroundResource(R.drawable.bg_day_selected)
        } else {
            holder.tvDayNumber.setTypeface(null, Typeface.NORMAL)
            holder.itemView.setBackgroundResource(R.drawable.bg_day_unselected)
        }

        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            onDaySelected(selectedPosition)
        }
    }

    override fun getItemCount(): Int = days.size

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDayNumber: TextView = itemView.findViewById(R.id.tvDayNumber)
        val tvDayDate: TextView = itemView.findViewById(R.id.tvDayDate)
    }
}
