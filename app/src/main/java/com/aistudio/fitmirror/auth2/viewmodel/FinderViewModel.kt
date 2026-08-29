package com.aistudio.fitmirror.auth2.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FinderViewModel : ViewModel() {
    private val _isSignedUp = MutableLiveData(false)
    val isSignedUp: LiveData<Boolean> = _isSignedUp

    fun signUp(email: String, cardNumber: String) {
        _isSignedUp.value = true
    }
}