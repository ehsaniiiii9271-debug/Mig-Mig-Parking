package com.aistudio.fitmirror.auth2.model

data class ParkingRequest(
    val id: String,
    val lat: Double,
    val lng: Double,
    val driverEmail: String,
    val status: RequestStatus = RequestStatus.PENDING
)

enum class RequestStatus {
    PENDING, ACCEPTED, COMPLETED
}