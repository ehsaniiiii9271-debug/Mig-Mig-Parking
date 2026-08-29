package com.aistudio.fitmirror.auth2.repository

import android.util.Log
import com.aistudio.fitmirror.auth2.model.ParkingRequest
import com.aistudio.fitmirror.auth2.model.RequestStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

object FirebaseManager {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private const val COLLECTION_NAME = "parking_requests"

    fun signUpOrLogin(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { signInError ->
                if (signInError is FirebaseAuthInvalidUserException) {
                    // User does not exist, try creating one
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { signUpError ->
                            onFailure(signUpError.localizedMessage ?: "Registration failed")
                        }
                } else {
                    // Password might be wrong or other error
                    onFailure(signInError.localizedMessage ?: "Authentication failed")
                }
            }
    }

    fun sendDriverRequest(lat: Double, lng: Double, email: String, onSuccess: () -> Unit) {
        val data = hashMapOf(
            "lat" to lat,
            "lng" to lng,
            "email" to email,
            "type" to "driver",
            "status" to RequestStatus.PENDING.name,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection(COLLECTION_NAME).add(data).addOnSuccessListener { onSuccess() }
    }

    fun reportSpot(lat: Double, lng: Double, email: String, onSuccess: (String) -> Unit) {
        val data = hashMapOf(
            "lat" to lat,
            "lng" to lng,
            "email" to email,
            "type" to "finder",
            "status" to RequestStatus.PENDING.name,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection(COLLECTION_NAME).add(data).addOnSuccessListener { onSuccess(it.id) }
    }

    fun observeRequests(onUpdate: (List<ParkingRequest>) -> Unit): ListenerRegistration {
        return db.collection(COLLECTION_NAME)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val requests = snapshot?.mapNotNull { doc ->
                    try {
                        ParkingRequest(
                            id = doc.id,
                            lat = doc.getDouble("lat") ?: 0.0,
                            lng = doc.getDouble("lng") ?: 0.0,
                            driverEmail = doc.getString("email") ?: "",
                            status = RequestStatus.valueOf(doc.getString("status") ?: "PENDING")
                        )
                    } catch (ex: Exception) {
                        null
                    }
                } ?: emptyList()
                onUpdate(requests)
            }
    }

    fun updateStatus(requestId: String, status: RequestStatus) {
        db.collection(COLLECTION_NAME).document(requestId).update("status", status.name)
    }
}