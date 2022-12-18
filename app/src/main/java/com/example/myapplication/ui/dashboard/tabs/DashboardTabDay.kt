package com.example.myapplication.ui.dashboard.tabs

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentDashboardTabDayBinding
import com.example.myapplication.util.ChartUtil
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class DashboardTabDay : Fragment() {
    private lateinit var mContext: Context
    private var _binding: FragmentDashboardTabDayBinding? = null
    private val binding get() = _binding!!
    private var currentStartTime = 0L
    private val okHttpClient = OkHttpClient()
    private lateinit var dashboardTabDayViewModel: DashboardTabDayViewModel
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }
    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardTabDayBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val chart = binding.barChartDay
        dashboardTabDayViewModel = ViewModelProvider(this)[DashboardTabDayViewModel::class.java]
        var currentTime = System.currentTimeMillis()
        val timeZoneOffset = TimeZone.getDefault().getOffset(currentTime)
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var timeMidnightToday = (currentTime + timeZoneOffset) / 86400000L * 86400000L - timeZoneOffset
        val timeMidnight2DaysAgo = dateFormatter.format(currentTime - 2 * 24 * 60 * 60 * 1000)
        val timeMidnightYesterday = dateFormatter.format(currentTime - 1 * 24 * 60 * 60 * 1000)
        val sharedPreferences = mContext.getSharedPreferences(
            "com.mas.smartmeter.mqttpreferences",
            Context.MODE_PRIVATE
        )
        dashboardTabDayViewModel.textPriceYesterday.observe(viewLifecycleOwner) {
            if(it.second == null) {
                binding.preisGesternCard.setCardBackgroundColor(mContext.getColor(R.color.orange_700))
                binding.preisVorgesternCard.setCardBackgroundColor(mContext.getColor(R.color.orange_700))
                binding.textStrompreisGestern.text = it.first
                return@observe
            }
            val changeString =
                if (it.second!! > 999) "+${getString(R.string.a_lot)}"
                else if (it.second!! > 0) "+%d%%".format(
                    it.second
                )
                else if (it.second!! < -999) "-${getString(R.string.a_lot)}"
                else "%d%%".format(it.second)
            binding.textStrompreisGestern.text = "${it.first} ($changeString)"
            if(it.second!! <0) {
                binding.preisGesternCard.setCardBackgroundColor(mContext.getColor(R.color.light_green_700))
                binding.preisVorgesternCard.setCardBackgroundColor(mContext.getColor(R.color.red_700))
            }
            else if(it.second!! >0){
                binding.preisGesternCard.setCardBackgroundColor(mContext.getColor(R.color.red_700))
                binding.preisVorgesternCard.setCardBackgroundColor(mContext.getColor(R.color.light_green_700))
            }
            else {
                binding.preisGesternCard.setCardBackgroundColor(mContext.getColor(R.color.orange_700))
                binding.preisVorgesternCard.setCardBackgroundColor(mContext.getColor(R.color.orange_700))
            }
        }
        dashboardTabDayViewModel.textPriceBeforeYesterday.observe(viewLifecycleOwner) {
            binding.textStrompreisVorgestern.text = it
        }
        dashboardTabDayViewModel.textPowerChosen.observe(viewLifecycleOwner) {
            binding.textLeistungAuswahl.text = it
        }
        dashboardTabDayViewModel.textPriceChosen.observe(viewLifecycleOwner) {
            binding.textKostenAuswahl.text = it
        }
        dashboardTabDayViewModel.textChosenDays.observe(viewLifecycleOwner) {
            binding.textTagAuswahl.text = it
        }
        dashboardTabDayViewModel.barChartData.observe(viewLifecycleOwner) {
            binding.barChartDay.data = it
            binding.barChartDay.invalidate()
        }
        dashboardTabDayViewModel.textChartRange.observe(viewLifecycleOwner) {
            binding.textChartRange.text = it
        }
        if(!URLUtil.isValidUrl(sharedPreferences.getString("database_host", ""))) {
            Toast.makeText(mContext, mContext.getString(R.string.database_host_not_selected), Toast.LENGTH_LONG).show()
            return root
        }
        try {
            refreshChart(chart, Pair(timeMidnightToday, currentTime))
            updateLastDaysDisplay(timeMidnight2DaysAgo, timeMidnightYesterday)
        } catch (e:Error) {
            Log.e("DashboardTabDay", e.toString())
        }

        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.choose_date_title))
                .setCalendarConstraints(
                    CalendarConstraints.Builder()
                    .setEnd(MaterialDatePicker.thisMonthInUtcMilliseconds())
                    .setValidator(DateValidatorPointBackward.now())
                    .build()
                )
                .build()
        binding.datePickerDay.setOnClickListener {
            datePicker.addOnPositiveButtonClickListener { selectedTime ->
                dashboardTabDayViewModel.barChartDataPost(BarData())
                val selectedTimeTimezoneAdjusted = selectedTime - TimeZone.getDefault().getOffset(selectedTime)
                try {
                    refreshChart(chart, Pair(selectedTimeTimezoneAdjusted, selectedTimeTimezoneAdjusted + 24 * 60 * 60 * 1000))
                } catch (e:Error) {
                    Log.e("DashboardTabDay", e.toString())
                }
            }
            datePicker.show(parentFragmentManager, "")
        }
        binding.datePickerReset.setOnClickListener {
            currentTime = System.currentTimeMillis()
            timeMidnightToday = (currentTime + timeZoneOffset) / 86400000L * 86400000L - timeZoneOffset
            dashboardTabDayViewModel.barChartDataPost(BarData())
            refreshChart(chart, Pair(timeMidnightToday, currentTime))
        }
        binding.dateMoveLeft.setOnClickListener {
            okHttpClient.dispatcher.queuedCalls().forEach { it.cancel() }
            okHttpClient.dispatcher.runningCalls().forEach { it.cancel() }
            val endTime = currentStartTime
            val startTime = endTime - 24 * 60 * 60 * 1000
            try{
                refreshChart(chart, Pair(startTime,endTime))
            }catch (e:Error) {
                Log.e("DashboardTabDay", e.toString())
            }
        }
        binding.dateMoveRight.setOnClickListener {
            okHttpClient.dispatcher.queuedCalls().forEach { it.cancel() }
            okHttpClient.dispatcher.runningCalls().forEach { it.cancel() }
            val startTime = currentStartTime + 24 * 60 * 60 * 1000
            val endTime = startTime + 24 * 60 * 60 * 1000
            if(startTime > System.currentTimeMillis()) return@setOnClickListener
            try{
                refreshChart(chart, Pair(startTime,endTime))
            }catch (e:Error) {
                Log.e("DashboardTabDay", e.toString())
            }
        }
        return root
    }

    private fun updateLastDaysDisplay(
        dateBeforeYesterday: String?,
        dateYesterday: String?
    ) {
        val sharedPreferences = mContext.getSharedPreferences(
            "com.mas.smartmeter.mqttpreferences",
            Context.MODE_PRIVATE
        )
        val request = Request.Builder()
            .url("${sharedPreferences.getString("database_host", "")}:${sharedPreferences.getInt("database_port", 1)}/api/v1/daily/?day_start=$dateBeforeYesterday&day_end=$dateYesterday")
            .addHeader("Authorization", sharedPreferences.getString("database_api_token", "")!!)
            .build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OkHttp", e.message.toString())
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let {
                    val resJson = Json.parseToJsonElement(it.string())
                    val powerPrice = sharedPreferences.getFloat("strompreis", 0F)
                    if (response.code == 200) {
                        val resultCount = resJson.jsonObject["found"]!!.toString().toInt()
                        if (resultCount > 0) {
                            var powerUsageDayBeforeYesterday: Float? = null
                            for (resultIndex in 0 until resultCount) {
                                val resultJsonObject =
                                    resJson.jsonObject["data"]!!.jsonArray[resultIndex].jsonObject
                                val resultDay = resultJsonObject["day"].toString().replace("\"", "")
                                val resultTotalPowerUsage =
                                    resultJsonObject["Gesamtleistung"].toString().toFloat()
                                val preis =
                                    "%.2f".format((resultTotalPowerUsage / 100000) * powerPrice)
                                if (resultDay == dateYesterday) {
                                    if (powerUsageDayBeforeYesterday != null) {

                                        val changePercentage =
                                            if(powerUsageDayBeforeYesterday <= 0) 0 else (((resultTotalPowerUsage / powerUsageDayBeforeYesterday) * 100)-100).roundToInt()
                                        dashboardTabDayViewModel.priceYesterdayPost(
                                            "${preis}€",
                                            changePercentage
                                        )
                                    } else {
                                        dashboardTabDayViewModel.priceYesterdayPost("${preis}€", null)
                                    }
                                } else if (resultDay == dateBeforeYesterday) {
                                    dashboardTabDayViewModel.priceBeforeYesterdayPost("${preis}€")
                                    powerUsageDayBeforeYesterday = resultTotalPowerUsage
                                }

                            }
                        }
                    } else {
                        when (response.code) {
                            401 -> Toast.makeText(mContext, mContext.getString(R.string.unauthorized), Toast.LENGTH_SHORT).show()
                            500 -> Toast.makeText(mContext, mContext.getString(R.string.internal_server_error), Toast.LENGTH_SHORT).show()
                            else -> Toast.makeText(mContext, mContext.getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    private fun refreshChart(chartHours: BarChart, dateRange:Pair<Long, Long>) {
        val sharedPreferences = mContext.getSharedPreferences(
            "com.mas.smartmeter.mqttpreferences",
            Context.MODE_PRIVATE
        )
        currentStartTime = dateRange.first
        val timeFormatterUI = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dayOfWeekFormatterUI = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormatterUI = SimpleDateFormat("dd.MM.", Locale.getDefault())
        val timeMidnightToday = dateRange.first
        val showDate =
            dateFormatterUI.format(timeMidnightToday).let { displayDate ->
                if(displayDate == dateFormatterUI.format(System.currentTimeMillis())) getString(R.string.today)
                else dayOfWeekFormatterUI.format(timeMidnightToday) + " " + displayDate
            }
        dashboardTabDayViewModel.textChartRangePost(showDate)
        val request = Request.Builder()
            .url("${sharedPreferences.getString("database_host", "")}:${sharedPreferences.getInt("database_port", 1)}/api/v1/hourly/?time_start=${dateRange.first}&time_end=${dateRange.second}")
            .addHeader("Authorization", sharedPreferences.getString("database_api_token", "")!!)
            .build()
        okHttpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OkHttp", "OkHttp is not OK" + e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let {
                    val resJson = Json.parseToJsonElement(it.string())
                    if (response.code == 200) {
                        val resultCount = resJson.jsonObject["found"]?.toString()?.toInt()
                        val chartEntries: ArrayList<BarEntry> = ArrayList()
                        if (resultCount != null && resultCount > 0) {
                            dashboardTabDayViewModel.textChosenDaysPost("$showDate (${mContext.getString(R.string.chosen)})")
                            val resultData = resJson.jsonObject["data"]!!.jsonArray
                            var totalPowerUsage = 0f
                            var emptyDays = 0
                            for(resultIndex in 0 until resultCount) {
                                while(dateRange.first + (resultIndex + emptyDays) * 1 * 60 * 60 * 1000 != resultData[resultIndex].jsonObject["timeStart"].toString().toLong()) {
                                    chartEntries.add(BarEntry((resultIndex+emptyDays).toFloat(), 0F))
                                    emptyDays++
                                }
                                val resultPower = resultData[resultIndex].jsonObject["Momentanleistung"].toString().toFloat()
                                chartEntries.add(BarEntry((resultIndex+emptyDays).toFloat() + 0.5f, resultPower))
                                totalPowerUsage+=resultPower
                            }
                            chartHours.xAxis.valueFormatter = object: ValueFormatter() {
                                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                                    return if (value < 25 && value>=0) timeFormatterUI.format(value.roundToInt() * 60 * 60 * 1000 + dateRange.first) else ""
                                }
                            }
                            val powerPrice = sharedPreferences.getFloat("strompreis", 0F)
                            val totalPowerPrice = "%.2f€".format((totalPowerUsage/100000)*powerPrice)
                            val totalPowerUsageKWh = "%.1fkWh".format(totalPowerUsage/1000)
                            dashboardTabDayViewModel.textPowerChosenPost(totalPowerUsageKWh)
                            dashboardTabDayViewModel.textPriceChosenPost(totalPowerPrice)
                        } else {
                            dashboardTabDayViewModel.textChosenDaysPost("$showDate (${getString(R.string.chosen)})")
                            dashboardTabDayViewModel.textPowerChosenPost("-,--kWh")
                            dashboardTabDayViewModel.textPriceChosenPost("-,--€")
                            dashboardTabDayViewModel.barChartDataPost(BarData())
                            return@let
                        }
                        val dataSet = BarDataSet(chartEntries, mContext.getString(R.string.watthours))
                        ChartUtil.formatBarChart(mContext, 24, chartHours, dataSet, false)
                        dashboardTabDayViewModel.barChartDataPost(BarData(dataSet))
                    }

                }
            }
        })
    }
    companion object {
        @JvmStatic
        fun newInstance() =
            DashboardTabDay()
    }
}