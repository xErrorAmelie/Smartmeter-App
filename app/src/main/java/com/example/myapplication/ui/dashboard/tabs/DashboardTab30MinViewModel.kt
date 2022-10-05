package com.example.myapplication.ui.dashboard.tabs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DashboardTab30MinViewModel : ViewModel() {
    private val _textAverage = MutableLiveData<String>().apply {
        value = "-,--€"
    }
    private val _textPeak = MutableLiveData<String>().apply {
        value = "-,--€"
    }
    private val _textLow = MutableLiveData<String>().apply {
        value = "-,--€"
    }
    val textAverage: LiveData<String> = _textAverage
    val textPeak: LiveData<String> = _textPeak
    val textLow: LiveData<String> = _textLow
    fun textAveragePost(onedaypower: String){
        _textAverage.postValue(onedaypower)
    }
    fun textPeakPost(onedaypower: String){
        _textPeak.postValue(onedaypower)
    }
    fun textLowPost(onedaypower: String){
        _textLow.postValue(onedaypower)
    }
}