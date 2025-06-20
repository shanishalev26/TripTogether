package com.example.triptogether.model

import java.io.Serializable

data class Trip(
    val id: String = "",
    val name: String = "",
    val country: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val members: List<String> = emptyList(),
    val createdBy: String = "",
    var isFavorite: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUrl: String? = null
) : Serializable

