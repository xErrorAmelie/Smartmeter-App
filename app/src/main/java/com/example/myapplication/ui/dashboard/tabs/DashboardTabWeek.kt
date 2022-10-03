package com.example.myapplication.ui.dashboard.tabs

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.util.toKotlinPair
import com.example.myapplication.databinding.FragmentDashboardTabWeekBinding
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
import kotlin.math.abs
import kotlin.math.roundToInt

class DashboardTabWeek : Fragment() {
    private lateinit var mContext: Context
    private var _binding: FragmentDashboardTabWeekBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardTabWeekBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val chartWeek = binding.barChartWeek
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var datePreviousWeek = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
        var dateYesterday = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000
        refreshChart(chartWeek, Pair(datePreviousWeek, dateYesterday))
        val datePicker =
            MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Woche auswählen")
                .setCalendarConstraints(CalendarConstraints.Builder()
                    .setEnd(MaterialDatePicker.thisMonthInUtcMilliseconds())
                    .setValidator(DateValidatorPointBackward.now())
                    .build()
                )
                .build()
        binding.datePickerWeek.setOnClickListener {
            datePicker.addOnPositiveButtonClickListener {
                if(abs(it.first - it.second) > 7 * 24 * 60 * 60 * 1000) {
                    Toast.makeText(mContext, "Bitte max. 7 Tage auswählen", Toast.LENGTH_LONG)
                        .show()
                    return@addOnPositiveButtonClickListener
                }
                chartWeek.data = null
                chartWeek.invalidate()
                refreshChart(chartWeek, it.toKotlinPair())

            }
            datePicker.show(parentFragmentManager, "")
        }
        binding.datePickerReset.setOnClickListener {
            datePreviousWeek = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
            dateYesterday = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000
            chartWeek.data = null
            chartWeek.invalidate()
            refreshChart(chartWeek, Pair(datePreviousWeek, dateYesterday))
        }
        return root
    }
    private fun refreshChart(chartWeek:BarChart, dateRange:Pair<Long, Long>) {
        val client = OkHttpClient()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateFormatterUI = SimpleDateFormat("dd.MM.", Locale.getDefault())
        val datePreviousWeek = dateFormatter.format(dateRange.first)
        val dateYesterday = dateFormatter.format(dateRange.second)
        binding.textChartRange.text = dateFormatterUI.format(dateRange.first) + " - " + dateFormatterUI.format(dateRange.second)
        Log.d("DashboardTabDay", "http://10.0.0.26:8080/api/v1/daily/?day_start=$datePreviousWeek&day_end=$dateYesterday")
        val requestHours = Request.Builder()
            .url("http://10.0.0.26:8080/api/v1/daily/?day_start=$datePreviousWeek&day_end=$dateYesterday")
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
                    if (response.code == 200) {
                        val number = resJson.jsonObject["found"]?.toString()?.toInt()
                        val entries: ArrayList<BarEntry> = ArrayList()
                        if (number != null && number > 0) {
                            val data = resJson.jsonObject["data"]!!.jsonArray
                            val startDate =
                                dateFormatter.parse(data[0].jsonObject["day"].toString().replace("\"", ""))!!.time
                            for(entry in 0 until number) {
                                entries.add(BarEntry(entry.toFloat(), data[entry].jsonObject["Gesamtleistung"].toString().toFloat()))
                            }
                            chartWeek.xAxis.valueFormatter = object: ValueFormatter() {
                                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                                    return if (value < number && value>=0) dateFormatterUI.format(value.roundToInt() * 24 * 60 * 60 * 1000 + startDate) else ""
                                }
                            }
                        } else return
                        val themeColor = TypedValue()
                        val primaryColor = TypedValue()
                        val dataSet = BarDataSet(entries, "Wattstunden")
                        mContext.theme.resolveAttribute(com.google.android.material.R.attr.colorOnBackground, themeColor, true)
                        mContext.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, primaryColor, true)
                        dataSet.color = primaryColor.data
                        dataSet.valueTextColor = themeColor.data
                        dataSet.barBorderColor = primaryColor.data
                        dataSet.valueTextSize = 10F
                        chartWeek.xAxis.textColor = themeColor.data
                        chartWeek.xAxis.axisMaximum = number.toFloat()-0.5f
                        chartWeek.xAxis.axisMinimum = -0.5f
                        chartWeek.xAxis.labelCount = number - 1
                        chartWeek.setVisibleXRange(0f, number.toFloat())
                        chartWeek.setFitBars(true)
                        chartWeek.legend.textColor = themeColor.data
                        chartWeek.axisLeft.textColor = themeColor.data
                        chartWeek.axisRight.isEnabled = false
                        chartWeek.description.isEnabled = false
                        chartWeek.data = BarData(dataSet)
                        chartWeek.invalidate()
                    }

                }

            }

        })
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            DashboardTabWeek()
    }
}