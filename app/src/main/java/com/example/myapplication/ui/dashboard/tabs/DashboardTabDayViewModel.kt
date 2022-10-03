package com.example.myapplication.ui.dashboard.tabs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.BarData

class DashboardTabDayViewModel : ViewModel() {
    private val _barChartData = MutableLiveData<BarData?>().apply {
        value = null
    }
    private val _textPriceYesterday = MutableLiveData<String>().apply {
        value = "-,--€"
    }
    private val _textPriceBeforeYesterday = MutableLiveData<String>().apply {
        value = "-,--€"
    }
    private val _textPriceChosen = MutableLiveData<String>().apply {
        value = "-,--€"
    }
    private val _textPowerChosen = MutableLiveData<String>().apply {
        value = "-,--€"
    }
    val textPriceYesterday: LiveData<String> = _textPriceYesterday
    val textPriceBeforeYesterday: LiveData<String> = _textPriceBeforeYesterday
    val barChartData: LiveData<BarData?> = _barChartData
    val textPriceChosen: LiveData<String> = _textPriceChosen
    val textPowerChosen: LiveData<String> = _textPowerChosen
    fun priceYesterdayPost(onedaypower: String){
        _textPriceYesterday.postValue(onedaypower)
    }
    fun priceBeforeYesterdayPost(onedaypower: String){
        _textPriceBeforeYesterday.postValue(onedaypower)
    }
    fun barChartDataPost(onedaypower: BarData?){
        _barChartData.postValue(onedaypower)
    }
    fun textPriceChosenPost(onedaypower: String){
        _textPriceChosen.postValue(onedaypower)
    }
    fun textPowerChosenPost(onedaypower: String){
        _textPowerChosen.postValue(onedaypower)
    }
}