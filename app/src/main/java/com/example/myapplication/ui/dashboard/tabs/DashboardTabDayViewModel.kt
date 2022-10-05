package com.example.myapplication.ui.dashboard.tabs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.BarData

class DashboardTabDayViewModel : ViewModel() {
    private val _barChartData = MutableLiveData<BarData>()
    private val _textPriceYesterday = MutableLiveData<Pair<String,Int?>>().apply {
        value = Pair("-,--€", null)
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
    private val _textChartRange = MutableLiveData<String>()
    private val _textChosenDays = MutableLiveData<String>()
    val textPriceYesterday: LiveData<Pair<String,Int?>> = _textPriceYesterday
    val textPriceBeforeYesterday: LiveData<String> = _textPriceBeforeYesterday
    val barChartData: LiveData<BarData> = _barChartData
    val textPriceChosen: LiveData<String> = _textPriceChosen
    val textPowerChosen: LiveData<String> = _textPowerChosen
    val textChosenDays: LiveData<String> = _textChosenDays
    val textChartRange: LiveData<String> = _textChartRange
    fun priceYesterdayPost(onedaypower: String,change: Int?){
        _textPriceYesterday.postValue(Pair(onedaypower, change))
    }
    fun priceBeforeYesterdayPost(onedaypower: String){
        _textPriceBeforeYesterday.postValue(onedaypower)
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
    fun textChartRangePost(onedaypower: String){
        _textChartRange.postValue(onedaypower)
    }
}