package com.example.myapplication.ui.dashboard.tabs

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentDashboardTab30MinBinding
import com.example.myapplication.ui.dashboard.DashboardFragment
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.*
import java.io.IOException
import java.util.*

class DashboardTab30Min : Fragment() {
    private lateinit var mContext: Context
    private var _binding: FragmentDashboardTab30MinBinding? = null
    private val binding get() = _binding!!
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardTab30MinBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val chart = binding.chartTest
        val client = OkHttpClient()
        val currentTime = System.currentTimeMillis()
        val dashboardTab30MinViewModel = ViewModelProvider(this)[DashboardTab30MinViewModel::class.java]
        var chartXAxisDiff:Long? = null
        var currentTotalPower = 0L
        val sharedPreferences = mContext.getSharedPreferences(
            "com.mas.smartmeter.mqttpreferences",
            Context.MODE_PRIVATE
        )
        dashboardTab30MinViewModel.textAverage.observe(viewLifecycleOwner) {
            binding.textAverage.text = it
        }
        dashboardTab30MinViewModel.textLow.observe(viewLifecycleOwner) {
            binding.textLow.text = it
        }
        dashboardTab30MinViewModel.textPeak.observe(viewLifecycleOwner) {
            binding.textPeak.text = it
        }
        if(!URLUtil.isValidUrl(sharedPreferences.getString("database_host", ""))) {
            Toast.makeText(mContext, mContext.getString(R.string.database_host_not_selected), Toast.LENGTH_LONG).show()
            return root
        }
        val request = Request.Builder()
            .url("${sharedPreferences.getString("database_host", "")}:${sharedPreferences.getInt("database_port", 1)}/api/v1/all/?time_start=${currentTime - 30 * 60 * 1000}&time_end=$currentTime")
            .addHeader("Authorization", sharedPreferences.getString("database_api_token", "")!!)
            .build()
        try {
            client.newCall(request).enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("OkHttp", e.message.toString())
                }
                override fun onResponse(call: Call, response: Response) {
                    response.body?.let {
                        if(response.code != 200) {
                            when (response.code) {
                                401 -> Toast.makeText(mContext, mContext.getString(R.string.unauthorized), Toast.LENGTH_SHORT).show()
                                500 -> Toast.makeText(mContext, mContext.getString(R.string.internal_server_error), Toast.LENGTH_SHORT).show()
                                else -> Toast.makeText(mContext, mContext.getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
                            }
                            return
                        }
                        val data = Json.parseToJsonElement(it.string()).jsonObject["data"]?.jsonArray
                        val chartEntries: ArrayList<Entry> = ArrayList()
                        chartXAxisDiff = data?.get(0)?.jsonObject?.get("time").toString().toLong()
                        var totalPowerUsage = 0L
                        data?.stream()?.forEach { entry ->
                            chartEntries.add(Entry((entry.jsonObject["time"].toString().toLong()- chartXAxisDiff!!).toFloat(), entry.jsonObject["Momentanleistung"].toString().toFloat()))
                            totalPowerUsage += entry.jsonObject["Momentanleistung"].toString().toLong()
                        }
                        val themeColor = TypedValue()
                        val primaryColor = TypedValue()
                        val dataSet = LineDataSet(chartEntries, mContext.getString(R.string.watt))
                        currentTotalPower = totalPowerUsage
                        dashboardTab30MinViewModel.textAveragePost("%dW".format((totalPowerUsage / chartEntries.count()).toInt()))
                        dashboardTab30MinViewModel.textPeakPost("%dW".format(dataSet.yMax.toInt()))
                        dashboardTab30MinViewModel.textLowPost("%dW".format(dataSet.yMin.toInt()))
                        mContext.theme.resolveAttribute(com.google.android.material.R.attr.colorOnBackground, themeColor, true)
                        mContext.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, primaryColor, true)
                        dataSet.color = primaryColor.data
                        dataSet.valueTextColor = themeColor.data
                        dataSet.circleRadius = 1F
                        chart.xAxis.textColor = themeColor.data
                        chart.xAxis.valueFormatter = object: ValueFormatter() {
                            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                                return Date(value.toLong() + chartXAxisDiff!!).toString().substring(10,19)
                            }
                        }
                        chart.legend.textColor = themeColor.data
                        chart.axisLeft.textColor = themeColor.data
                        chart.axisRight.setDrawGridLines(false)
                        chart.axisRight.setDrawLabels(false)
                        chart.description.isEnabled = false
                        chart.data = LineData(dataSet)
                        chart.invalidate()
                    }

                }

            })
        } catch (e:Error) {
            Log.e("DashboardTab30Min", e.toString())
        }
        DashboardFragment.currentMqttData.observe(viewLifecycleOwner) { timeValuePair ->
            if(timeValuePair != null && chartXAxisDiff != null) {
                if(chart.data.dataSetCount > 0){
                    val dataSet = chart.data.getDataSetByIndex(0)
                    if(dataSet.entryCount > 0) {
                        if(dataSet.getEntryForIndex(dataSet.entryCount-1).x.toLong() + chartXAxisDiff!! < timeValuePair.first - 10 * 10000) {
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.dashboard_tab_fragment, newInstance())
                                .commit()
                        }
                        currentTotalPower -= dataSet.getEntryForIndex(0).y.toLong()
                        currentTotalPower += timeValuePair.second.toLong()
                        dataSet.removeEntry(0)
                        dataSet.addEntry(Entry((timeValuePair.first-chartXAxisDiff!!).toFloat(), timeValuePair.second))
                        chart.data = LineData(dataSet)
                        chart.invalidate()
                        dashboardTab30MinViewModel.textPeakPost("%dW".format(dataSet.yMax.toInt()))
                        dashboardTab30MinViewModel.textLowPost("%dW".format(dataSet.yMin.toInt()))
                        dashboardTab30MinViewModel.textAveragePost("%dW".format((currentTotalPower / dataSet.entryCount).toInt()))
                    }
                }
            }
        }
        return root
    }
    companion object {
        @JvmStatic
        fun newInstance() =
            DashboardTab30Min()
    }
}