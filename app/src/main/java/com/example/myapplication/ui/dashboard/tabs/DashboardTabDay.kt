package com.example.myapplication.ui.dashboard.tabs

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.databinding.FragmentDashboardTabDayBinding
import com.example.myapplication.ui.dashboard.DashboardViewModel
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
    private lateinit var dashboardViewModel:DashboardViewModel
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardTabDayBinding.inflate(inflater, container, false)
        val textView: TextView = binding.textViewStrompreis
        dashboardViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it.toString()
        }
        val root: View = binding.root
        val chartHours = binding.barChartDay
        val time = System.currentTimeMillis()
        val timeStart = time - 25 * 60 * 60 * 1000
        refreshChart(chartHours, Pair(timeStart, time))
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

                chartHours.data = null
                chartHours.invalidate()
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
                            var totalstromverbrauch = 0F
                            val sharedPreferences = mContext.getSharedPreferences(
                                "com.mas.smartmeter.mqttpreferences",
                                Context.MODE_PRIVATE
                            )
                            for(entry in 0 until number) {
                                val leistung = data[entry].jsonObject["Momentanleistung"].toString().toFloat()
                                totalstromverbrauch += leistung
                                entries.add(BarEntry(entry.toFloat(), leistung))
                            }
                            val strompreis = sharedPreferences.getFloat("strompreis", 0F)
                            dashboardViewModel.meowPost("%.2f".format((totalstromverbrauch/100000)*strompreis))
                            chartHours.xAxis.valueFormatter = object: ValueFormatter() {
                                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                                    return if (value < number && value>=0) timeFormatterUI.format(value.roundToInt() * 60 * 60 * 1000 + startDate) else ""
                                }
                            }
                        } else return
                        val dataSet = BarDataSet(entries, "Wattstunden")
                        ChartUtil.formatBarChart(mContext, number, chartHours, dataSet)
                        chartHours.data = BarData(dataSet)
                        chartHours.invalidate()
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