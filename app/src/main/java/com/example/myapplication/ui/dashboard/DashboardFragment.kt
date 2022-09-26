package com.example.myapplication.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import kotlinx.serialization.json.Json
import com.example.myapplication.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.*
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textDashboard
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        val chart = binding.chartTest
        val chartHours = binding.barChartTest
        val client = OkHttpClient()
        val time = System.currentTimeMillis()
        Log.i("OkHttp", "http://10.0.0.26:8080/api/v1/all/?time_start=${time - 30 * 60 * 1000}&time_end=$time")
        val request = Request.Builder()
            .url("http://10.0.0.26:8080/api/v1/all/?time_start=${time - 30 * 60 * 1000}&time_end=$time")
            .build()
        client.newCall(request).enqueue(object:Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OkHttp", "OkHttp is not OK" + e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let {
                    val resString = it.string()
                    Log.i("OkHttp", resString)
                    val resJson = Json.parseToJsonElement(resString)
                    val data = resJson.jsonObject["data"]?.jsonArray
                    val entries:ArrayList<Entry> = ArrayList()
                    val diff = (data?.get(0)?.jsonObject?.get("time").toString().dropLast(8) + "00000000").toLong()
                    data?.stream()?.forEach { entry ->
                        entries.add(Entry((entry.jsonObject["time"].toString().toLong()-diff).toFloat(), entry.jsonObject["Momentanleistung"].toString().toFloat()))
                    }

                    val themeColor = TypedValue()
                    val dataSet = LineDataSet(entries, "Momentanleistung")
                    requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorOnBackground, themeColor, true)
                    dataSet.color = resources.getColor(R.color.purple_500, requireContext().theme)
                    dataSet.valueTextColor = themeColor.data
                    dataSet.circleRadius = 1F
                    chart.xAxis.textColor = themeColor.data
                    chart.legend.textColor = themeColor.data
                    chart.axisLeft.textColor = themeColor.data
                    chart.xAxis.valueFormatter = object:ValueFormatter() {
                        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                            return Date(value.toLong() + diff).toString().substring(10,19);
                        }
                    }
                    chart.axisRight.isEnabled = false
                    chart.description.isEnabled = false
                    chart.data = LineData(dataSet)
                    chart.invalidate()
                }

            }

        })
        val requestHours = Request.Builder()
            .url("http://10.0.0.26:8080/api/v1/hourly/?time_start=${time - 25 * 60 * 60 * 1000}&time_end=$time")
            .build()
        client.newCall(requestHours).enqueue(object:Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OkHttp", "OkHttp is not OK" + e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let {
                    val resString = it.string()
                    Log.i("OkHttp", resString)
                    val resJson = Json.parseToJsonElement(resString)
                    val data = resJson.jsonObject["data"]?.jsonArray
                    val entries:ArrayList<BarEntry> = ArrayList()
                    val diff = (data?.get(0)?.jsonObject?.get("timeStart").toString().dropLast(8)).toLong()
                    data?.stream()?.forEach { entry ->
                        Log.d("OkHttp", entry.jsonObject["timeStart"].toString() + " " + ((entry.jsonObject["timeStart"].toString().toLong()-diff)).toString())
                        entries.add(BarEntry((entry.jsonObject["timeStart"].toString().toLong()-diff).toFloat(), entry.jsonObject["Momentanleistung"].toString().toFloat()))
                    }

                    val themeColor = TypedValue()
                    val dataSet = BarDataSet(entries, "Momentanleistung")
                    requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorOnBackground, themeColor, true)
                    dataSet.color = resources.getColor(R.color.purple_500, requireContext().theme)
                    dataSet.valueTextColor = themeColor.data
                    dataSet.barBorderColor = resources.getColor(R.color.purple_500, requireContext().theme)
                    dataSet.barBorderWidth = 10F
                    chartHours.xAxis.textColor = themeColor.data
                    chartHours.setFitBars(true)
                    chartHours.legend.textColor = themeColor.data
                    chartHours.axisLeft.textColor = themeColor.data
                    chartHours.xAxis.valueFormatter = object:ValueFormatter() {
                        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                            return Date(value.toLong() + diff).toString().substring(10,16);
                        }
                    }
                    chartHours.axisRight.isEnabled = false
                    chartHours.description.isEnabled = false
                    chartHours.data = BarData(dataSet)
                    chartHours.invalidate()
                }

            }

        })

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}