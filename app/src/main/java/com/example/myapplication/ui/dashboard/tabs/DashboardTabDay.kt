package com.example.myapplication.ui.dashboard.tabs

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var dashboardTabDayViewModel: DashboardTabDayViewModel
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardTabDayBinding.inflate(inflater, container, false)
        dashboardTabDayViewModel = ViewModelProvider(this)[DashboardTabDayViewModel::class.java]
        dashboardTabDayViewModel.textPriceYesterday.observe(viewLifecycleOwner) {
            binding.textStrompreisGestern.text = it
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
        dashboardTabDayViewModel.barChartData.observe(viewLifecycleOwner) {
            binding.barChartDay.data = it
            binding.barChartDay.invalidate()
        }
        val root: View = binding.root
        val chartHours = binding.barChartDay
        val time = System.currentTimeMillis()
        val timeStart = time - 25 * 60 * 60 * 1000
        refreshChart(chartHours, Pair(timeStart, time))
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateBeforeYesterday = dateFormatter.format(time - 2 * 24 * 60 * 60 * 1000)
        val dateYesterday = dateFormatter.format(time - 1 * 24 * 60 * 60 * 1000)
        val requestLastDays = Request.Builder()
            .url("http://10.0.0.26:8080/api/v1/daily/?day_start=$dateBeforeYesterday&day_end=$dateYesterday")
            .build()
        OkHttpClient().newCall(requestLastDays).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OkHttp", "OkHttp is not OK" + e.message)
            }
            override fun onResponse(call: Call, response: Response) {
                response.body?.let {
                    val resString = it.string()
                    val resJson = Json.parseToJsonElement(resString)
                    val sharedPreferences = mContext.getSharedPreferences(
                        "com.mas.smartmeter.mqttpreferences",
                        Context.MODE_PRIVATE
                    )
                    val strompreis = sharedPreferences.getFloat("strompreis", 0F)
                    if (response.code == 200) {
                        val number = resJson.jsonObject["found"]?.toString()?.toInt()
                        if (number != null && number > 0) {
                            var verbrauchVorgestern:Float? = null
                            for(value in 0 until number) {
                                val obj = resJson.jsonObject["data"]?.jsonArray?.get(value)?.jsonObject
                                val day = obj?.get("day").toString().replace("\"", "")
                                val totalstromverbrauch = obj?.get("Gesamtleistung").toString().toFloat()
                                val preis = "%.2f".format((totalstromverbrauch/100000)*strompreis)
                                if(day == dateYesterday) {
                                    if(verbrauchVorgestern != null) {
                                        val changePercentage = (100 - ((verbrauchVorgestern/totalstromverbrauch)*100)).roundToInt()
                                        val changeString =
                                            if(changePercentage > 0) "+%d%%".format(changePercentage)
                                            else "%d%%".format(changePercentage)
                                        dashboardTabDayViewModel.priceYesterdayPost("${preis}€ ($changeString)")
                                    } else {
                                        dashboardTabDayViewModel.priceYesterdayPost("${preis}€")
                                    }
                                } else if(day == dateBeforeYesterday) {
                                    dashboardTabDayViewModel.priceBeforeYesterdayPost("${preis}€")
                                    verbrauchVorgestern = totalstromverbrauch
                                }

                            }
                        }
                    }
                }
            }
        })
        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText("Woche auswählen")
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
        return root
    }
    private fun refreshChart(chartHours: BarChart, dateRange:Pair<Long, Long>) {
        val client = OkHttpClient()
        val dateFormatterUI = SimpleDateFormat("dd.MM.", Locale.getDefault())
        val showDate = dateFormatterUI.format(dateRange.first).let { first ->
            dateFormatterUI.format(dateRange.second-1).let { second ->
                if(first == second) first
                else "$first - $second"
            }
        }
        binding.textChartRange.text =  showDate
        val requestHours = Request.Builder()
            .url("http://10.0.0.26:8080/api/v1/hourly/?time_start=${dateRange.first}&time_end=${dateRange.second}")
            .build()
        client.newCall(requestHours).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OkHttp", "OkHttp is not OK" + e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let {
                    val resString = it.string()
                    Log.i("OkHttp", resString)
                    val resJson = Json.parseToJsonElement(resString)
                    val timeFormatterUI = SimpleDateFormat("HH:mm", Locale.getDefault())
                    if (response.code == 200) {
                        val number = resJson.jsonObject["found"]?.toString()?.toInt()
                        val entries: ArrayList<BarEntry> = ArrayList()
                        if (number != null && number > 0) {
                            val data = resJson.jsonObject["data"]!!.jsonArray
                            val startDate =
                                data[0].jsonObject["timeStart"].toString().toLong()
                            var gesamtverbrauch = 0f
                            for(entry in 0 until number) {
                                val leistung = data[entry].jsonObject["Momentanleistung"].toString().toFloat()
                                entries.add(BarEntry(entry.toFloat(), leistung))
                                gesamtverbrauch+=leistung
                            }
                            chartHours.xAxis.valueFormatter = object: ValueFormatter() {
                                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                                    return if (value < number && value>=0) timeFormatterUI.format(value.roundToInt() * 60 * 60 * 1000 + startDate) else ""
                                }
                            }
                            val sharedPreferences = mContext.getSharedPreferences(
                                "com.mas.smartmeter.mqttpreferences",
                                Context.MODE_PRIVATE
                            )
                            val strompreis = sharedPreferences.getFloat("strompreis", 0F)
                            val preis = "%.2f€".format((gesamtverbrauch/100000)*strompreis)
                            val verbrauch = "%.1fkWh".format(gesamtverbrauch/1000)
                            dashboardTabDayViewModel.textPowerChosenPost(verbrauch)
                            dashboardTabDayViewModel.textPriceChosenPost(preis)
                        } else return
                        val dataSet = BarDataSet(entries, "Wattstunden")
                        ChartUtil.formatBarChart(mContext, number, chartHours, dataSet)
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