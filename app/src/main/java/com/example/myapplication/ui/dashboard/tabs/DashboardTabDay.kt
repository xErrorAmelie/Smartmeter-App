package com.example.myapplication.ui.dashboard.tabs

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        dashboardTabDayViewModel = ViewModelProvider(this)[DashboardTabDayViewModel::class.java]
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
        val root: View = binding.root
        val chartHours = binding.barChartDay
        var time = Date().time
        val offset = TimeZone.getDefault().getOffset(time)
        var timeStart = (time + offset) / 86400000L * 86400000L - offset
        refreshChart(chartHours, Pair(timeStart, time))
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateBeforeYesterday = dateFormatter.format(time - 2 * 24 * 60 * 60 * 1000)
        val dateYesterday = dateFormatter.format(time - 1 * 24 * 60 * 60 * 1000)
        updateLastDaysDisplay(dateBeforeYesterday, dateYesterday)
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
            datePicker.addOnPositiveButtonClickListener {

                dashboardTabDayViewModel.barChartDataPost(null)
                val timeAdjusted = it - TimeZone.getDefault().getOffset(it)
                refreshChart(chartHours, Pair(timeAdjusted, timeAdjusted+24 * 60 * 60 * 1000))

            }
            datePicker.show(parentFragmentManager, "")
        }
        binding.datePickerReset.setOnClickListener {
            time = Date().time
            timeStart = (time + offset) / 86400000L * 86400000L - offset
            chartHours.data = BarData()
            chartHours.invalidate()
            refreshChart(chartHours, Pair(timeStart, time))
        }
        binding.dateMoveLeft.setOnClickListener {
            okHttpClient.dispatcher.queuedCalls().forEach { it.cancel() }
            okHttpClient.dispatcher.runningCalls().forEach { it.cancel() }
            val endTime = currentStartTime
            val startTime = endTime - 24 * 60 * 60 * 1000
            refreshChart(chartHours, Pair(startTime,endTime))
        }
        binding.dateMoveRight.setOnClickListener {
            okHttpClient.dispatcher.queuedCalls().forEach { it.cancel() }
            okHttpClient.dispatcher.runningCalls().forEach { it.cancel() }
            val startTime = currentStartTime + 24 * 60 * 60 * 1000
            val endTime = startTime + 24 * 60 * 60 * 1000
            if(startTime > System.currentTimeMillis()) return@setOnClickListener
            refreshChart(chartHours, Pair(startTime,endTime))
        }
        return root
    }

    private fun updateLastDaysDisplay(
        dateBeforeYesterday: String?,
        dateYesterday: String?
    ) {
        val sharedPreferences = requireContext().getSharedPreferences(
            "com.mas.smartmeter.mqttpreferences",
            Context.MODE_PRIVATE
        )
        val requestLastDays = Request.Builder()
            .url("${sharedPreferences.getString("database_host", "http://127.0.0.1")}:${sharedPreferences.getInt("database_port", 1)}/api/v1/daily/?day_start=$dateBeforeYesterday&day_end=$dateYesterday")
            .addHeader("Authorization", sharedPreferences.getString("database_api_token", "")!!)
            .build()
        OkHttpClient().newCall(requestLastDays).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OkHttp", "OkHttp is not OK" + e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let {
                    val resString = it.string()
                    val resJson = Json.parseToJsonElement(resString)
                    val strompreis = sharedPreferences.getFloat("strompreis", 0F)
                    if (response.code == 200) {
                        val number = resJson.jsonObject["found"]?.toString()?.toInt()
                        if (number != null && number > 0) {
                            var verbrauchVorgestern: Float? = null
                            for (value in 0 until number) {
                                val obj =
                                    resJson.jsonObject["data"]?.jsonArray?.get(value)?.jsonObject
                                val day = obj?.get("day").toString().replace("\"", "")
                                val totalstromverbrauch =
                                    obj?.get("Gesamtleistung").toString().toFloat()
                                val preis =
                                    "%.2f".format((totalstromverbrauch / 100000) * strompreis)
                                if (day == dateYesterday) {
                                    if (verbrauchVorgestern != null) {
                                        val changePercentage =
                                            (((totalstromverbrauch / verbrauchVorgestern) * 100)-100).roundToInt()
                                        dashboardTabDayViewModel.priceYesterdayPost(
                                            "${preis}€",
                                            changePercentage
                                        )
                                    } else {
                                        dashboardTabDayViewModel.priceYesterdayPost("${preis}€", null)
                                    }
                                } else if (day == dateBeforeYesterday) {
                                    dashboardTabDayViewModel.priceBeforeYesterdayPost("${preis}€")
                                    verbrauchVorgestern = totalstromverbrauch
                                }

                            }
                        }
                    }
                }
            }
        })
    }

    private fun refreshChart(chartHours: BarChart, dateRange:Pair<Long, Long>) {
        val sharedPreferences = requireContext().getSharedPreferences(
            "com.mas.smartmeter.mqttpreferences",
            Context.MODE_PRIVATE
        )
        currentStartTime = dateRange.first
        val timeFormatterUI = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dayOfWeekFormatterUI = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormatterUI = SimpleDateFormat("dd.MM.", Locale.getDefault())
        val date = dateRange.first+1
        val showDate =
            dateFormatterUI.format(date).let { displayDate ->
                if(displayDate == dateFormatterUI.format(System.currentTimeMillis())) getString(R.string.today)
                else dayOfWeekFormatterUI.format(date) + " " + displayDate
            }
        binding.textChartRange.text = showDate
        val requestHours = Request.Builder()
            .url("${sharedPreferences.getString("database_host", "http://127.0.0.1")}:${sharedPreferences.getInt("database_port", 1)}/api/v1/hourly/?time_start=${dateRange.first}&time_end=${dateRange.second}")
            .addHeader("Authorization", sharedPreferences.getString("database_api_token", "")!!)
            .build()
        okHttpClient.newCall(requestHours).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OkHttp", "OkHttp is not OK" + e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let {
                    val resString = it.string()
                    Log.i("OkHttp", resString)
                    val resJson = Json.parseToJsonElement(resString)
                    if (response.code == 200) {
                        val number = resJson.jsonObject["found"]?.toString()?.toInt()
                        val entries: ArrayList<BarEntry> = ArrayList()
                        if (number != null && number > 0) {
                            dashboardTabDayViewModel.textChosenDaysPost("$showDate (${getString(R.string.chosen)})")
                            val data = resJson.jsonObject["data"]!!.jsonArray
                            var gesamtverbrauch = 0f
                            var emptyDays = 0
                            for(entry in 0 until number) {
                                while(dateRange.first + (entry + emptyDays) * 1 * 60 * 60 * 1000 != data[entry].jsonObject["timeStart"].toString().toLong()) {
                                    entries.add(BarEntry((entry+emptyDays).toFloat(), 0F))
                                    emptyDays++
                                }
                                val leistung = data[entry].jsonObject["Momentanleistung"].toString().toFloat()
                                entries.add(BarEntry((entry+emptyDays).toFloat() +0.5f, leistung))
                                gesamtverbrauch+=leistung
                            }
                            chartHours.xAxis.valueFormatter = object: ValueFormatter() {
                                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                                    return if (value < 25 && value>=0) timeFormatterUI.format(value.roundToInt() * 60 * 60 * 1000 + dateRange.first) else ""
                                }
                            }
                            val strompreis = sharedPreferences.getFloat("strompreis", 0F)
                            val preis = "%.2f€".format((gesamtverbrauch/100000)*strompreis)
                            val verbrauch = "%.1fkWh".format(gesamtverbrauch/1000)
                            dashboardTabDayViewModel.textPowerChosenPost(verbrauch)
                            dashboardTabDayViewModel.textPriceChosenPost(preis)
                        } else {
                            dashboardTabDayViewModel.textChosenDaysPost("$showDate (${getString(R.string.chosen)})")
                            dashboardTabDayViewModel.textPowerChosenPost("-,--kWh")
                            dashboardTabDayViewModel.textPriceChosenPost("-,--€")
                            dashboardTabDayViewModel.barChartDataPost(BarData())
                            return
                        }
                        val dataSet = BarDataSet(entries, getString(R.string.watthours))
                        ChartUtil.formatBarChart(mContext, 24, chartHours, dataSet, false)
                        dataSet.setDrawValues(false)
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