package com.example.myapplication.ui.dashboard.tabs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.BarData

class DashboardTabMonthViewModel : ViewModel(){
    private val _barChartData = MutableLiveData<BarData>()
    private val _textPriceLast = MutableLiveData<Pair<String,Int?>>().apply {
        value = Pair("-,--€", null)
    }
    private val _textPriceBeforeLast = MutableLiveData<String>().apply {
        value = "-,--€"
    }
    private val _textPriceChosen = MutableLiveData<String>().apply {
        value = "-,--€"
    }
    private val _textPowerChosen = MutableLiveData<String>().apply {
        value = "-,--€"
    }
    private val _textChosenDays = MutableLiveData<String>()
    val textPriceLast: LiveData<Pair<String, Int?>> = _textPriceLast
    val textPriceBeforeLast: LiveData<String> = _textPriceBeforeLast
    val barChartData: LiveData<BarData> = _barChartData
    val textPriceChosen: LiveData<String> = _textPriceChosen
    val textPowerChosen: LiveData<String> = _textPowerChosen
    val textChosenDays: LiveData<String> = _textChosenDays
    fun priceLastPost(onedaypower: String, change: Int?){
        _textPriceLast.postValue(Pair(onedaypower, change))
    }
    fun priceBeforeLastPost(onedaypower: String){
        _textPriceBeforeLast.postValue(onedaypower)
    }
    fun barChartDataPost(onedaypower: BarData){
        _barChartData.postValue(onedaypower)
    }
    fun textPriceChosenPost(onedaypower: String){
        _textPriceChosen.postValue(onedaypower)
    }
    fun textPowerChosenPost(onedaypower: String){
        _textPowerChosen.postValue(onedaypower)
    }
    fun textChosenDaysPost(onedaypower: String){
        _textChosenDays.postValue(onedaypower)
    }
}