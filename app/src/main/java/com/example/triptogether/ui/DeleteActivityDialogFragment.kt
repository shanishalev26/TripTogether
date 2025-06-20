package com.example.triptogether.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.firebase.database.FirebaseDatabase

class DeleteActivityDialogFragment(
    private val tripId: String,
    private val createdBy: String,
    private val dayIndex: Int,
    private val activityKey: String,
    private val onDeleted: () -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Delete Activity")
            .setMessage("Are you sure you want to delete this activity?")
            .setPositiveButton("Delete") { _, _ ->
                deleteActivity()
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    private fun deleteActivity() {
        val dayKey = "day$dayIndex"
        val ref = FirebaseDatabase.getInstance()
            .getReference("trips")
            .child(createdBy)
            .child(tripId)
            .child("plan")
            .child(dayKey)
            .child(activityKey)

        ref.removeValue().addOnSuccessListener {
            if (isAdded) {
                Toast.makeText(requireContext(), "Activity deleted", Toast.LENGTH_SHORT).show()
            }
            dismiss()
            onDeleted()
        }.addOnFailureListener {
            if (isAdded) {
                Toast.makeText(requireContext(), "Failed to delete", Toast.LENGTH_SHORT).show()
            }
        }
    }


}
