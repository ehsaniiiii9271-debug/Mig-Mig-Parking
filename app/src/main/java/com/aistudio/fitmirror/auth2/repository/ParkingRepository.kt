package com.aistudio.fitmirror.auth2.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object ParkingRepository {
    private val _walletBalance = MutableLiveData<Double>(0.0)
    val walletBalance: LiveData<Double> = _walletBalance

    fun rechargeWallet(amount: Double) {
        val current = _walletBalance.value ?: 0.0
        _walletBalance.value = current + amount
    }

    fun processPayment(amount: Double): Boolean {
        val current = _walletBalance.value ?: 0.0
        if (current >= amount) {
            _walletBalance.value = current - amount
            return true
        }
        return false
    }
}