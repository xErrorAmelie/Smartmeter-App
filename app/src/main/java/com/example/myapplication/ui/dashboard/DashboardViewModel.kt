package com.example.myapplication.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DashboardViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "No Data"
    }
    val text: LiveData<String> = _text

    fun meow(currentpower: String){
        _text.value = "$currentpower Watt"
    }
    fun meowPost(onedaypower: String){
        _text.postValue("$onedaypower Euro")
    }
}