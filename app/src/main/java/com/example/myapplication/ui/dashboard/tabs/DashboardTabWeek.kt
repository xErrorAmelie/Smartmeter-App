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
import android.webkit.URLUtil
import android.widget.Toast
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
        val root: View = binding.root
        val chartWeek = binding.barChartWeek
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val lastSundayDateTime = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SUNDAY))
        val lastSundayMidnightTimeString = "%04d-%02d-%02d".format(lastSundayDateTime.get(ChronoField.YEAR), lastSundayDateTime.get(ChronoField.MONTH_OF_YEAR), lastSundayDateTime.get(ChronoField.DAY_OF_MONTH))
        val lastSundayMidnightTime = dateFormatter.parse(lastSundayMidnightTimeString)?.time?.plus(TimeZone.getDefault().getOffset(System.currentTimeMillis()))
        val lastMondayMidnightTime = lastSundayMidnightTime?.minus(6 * 24 * 60 * 60 * 1000)
        val weekBeforeLastSundayMidnightTime = lastMondayMidnightTime?.minus(1 * 24 * 60 * 60 * 1000)
        val weekBeforeLastMondayMidnightTime = weekBeforeLastSundayMidnightTime?.minus(6 * 24 * 60 * 60 * 1000)
        val thisMondayMidnightTime = lastSundayMidnightTime!!.plus(1 * 24 * 60 * 60 * 1000)
        val thisSundayMidnightTime = lastSundayMidnightTime.plus(7 * 24 * 60 * 60 * 1000)
        val sharedPreferences = mContext.getSharedPreferences(
            "com.mas.smartmeter.mqttpreferences",
            Context.MODE_PRIVATE
        )
        dashboardTabWeekViewModel = ViewModelProvider(this)[DashboardTabWeekViewModel::class.java]
        dashboardTabWeekViewModel.textPriceLast.observe(viewLifecycleOwner) {
            if(it.second == null) {
                binding.preisGesternCard.setCardBackgroundColor(mContext.getColor(R.color.orange_700))
                binding.preisVorgesternCard.setCardBackgroundColor(mContext.getColor(R.color.orange_700))
                binding.textStrompreisGestern.text = it.first
                return@observe
            }
            val changeString =
                if (it.second!! > 999) "+${mContext.getString(R.string.a_lot)}"
                else if (it.second!! > 0) "+%d%%".format(
                    it.second
                )
                else if (it.second!! < -999) "-${mContext.getString(R.string.a_lot)}"
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
        if(!URLUtil.isValidUrl(sharedPreferences.getString("database_host", ""))) {
            Toast.makeText(mContext, mContext.getString(R.string.database_host_not_selected), Toast.LENGTH_LONG).show()
            return root
        }
        try {
            updateLastWeeksDisplay(
                dateFormatter,
                lastMondayMidnightTime,
                lastSundayMidnightTime,
                weekBeforeLastMondayMidnightTime,
                weekBeforeLastSundayMidnightTime
            )
            refreshChart(chartWeek, Pair(thisMondayMidnightTime, thisSundayMidnightTime))
        }catch (e:Error) {
            Log.e("DashboardTabWeek", e.toString())
        }
        binding.datePickerReset.setOnClickListener {
            try{
                refreshChart(chartWeek, Pair(thisMondayMidnightTime, thisSundayMidnightTime))
            }catch (e:Error) {
                Log.e("DashboardTabWeek", e.toString())
            }
        }
        binding.dateMoveLeft.setOnClickListener {
            okHttpClient.dispatcher.queuedCalls().forEach { it.cancel() }
            okHttpClient.dispatcher.runningCalls().forEach { it.cancel() }
            val endTime = currentStartTime - 1 * 24 * 60 * 60 * 1000
            val startTime = endTime - 6 * 24 * 60 * 60 * 1000
            try{
                refreshChart(chartWeek, Pair(startTime,endTime))
            }catch (e:Error) {
                Log.e("DashboardTabWeek", e.toString())
            }
        }
        binding.dateMoveRight.setOnClickListener {
            okHttpClient.dispatcher.queuedCalls().forEach { it.cancel() }
            okHttpClient.dispatcher.runningCalls().forEach { it.cancel() }
            val startTime = currentStartTime + 7 * 24 * 60 * 60 * 1000
            val endTime = startTime + 6 * 24 * 60 * 60 * 1000
            if(startTime > System.currentTimeMillis()) return@setOnClickListener
            try{
                refreshChart(chartWeek, Pair(startTime,endTime))
            }catch (e:Error) {
                Log.e("DashboardTabWeek", e.toString())
            }
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
        val sharedPreferences = mContext.getSharedPreferences(
            "com.mas.smartmeter.mqttpreferences",
            Context.MODE_PRIVATE
        )
        Log.d("a", "${sharedPreferences.getString("database_host", "")}:${sharedPreferences.getInt("database_port", 1)}/api/v1/daily/?day_start=${dateFormatter.format(beforeLastMondayTime)}&day_end=${dateFormatter.format(beforeLastSundayTime)}")
        val requestWeekBeforeLast = Request.Builder()
            .url(
                "${sharedPreferences.getString("database_host", "")}:${sharedPreferences.getInt("database_port", 1)}/api/v1/daily/?day_start=${dateFormatter.format(beforeLastMondayTime)}&day_end=${dateFormatter.format(beforeLastSundayTime)}"
            )
            .addHeader("Authorization", sharedPreferences.getString("database_api_token", "")!!)
            .build()
        OkHttpClient().newCall(requestWeekBeforeLast).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OkHttp", e.message.toString())
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let {
                    val powerPrice = sharedPreferences.getFloat("strompreis", 0F)
                    if (response.code == 200) {
                        val resJsonWeekBeforeLast = Json.parseToJsonElement(it.string())
                        val valueCountWeekBeforeLast = resJsonWeekBeforeLast.jsonObject["found"]!!.toString().toInt()
                        var totalPowerUsageWeekBeforeLast = 0F
                        if (valueCountWeekBeforeLast > 0) {
                            for (value in 0 until valueCountWeekBeforeLast) {
                                val valueJsonObject =
                                    resJsonWeekBeforeLast.jsonObject["data"]!!.jsonArray[value].jsonObject
                                totalPowerUsageWeekBeforeLast += valueJsonObject["Gesamtleistung"].toString().toFloat()

                            }
                            dashboardTabWeekViewModel.priceBeforeLastPost(
                                "%.2f€".format(((totalPowerUsageWeekBeforeLast / 100000) * powerPrice))
                            )
                        }
                        val requestLastWeek = Request.Builder()
                            .url(
                                "${sharedPreferences.getString("database_host", "")}:${sharedPreferences.getInt("database_port", 1)}/api/v1/daily/?day_start=${
                                    dateFormatter.format(
                                        lastMondayTime
                                    )
                                    
                                }&day_end=${dateFormatter.format(lastSundayTime)}"
                            )
                            .addHeader("Authorization", sharedPreferences.getString("database_api_token", "")!!)
                            .build()
                        OkHttpClient().newCall(requestLastWeek).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                Log.e("OkHttp", e.message.toString())
                            }

                            override fun onResponse(call: Call, response: Response) {
                                response.body?.let { it2 ->
                                    if (response.code == 200) {
                                        val resJsonLastWeek = Json.parseToJsonElement(it2.string())
                                        val valueCountLastWeek = resJsonLastWeek.jsonObject["found"]!!.toString().toInt()
                                        if (valueCountLastWeek > 0) {
                                            var totalPowerUsageLastWeek = 0F
                                            for (valueIndex in 0 until valueCountLastWeek) {
                                                val valueJsonObject =
                                                    resJsonLastWeek.jsonObject["data"]!!.jsonArray[valueIndex].jsonObject
                                                totalPowerUsageLastWeek += valueJsonObject["Gesamtleistung"].toString().toFloat()

                                            }
                                            val changePercentage =
                                                if(totalPowerUsageWeekBeforeLast <= 0f) 0 else if(valueCountWeekBeforeLast>0)(((totalPowerUsageLastWeek/totalPowerUsageWeekBeforeLast ) * 100)-100).roundToInt()
                                                else null
                                            val preis = "%.2f".format(((totalPowerUsageLastWeek / 100000) * powerPrice))
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
        val sharedPreferences = mContext.getSharedPreferences(
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
        val requestHours = Request.Builder()
            .url("${sharedPreferences.getString("database_host", "")}:${sharedPreferences.getInt("database_port", 1)}/api/v1/daily/?day_start=$datePreviousWeek&day_end=$dateYesterday")
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
                        val valueCount = resJson.jsonObject["found"]?.toString()?.toInt()
                        val entries: ArrayList<BarEntry> = ArrayList()
                        if (valueCount != null && valueCount > 0) {
                            val dataJsonArray = resJson.jsonObject["data"]!!.jsonArray
                            dashboardTabWeekViewModel.textChosenDaysPost("${dayOfWeekFormatterUI.format(dateRange.first)} ${dateFormatterUI.format(dateRange.first)} - ${dayOfWeekFormatterUI.format(dateRange.second)} ${dateFormatterUI.format(dateRange.second)}")
                            var totalPower = 0f
                            var emptyDays = 0
                            for(valueIndex in 0 until valueCount) {

                                while(dateFormatter.format(dateRange.first + (valueIndex + emptyDays) * 24 * 60 * 60 * 1000) != dataJsonArray[valueIndex].jsonObject["day"].toString().replace("\"", "")) {
                                    entries.add(BarEntry((valueIndex+emptyDays).toFloat(), 0F))
                                    emptyDays++
                                }
                                val entryPower = dataJsonArray[valueIndex].jsonObject["Gesamtleistung"].toString().toFloat()
                                entries.add(BarEntry((valueIndex+emptyDays).toFloat(), entryPower))
                                totalPower += entryPower
                            }
                            val powerPrice = sharedPreferences.getFloat("strompreis", 0F)
                            dashboardTabWeekViewModel.textPowerChosenPost("%.1fkWh".format(totalPower/1000))
                            dashboardTabWeekViewModel.textPriceChosenPost("%.2f€".format((totalPower/100000)*powerPrice))
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
                            ChartUtil.formatBarChart(mContext,  7, chartWeek, BarDataSet(List(7) { _ ->
                                BarEntry(
                                    0f,
                                    0f
                                )
                            }, "Watt"), true)
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
                                        return
                                    }
                                }
                            })
                            dashboardTabWeekViewModel.barChartDataPost(BarData())
                            return
                        }
                        val dataSet = BarDataSet(entries, mContext.getString(R.string.watthours))
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