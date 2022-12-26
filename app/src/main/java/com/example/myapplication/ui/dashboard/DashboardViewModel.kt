package com.example.myapplication.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DashboardViewModel : ViewModel() {

    private val _text = MutableLiveData<String>()
    val text: LiveData<String> = _text

    fun postText(message: String, nostatus: Boolean){
        if(nostatus){
            _text.value = "$message Watt"
        }else{
            _text.value = message
        }
    }
}