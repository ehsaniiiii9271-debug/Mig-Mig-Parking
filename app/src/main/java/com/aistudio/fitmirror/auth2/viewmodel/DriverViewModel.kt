package com.aistudio.fitmirror.auth2.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aistudio.fitmirror.auth2.repository.ParkingRepository

class DriverViewModel : ViewModel() {
    private val _isSignedUp = MutableLiveData(false)
    val isSignedUp: LiveData<Boolean> = _isSignedUp

    private val _isStarted = MutableLiveData(false)
    val isStarted: LiveData<Boolean> = _isStarted

    val walletBalance: LiveData<Double> = ParkingRepository.walletBalance

    fun signUp(email: String, carModel: String, initialRecharge: Double) {
        ParkingRepository.rechargeWallet(initialRecharge)
        _isSignedUp.value = true
    }

    fun start() = run { _isStarted.value = true }

    fun pay(amount: Double): Boolean = ParkingRepository.processPayment(amount)
}