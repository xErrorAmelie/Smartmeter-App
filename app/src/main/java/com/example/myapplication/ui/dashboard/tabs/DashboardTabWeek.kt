package com.example.myapplication.ui.dashboard.tabs

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentDashboardTabWeekBinding
import com.example.myapplication.util.ChartUtil
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.renderer.XAxisRenderer
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Utils
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.*
import java.util.*
import kotlin.math.roundToInt

class DashboardTabWeek : Fragment() {
    private lateinit var mContext: Context
    private var _binding: FragmentDashboardTabWeekBinding? = null
    private val binding get() = _binding!!
    private var currentStartTime = 0L
    private val okHttpClient = OkHttpClient()
    private lateinit var dashboardTabWeekViewModel: DashboardTabWeekViewModel
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }
    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardTabWeekBinding.inflate(inflater, container, false)
        dashboardTabWeekViewModel = ViewModelProvider(this)[DashboardTabWeekViewModel::class.java]
        dashboardTabWeekViewModel.textPriceLast.observe(viewLifecycleOwner) {
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
        dashboardTabWeekViewModel.textPriceBeforeLast.observe(viewLifecycleOwner) {
            binding.textStrompreisVorgestern.text = it
        }
        dashboardTabWeekViewModel.textPowerChosen.observe(viewLifecycleOwner) {
            binding.textLeistungAuswahl.text = it
        }
        dashboardTabWeekViewModel.textPriceChosen.observe(viewLifecycleOwner) {
            binding.textKostenAuswahl.text = it
        }
        dashboardTabWeekViewModel.textChosenDays.observe(viewLifecycleOwner) {
            binding.textTagAuswahl.text = it
        }
        dashboardTabWeekViewModel.barChartData.observe(viewLifecycleOwner) {
            binding.barChartWeek.data = it
            binding.barChartWeek.invalidate()
        }
        val root: View = binding.root
        val chartWeek = binding.barChartWeek
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val lastSundayDateTime = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SUNDAY))
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val lastSunday = "%04d-%02d-%02d".format(lastSundayDateTime.get(ChronoField.YEAR), lastSundayDateTime.get(ChronoField.MONTH_OF_YEAR), lastSundayDateTime.get(ChronoField.DAY_OF_MONTH))
        val lastSundayTime = dateFormatter.parse(lastSunday)?.time?.plus(TimeZone.getDefault().getOffset(System.currentTimeMillis()))
        val lastMondayTime = lastSundayTime?.minus(6 * 24 * 60 * 60 * 1000)
        val previousLastSundayTime = lastMondayTime?.minus(1 * 24 * 60 * 60 * 1000)
        val previousLastMondayTime = previousLastSundayTime?.minus(6 * 24 * 60 * 60 * 1000)
        val thisMondayTime = lastSundayTime!!.plus(1 * 24 * 60 * 60 * 1000)
        val thisSundayTime = lastSundayTime.plus(7 * 24 * 60 * 60 * 1000)
        updateLastWeeksDisplay(dateFormatter, lastMondayTime, lastSundayTime, previousLastMondayTime, previousLastSundayTime)
        refreshChart(chartWeek, Pair(thisMondayTime, thisSundayTime))
        binding.datePickerReset.setOnClickListener {
            refreshChart(chartWeek, Pair(thisMondayTime, thisSundayTime))
        }
        binding.dateMoveLeft.setOnClickListener {
            okHttpClient.dispatcher.queuedCalls().forEach { it.cancel() }
            okHttpClient.dispatcher.runningCalls().forEach { it.cancel() }
            val endTime = currentStartTime - 1 * 24 * 60 * 60 * 1000
            val startTime = endTime - 6 * 24 * 60 * 60 * 1000
            refreshChart(chartWeek, Pair(startTime,endTime))
        }
        binding.dateMoveRight.setOnClickListener {
            okHttpClient.dispatcher.queuedCalls().forEach { it.cancel() }
            okHttpClient.dispatcher.runningCalls().forEach { it.cancel() }
            val startTime = currentStartTime + 7 * 24 * 60 * 60 * 1000
            val endTime = startTime + 6 * 24 * 60 * 60 * 1000
            if(startTime > System.currentTimeMillis()) return@setOnClickListener
            refreshChart(chartWeek, Pair(startTime,endTime))
        }
        return root
    }
    private fun updateLastWeeksDisplay(
        dateFormatter: SimpleDateFormat,
        lastMondayTime: Long?,
        lastSundayTime: Long?,
        beforeLastMondayTime: Long?,
        beforeLastSundayTime: Long?,

    ) {
        val sharedPreferences = requireContext().getSharedPreferences(
            "com.mas.smartmeter.mqttpreferences",
            Context.MODE_PRIVATE
        )
        Log.d("a", "${sharedPreferences.getString("database_host", "http://127.0.0.1")}:${sharedPreferences.getInt("database_port", 1)}/api/v1/daily/?day_start=${dateFormatter.format(beforeLastMondayTime)}&day_end=${dateFormatter.format(beforeLastSundayTime)}")
        val requestBeforeLastDays = Request.Builder()
            .url(
                "${sharedPreferences.getString("database_host", "http://127.0.0.1")}:${sharedPreferences.getInt("database_port", 1)}/api/v1/daily/?day_start=${dateFormatter.format(beforeLastMondayTime)}&day_end=${dateFormatter.format(beforeLastSundayTime)}"
            )
            .addHeader("Authorization", sharedPreferences.getString("database_api_token", "")!!)
            .build()
        OkHttpClient().newCall(requestBeforeLastDays).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OkHttp", "OkHttp is not OK" + e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let {
                    val resString = it.string()
                    val resJson = Json.parseToJsonElement(resString)
                    val strompreis = sharedPreferences.getFloat("strompreis", 0F)
                    if (response.code == 200) {
                        val number = resJson.jsonObject["found"]!!.toString().toInt()
                        var gesamtverbrauch = 0F
                        if (number > 0) {
                            for (value in 0 until number) {
                                val obj =
                                    resJson.jsonObject["data"]!!.jsonArray[value].jsonObject
                                gesamtverbrauch += obj["Gesamtleistung"].toString().toFloat()

                            }
                            dashboardTabWeekViewModel.priceBeforeLastPost(
                                "%.2f€".format(((gesamtverbrauch / 100000) * strompreis))
                            )
                        }
                        val requestLastDays = Request.Builder()
                            .url(
                                "${sharedPreferences.getString("database_host", "http://127.0.0.1")}:${sharedPreferences.getInt("database_port", 1)}/api/v1/daily/?day_start=${
                                    dateFormatter.format(
                                        lastMondayTime
                                    )
                                    
                                }&day_end=${dateFormatter.format(lastSundayTime)}"
                            )
                            .addHeader("Authorization", sharedPreferences.getString("database_api_token", "")!!)
                            .build()
                        OkHttpClient().newCall(requestLastDays).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                Log.e("OkHttp", "OkHttp is not OK" + e.message)
                            }

                            override fun onResponse(call: Call, response: Response) {
                                response.body?.let { it2 ->
                                    val resString2 = it2.string()
                                    val resJson2 = Json.parseToJsonElement(resString2)
                                    if (response.code == 200) {
                                        val number2 = resJson2.jsonObject["found"]!!.toString().toInt()
                                        if (number2 > 0) {
                                            var gesamtverbrauch2 = 0F
                                            for (value in 0 until number2) {
                                                val obj =
                                                    resJson2.jsonObject["data"]!!.jsonArray[value].jsonObject
                                                gesamtverbrauch2 += obj["Gesamtleistung"].toString().toFloat()

                                            }
                                            val changePercentage =
                                                if(number>0)(((gesamtverbrauch2/gesamtverbrauch ) * 100)-100).roundToInt()
                                                else null
                                            val preis = "%.2f".format(((gesamtverbrauch2 / 100000) * strompreis))
                                            dashboardTabWeekViewModel.priceLastPost(
                                                "$preis€",
                                                changePercentage
                                            )
                                        }
                                    }
                                }
                            }
                        })
                    }
                }
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun refreshChart(chartWeek:BarChart, dateRange:Pair<Long, Long>) {
        val sharedPreferences = requireContext().getSharedPreferences(
            "com.mas.smartmeter.mqttpreferences",
            Context.MODE_PRIVATE
        )
        currentStartTime = dateRange.first
        val client = OkHttpClient()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateFormatterUI = SimpleDateFormat("dd.MM.", Locale.getDefault())
        val dayOfWeekFormatterUI = SimpleDateFormat("EEE", Locale.getDefault())
        val datePreviousWeek = dateFormatter.format(dateRange.first)
        val dateYesterday = dateFormatter.format(dateRange.second)
        binding.textChartRange.text = "${dateFormatterUI.format(dateRange.first)} - ${dateFormatterUI.format(dateRange.second)}"
        Log.d("DashboardTabDay", "${sharedPreferences.getString("database_host", "127.0.0.1")}:${sharedPreferences.getInt("database_port", 1)}/api/v1/daily/?day_start=$datePreviousWeek&day_end=$dateYesterday")
        val requestHours = Request.Builder()
            .url("${sharedPreferences.getString("database_host", "http://127.0.0.1")}:${sharedPreferences.getInt("database_port", 1)}/api/v1/daily/?day_start=$datePreviousWeek&day_end=$dateYesterday")
            .addHeader("Authorization", sharedPreferences.getString("database_api_token", "")!!)
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
                            dashboardTabWeekViewModel.textChosenDaysPost("${dayOfWeekFormatterUI.format(dateRange.first)} ${dateFormatterUI.format(dateRange.first)} - ${dayOfWeekFormatterUI.format(dateRange.second)} ${dateFormatterUI.format(dateRange.second)}")
                            var gesamtleistung = 0f
                            var emptyDays = 0
                            for(entry in 0 until number) {

                                while(dateFormatter.format(dateRange.first + (entry + emptyDays) * 24 * 60 * 60 * 1000) != data[entry].jsonObject["day"].toString().replace("\"", "")) {
                                    entries.add(BarEntry((entry+emptyDays).toFloat(), 0F))
                                    emptyDays++
                                }
                                val leistung = data[entry].jsonObject["Gesamtleistung"].toString().toFloat()
                                entries.add(BarEntry((entry+emptyDays).toFloat(), leistung))
                                gesamtleistung += leistung
                            }
                            val strompreis = sharedPreferences.getFloat("strompreis", 0F)
                            dashboardTabWeekViewModel.textPowerChosenPost("%.1fkWh".format(gesamtleistung/1000))
                            dashboardTabWeekViewModel.textPriceChosenPost("%.2f€".format((gesamtleistung/100000)*strompreis))
                            chartWeek.xAxis.valueFormatter = object: ValueFormatter() {
                                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                                    val time = value.roundToInt() * 24 * 60 * 60 * 1000+ dateRange.first
                                    return if (value < 8 && value>=0) dayOfWeekFormatterUI.format(time) + "\n" + dateFormatterUI.format(time) else " \n "
                                }
                            }
                        } else {
                            dashboardTabWeekViewModel.textChosenDaysPost("${dayOfWeekFormatterUI.format(dateRange.first)} ${dateFormatterUI.format(dateRange.first)} - ${dayOfWeekFormatterUI.format(dateRange.second)} ${dateFormatterUI.format(dateRange.second)}")
                            dashboardTabWeekViewModel.textPowerChosenPost("-,--kWh")
                            dashboardTabWeekViewModel.textPriceChosenPost("-,--€")
                            dashboardTabWeekViewModel.barChartDataPost(BarData())
                            return
                        }
                        val dataSet = BarDataSet(entries, getString(R.string.watthours))
                        ChartUtil.formatBarChart(mContext,  7, chartWeek, dataSet, true)
                        chartWeek.setXAxisRenderer(object:XAxisRenderer(chartWeek.viewPortHandler, chartWeek.xAxis, chartWeek.getTransformer(YAxis.AxisDependency.LEFT)) {
                            override fun drawLabel(
                                c: Canvas?,
                                formattedLabel: String?,
                                x: Float,
                                y: Float,
                                anchor: MPPointF?,
                                angleDegrees: Float
                            ) {
                                formattedLabel?.split("\n")?.let { lines ->
                                    Utils.drawXAxisValue(c, lines[0], x, y - mAxisLabelPaint.textSize, mAxisLabelPaint, anchor, angleDegrees)
                                    Utils.drawXAxisValue(c, lines[1], x, y, mAxisLabelPaint, anchor, angleDegrees)
                                }
                            }
                        })
                        chartWeek.extraTopOffset = 15f
                        dashboardTabWeekViewModel.barChartDataPost(BarData(dataSet))
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