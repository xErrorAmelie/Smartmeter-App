package com.example.myapplication.ui.dashboard.tabs

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentDashboardTab30MinBinding
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.*
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
        _binding = FragmentDashboardTab30MinBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val chart = binding.chartTest
        val client = OkHttpClient()
        val time = System.currentTimeMillis()
        Log.i("OkHttp", "http://10.0.0.26:8080/api/v1/all/?time_start=${time - 30 * 60 * 1000}&time_end=$time")
        val request = Request.Builder()
            .url("http://10.0.0.26:8080/api/v1/all/?time_start=${time - 30 * 60 * 1000}&time_end=$time")
            .build()
        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OkHttp", "OkHttp is not OK" + e.message)
            }
            override fun onResponse(call: Call, response: Response) {
                response.body?.let {
                    val resString = it.string()
                    Log.i("OkHttp", resString)
                    val resJson = Json.parseToJsonElement(resString)
                    val data = resJson.jsonObject["data"]?.jsonArray
                    val entries: ArrayList<Entry> = ArrayList()
                    val diff = (data?.get(0)?.jsonObject?.get("time").toString().dropLast(8) + "00000000").toLong()
                    data?.stream()?.forEach { entry ->
                        entries.add(Entry((entry.jsonObject["time"].toString().toLong()-diff).toFloat(), entry.jsonObject["Momentanleistung"].toString().toFloat()))
                    }

                    val themeColor = TypedValue()
                    val primaryColor = TypedValue()
                    val dataSet = LineDataSet(entries, "Momentanleistung")
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
                            return Date(value.toLong() + diff).toString().substring(10,19)
                        }
                    }
                    chart.axisRight.isEnabled = false
                    chart.description.isEnabled = false
                    chart.data = LineData(dataSet)
                    chart.invalidate()
                }

            }

        })
        return root
    }
    companion object {
        @JvmStatic
        fun newInstance() =
            DashboardTab30Min()
    }
}