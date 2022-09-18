package com.example.myapplication.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.eclipse.paho.client.mqttv3.MqttMessage

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "No data"
    }
    val text: LiveData<String> = _text

    fun meow(xx: String){
        _text.value = xx + " Watt";
    }

    private val _text2 = MutableLiveData<String>().apply {
        value = "No data"
    }
    val text2: LiveData<String> = _text2
}