package com.example.myapplication.ui.dashboard.tabs

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentDashboardTabDayBinding
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.*
import java.io.IOException
import java.util.*

/**
 * A simple [Fragment] subclass.
 * Use the [DashboardTabDay.newInstance] factory method to
 * create an instance of this fragment.
 */
class DashboardTabDay : Fragment() {
    private lateinit var mContext: Context
    private var _binding: FragmentDashboardTabDayBinding? = null

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
        _binding = FragmentDashboardTabDayBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val chartHours = binding.barChartTest
        val client = OkHttpClient()
        val time = System.currentTimeMillis()
        val requestHours = Request.Builder()
            .url("http://10.0.0.26:8080/api/v1/hourly/?time_start=${time - 25 * 60 * 60 * 1000}&time_end=$time")
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
                    val data = resJson.jsonObject["data"]?.jsonArray
                    val entries: ArrayList<BarEntry> = ArrayList()
                    val diff = (data?.get(0)?.jsonObject?.get("timeStart").toString().dropLast(8)).toLong()
                    data?.stream()?.forEach { entry ->
                        Log.d("OkHttp", entry.jsonObject["timeStart"].toString() + " " + ((entry.jsonObject["timeStart"].toString().toLong()-diff)).toString())
                        entries.add(BarEntry((entry.jsonObject["timeStart"].toString().toLong()-diff).toFloat(), entry.jsonObject["Momentanleistung"].toString().toFloat()))
                    }

                    val themeColor = TypedValue()
                    val primaryColor = TypedValue()
                    TypedValue()
                    val dataSet = BarDataSet(entries, "Momentanleistung")
                    mContext.theme.resolveAttribute(com.google.android.material.R.attr.colorOnBackground, themeColor, true)
                    mContext.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, primaryColor, true)
                    dataSet.color = primaryColor.data
                    dataSet.valueTextColor = themeColor.data
                    dataSet.barBorderColor = primaryColor.data
                    dataSet.barBorderWidth = 10F
                    chartHours.xAxis.textColor = themeColor.data
                    chartHours.setFitBars(true)
                    chartHours.legend.textColor = themeColor.data
                    chartHours.axisLeft.textColor = themeColor.data
                    chartHours.xAxis.valueFormatter = object: ValueFormatter() {
                        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                            return Date(value.toLong() + diff).toString().substring(10,16)
                        }
                    }
                    chartHours.axisRight.isEnabled = false
                    chartHours.description.isEnabled = false
                    chartHours.data = BarData(dataSet)
                    chartHours.invalidate()
                }

            }

        })
        // Inflate the layout for this fragment
        return root
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            DashboardTabDay()
    }
}