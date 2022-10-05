package com.example.myapplication.ui.dashboard.tabs

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        // Inflate the layout for this fragment
        val sharedPreferences = requireContext().getSharedPreferences(
            "com.mas.smartmeter.mqttpreferences",
            Context.MODE_PRIVATE
        )
        _binding = FragmentDashboardTab30MinBinding.inflate(inflater, container, false)
        val dashboardTab30MinViewModel = ViewModelProvider(this)[DashboardTab30MinViewModel::class.java]
        var diff:Long? = null
        var currentTotalWatt = 0L
        dashboardTab30MinViewModel.textAverage.observe(viewLifecycleOwner) {
            binding.textAverage.text = it
        }
        dashboardTab30MinViewModel.textLow.observe(viewLifecycleOwner) {
            binding.textLow.text = it
        }
        dashboardTab30MinViewModel.textPeak.observe(viewLifecycleOwner) {
            binding.textPeak.text = it
        }
        val root: View = binding.root
        val chart = binding.chartTest
        val client = OkHttpClient()
        val time = System.currentTimeMillis()
        val request = Request.Builder()
            .url("${sharedPreferences.getString("database_host", "http://127.0.0.1")}:${sharedPreferences.getInt("database_port", 1)}/api/v1/all/?time_start=${time - 30 * 60 * 1000}&time_end=$time")
            .addHeader("Authorization", sharedPreferences.getString("database_api_token", "")!!)
            .build()
        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OkHttp", "OkHttp is not OK" + e.message)
            }
            override fun onResponse(call: Call, response: Response) {
                response.body?.let {
                    if(response.code != 200) return
                    val resString = it.string()
                    Log.i("OkHttp", resString)
                    val resJson = Json.parseToJsonElement(resString)
                    val data = resJson.jsonObject["data"]?.jsonArray
                    val entries: ArrayList<Entry> = ArrayList()
                    diff = (data?.get(0)?.jsonObject?.get("time").toString().dropLast(8) + "00000000").toLong()
                    var totalwert = 0L
                    data?.stream()?.forEach { entry ->
                        entries.add(Entry((entry.jsonObject["time"].toString().toLong()- diff!!).toFloat(), entry.jsonObject["Momentanleistung"].toString().toFloat()))
                        totalwert += entry.jsonObject["Momentanleistung"].toString().toLong()
                    }
                    currentTotalWatt = totalwert
                    dashboardTab30MinViewModel.textAveragePost("%dW".format((totalwert / entries.count()).toInt()))
                    val themeColor = TypedValue()
                    val primaryColor = TypedValue()
                    val dataSet = LineDataSet(entries, getString(R.string.watt))
                    dashboardTab30MinViewModel.textPeakPost("%dW".format(dataSet.yMax.toInt()))
                    dashboardTab30MinViewModel.textLowPost("%dW".format(dataSet.yMin.toInt()))
                    mContext.theme.resolveAttribute(com.google.android.material.R.attr.colorOnBackground, themeColor, true)
                    mContext.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, primaryColor, true)
                    dataSet.color = primaryColor.data
                    dataSet.valueTextColor = themeColor.data
                    dataSet.circleRadius = 1F
                    chart.xAxis.textColor = themeColor.data
                    chart.legend.textColor = themeColor.data
                    chart.axisLeft.textColor = themeColor.data
                    chart.xAxis.valueFormatter = object: ValueFormatter() {
                        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                            return Date(value.toLong() + diff!!).toString().substring(10,19)
                        }
                    }
                    chart.axisRight.isEnabled = false
                    chart.description.isEnabled = false
                    chart.data = LineData(dataSet)
                    chart.invalidate()

                }

            }

        })
        DashboardFragment.textPriceYesterday.observe(viewLifecycleOwner) {
            if(it != null && diff != null) {
                val dataSet = chart.data.getDataSetByIndex(0)
                if(dataSet.getEntryForIndex(dataSet.entryCount-1).x.toLong() + diff!! < it.first - 10 * 10000) {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.dashboard_tab_fragment, newInstance())
                        .commit()
                }
                currentTotalWatt -= dataSet.getEntryForIndex(0).y.toLong()
                currentTotalWatt += it.second.toLong()
                dataSet.removeEntry(0)
                dataSet.addEntry(Entry((it.first-diff!!).toFloat(), it.second))
                chart.data = LineData(dataSet)
                chart.invalidate()
                dashboardTab30MinViewModel.textPeakPost("%dW".format(dataSet.yMax.toInt()))
                dashboardTab30MinViewModel.textLowPost("%dW".format(dataSet.yMin.toInt()))
                dashboardTab30MinViewModel.textAveragePost("%dW".format((currentTotalWatt / dataSet.entryCount).toInt()))
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