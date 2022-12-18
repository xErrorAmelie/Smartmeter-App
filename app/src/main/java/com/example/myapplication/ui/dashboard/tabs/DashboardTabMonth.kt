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
import com.example.myapplication.databinding.FragmentDashboardTabMonthBinding
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime.ofInstant
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.math.roundToInt

class DashboardTabMonth : Fragment() {
    private lateinit var mContext: Context
    private var _binding: FragmentDashboardTabMonthBinding? = null
    private val binding get() = _binding!!
    private var currentStartTime = 0L
    private val okHttpClient = OkHttpClient()
    private lateinit var dashboardTabWeekViewModel: DashboardTabMonthViewModel
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }
    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardTabMonthBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val chartWeek = binding.barChartWeek
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val monthStartDateTime = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val monthStartTimeISOString = "%04d-%02d-%02d".format(monthStartDateTime.get(ChronoField.YEAR), monthStartDateTime.get(
            ChronoField.MONTH_OF_YEAR), monthStartDateTime.get(ChronoField.DAY_OF_MONTH))
        val monthStartTime = dateFormatter.parse(monthStartTimeISOString)!!.time.plus(TimeZone.getDefault().getOffset(System.currentTimeMillis()))
        val lastMonthEndTime = monthStartTime.minus(1 * 24 * 60 * 60 * 1000)
        val lastMonthStartTime = lastMonthEndTime.minus((ofInstant(Instant.ofEpochMilli(lastMonthEndTime), TimeZone.getDefault().toZoneId()).toLocalDate().lengthOfMonth()-1).toLong() * 24 * 60 * 60 * 1000)
        val monthBeforeLastEndTime = lastMonthStartTime.minus(1 * 24 * 60 * 60 * 1000)
        val monthBeforeLastStartTime = monthBeforeLastEndTime.minus((ofInstant(Instant.ofEpochMilli(monthBeforeLastEndTime), TimeZone.getDefault().toZoneId()).toLocalDate().lengthOfMonth()-1).toLong() * 24 * 60 * 60 * 1000)
        val monthEndTime = monthStartTime.plus((LocalDate.now().lengthOfMonth()-1).toLong() * 24 * 60 * 60 * 1000)
        val sharedPreferences = mContext.getSharedPreferences(
            "com.mas.smartmeter.mqttpreferences",
            Context.MODE_PRIVATE
        )
        dashboardTabWeekViewModel = ViewModelProvider(this)[DashboardTabMonthViewModel::class.java]
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
            updateLastMonthsDisplay(
                dateFormatter,
                lastMonthStartTime,
                lastMonthEndTime,
                monthBeforeLastStartTime,
                monthBeforeLastEndTime
            )
            refreshChart(chartWeek, Pair(monthStartTime, monthEndTime))
        }catch (e:Error) {
            Log.e("DashboardTabMonth", e.toString())
        }
            binding.datePickerReset.setOnClickListener {
            try{
                refreshChart(chartWeek, Pair(monthStartTime, monthEndTime))
            }catch (e:Error) {
                Log.e("DashboardTabMonth", e.toString())
            }
        }
        binding.dateMoveLeft.setOnClickListener {
            okHttpClient.dispatcher.queuedCalls().forEach { it.cancel() }
            okHttpClient.dispatcher.runningCalls().forEach { it.cancel() }
            val endTime = currentStartTime - 1 * 24 * 60 * 60 * 1000
            val startTime = endTime - (ofInstant(Instant.ofEpochMilli(endTime), TimeZone.getDefault().toZoneId()).toLocalDate().lengthOfMonth()-1).toLong() * 24 * 60 * 60 * 1000
            try{
                refreshChart(chartWeek, Pair(startTime,endTime))
            }catch (e:Error) {
                Log.e("DashboardTabMonth", e.toString())
            }
        }
        binding.dateMoveRight.setOnClickListener {
            okHttpClient.dispatcher.queuedCalls().forEach { it.cancel() }
            okHttpClient.dispatcher.runningCalls().forEach { it.cancel() }
            val startTime = currentStartTime + (ofInstant(Instant.ofEpochMilli(currentStartTime), TimeZone.getDefault().toZoneId()).toLocalDate().lengthOfMonth()).toLong() * 24 * 60 * 60 * 1000
            val endTime = startTime + (ofInstant(Instant.ofEpochMilli(startTime), TimeZone.getDefault().toZoneId()).toLocalDate().lengthOfMonth()-1).toLong() * 24 * 60 * 60 * 1000
            if(startTime > System.currentTimeMillis()) return@setOnClickListener
            try{
                refreshChart(chartWeek, Pair(startTime,endTime))
            }catch (e:Error) {
                Log.e("DashboardTabMonth", e.toString())
            }
        }
        return root
    }
    private fun updateLastMonthsDisplay(
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
        val requestMonthBeforeLast = Request.Builder()
            .url(
                "${sharedPreferences.getString("database_host", "")}:${sharedPreferences.getInt("database_port", 1)}/api/v1/daily/?day_start=${dateFormatter.format(beforeLastMondayTime)}&day_end=${dateFormatter.format(beforeLastSundayTime)}"
            )
            .addHeader("Authorization", sharedPreferences.getString("database_api_token", "")!!)
            .build()
        OkHttpClient().newCall(requestMonthBeforeLast).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OkHttp", "OkHttp is not OK" + e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let {
                    val resJsonMonthBeforeLast = Json.parseToJsonElement(it.string())
                    val powerPrice = sharedPreferences.getFloat("strompreis", 0F)
                    if (response.code == 200) {
                        val valueCountMonthBeforeLast = resJsonMonthBeforeLast.jsonObject["found"]!!.toString().toInt()
                        var totalPowerUsageMonthBeforeLast = 0F
                        if (valueCountMonthBeforeLast > 0) {
                            for (valueIndex in 0 until valueCountMonthBeforeLast) {
                                val valueJsonObject =
                                    resJsonMonthBeforeLast.jsonObject["data"]!!.jsonArray[valueIndex].jsonObject
                                totalPowerUsageMonthBeforeLast += valueJsonObject["Gesamtleistung"].toString().toFloat()

                            }
                            dashboardTabWeekViewModel.priceBeforeLastPost(
                                "%.2f€".format(((totalPowerUsageMonthBeforeLast / 100000) * powerPrice))
                            )
                        }
                        val requestLastMonth = Request.Builder()
                            .url(
                                "${sharedPreferences.getString("database_host", "")}:${sharedPreferences.getInt("database_port", 1)}/api/v1/daily/?day_start=${
                                    dateFormatter.format(
                                        lastMondayTime
                                    )

                                }&day_end=${dateFormatter.format(lastSundayTime)}"
                            )
                            .addHeader("Authorization", sharedPreferences.getString("database_api_token", "")!!)
                            .build()
                        OkHttpClient().newCall(requestLastMonth).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                Log.e("OkHttp", "OkHttp is not OK" + e.message)
                            }

                            override fun onResponse(call: Call, response: Response) {
                                response.body?.let { it2 ->
                                    val resJsonLastMonth = Json.parseToJsonElement(it2.string())
                                    if (response.code == 200) {
                                        val valueCountLastMonth = resJsonLastMonth.jsonObject["found"]!!.toString().toInt()
                                        if (valueCountLastMonth > 0) {
                                            var totalPowerUsageLastMonth = 0F
                                            for (valueIndex in 0 until valueCountLastMonth) {
                                                val valueJsonObject =
                                                    resJsonLastMonth.jsonObject["data"]!!.jsonArray[valueIndex].jsonObject
                                                totalPowerUsageLastMonth += valueJsonObject["Gesamtleistung"].toString().toFloat()

                                            }
                                            val changePercentage =
                                                if(totalPowerUsageMonthBeforeLast <= 0) 0 else if(valueCountMonthBeforeLast > 0)
                                                    (((totalPowerUsageLastMonth/totalPowerUsageMonthBeforeLast ) * 100)-100).roundToInt()
                                                else null
                                            val preis = "%.2f".format(((totalPowerUsageLastMonth / 100000) * powerPrice))
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
    private fun refreshChart(chart: BarChart, dateRange:Pair<Long, Long>) {
        val sharedPreferences = mContext.getSharedPreferences(
            "com.mas.smartmeter.mqttpreferences",
            Context.MODE_PRIVATE
        )
        currentStartTime = dateRange.first
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateFormatterUI = SimpleDateFormat("dd.MM.", Locale.getDefault())
        val dayOfWeekFormatterUI = SimpleDateFormat("EEE", Locale.getDefault())
        val dateMonthStart = dateFormatter.format(dateRange.first)
        val dateMonthEnd = dateFormatter.format(dateRange.second)
        binding.textChartRange.text = "${dateFormatterUI.format(dateRange.first)} - ${dateFormatterUI.format(dateRange.second)}"
        val request = Request.Builder()
            .url("${sharedPreferences.getString("database_host", "")}:${sharedPreferences.getInt("database_port", 1)}/api/v1/daily/?day_start=$dateMonthStart&day_end=$dateMonthEnd")
            .addHeader("Authorization", sharedPreferences.getString("database_api_token", "")!!)
            .build()
        okHttpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OkHttp", "OkHttp is not OK" + e.message)
                dashboardTabWeekViewModel.textChosenDaysPost("${dayOfWeekFormatterUI.format(dateRange.first)} ${dateFormatterUI.format(dateRange.first)} - ${dayOfWeekFormatterUI.format(dateRange.second)} ${dateFormatterUI.format(dateRange.second)}")
                dashboardTabWeekViewModel.textPowerChosenPost("-,--kWh")
                dashboardTabWeekViewModel.textPriceChosenPost("-,--€")
                dashboardTabWeekViewModel.barChartDataPost(BarData())
                return
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let {
                    val resString = it.string()
                    Log.i("OkHttp", resString)
                    val resJson = Json.parseToJsonElement(resString)
                    if (response.code == 200) {
                        val valueCount = resJson.jsonObject["found"]!!.toString().toInt()
                        val entries: ArrayList<BarEntry> = ArrayList()
                        if (valueCount > 0) {
                            val data = resJson.jsonObject["data"]!!.jsonArray
                            dashboardTabWeekViewModel.textChosenDaysPost("${dayOfWeekFormatterUI.format(dateRange.first)} ${dateFormatterUI.format(dateRange.first)} - ${dayOfWeekFormatterUI.format(dateRange.second)} ${dateFormatterUI.format(dateRange.second)}")
                            var totalPowerUsage = 0f
                            var emptyDays = 0
                            for(valueIndex in 0 until valueCount) {
                                while(dateFormatter.format(dateRange.first + (valueIndex + emptyDays).toLong() * 24 * 60 * 60 * 1000) != data[valueIndex].jsonObject["day"].toString().replace("\"", "")) {
                                    entries.add(BarEntry((valueIndex+emptyDays).toFloat(), 0F))
                                    emptyDays++
                                }
                                val valuePower = data[valueIndex].jsonObject["Gesamtleistung"].toString().toFloat()
                                entries.add(BarEntry((valueIndex+emptyDays).toFloat(), valuePower))
                                totalPowerUsage += valuePower
                            }
                            val powerPrice = sharedPreferences.getFloat("strompreis", 0F)
                            dashboardTabWeekViewModel.textPowerChosenPost("%.1fkWh".format(totalPowerUsage/1000))
                            dashboardTabWeekViewModel.textPriceChosenPost("%.2f€".format((totalPowerUsage/100000)*powerPrice))
                            chart.xAxis.valueFormatter = object: ValueFormatter() {
                                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                                    val time = value.roundToInt().toLong() * 24 * 60 * 60 * 1000+ dateRange.first
                                    return if (value < 32 && value>=0) dayOfWeekFormatterUI.format(time) + "\n" + dateFormatterUI.format(time) else " \n "
                                }
                            }
                        } else {
                            dashboardTabWeekViewModel.textChosenDaysPost("${dayOfWeekFormatterUI.format(dateRange.first)} ${dateFormatterUI.format(dateRange.first)} - ${dayOfWeekFormatterUI.format(dateRange.second)} ${dateFormatterUI.format(dateRange.second)}")
                            dashboardTabWeekViewModel.textPowerChosenPost("-,--kWh")
                            dashboardTabWeekViewModel.textPriceChosenPost("-,--€")
                            dashboardTabWeekViewModel.barChartDataPost(BarData())
                            return
                        }
                        val dataSet = BarDataSet(entries, mContext.getString(R.string.watthours))
                        ChartUtil.formatBarChart(mContext,  ofInstant(Instant.ofEpochMilli(dateRange.first), TimeZone.getDefault().toZoneId()).toLocalDate().lengthOfMonth(), chart, dataSet, true)
                        chart.setXAxisRenderer(object:
                            XAxisRenderer(chart.viewPortHandler, chart.xAxis, chart.getTransformer(
                                YAxis.AxisDependency.LEFT)) {
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
                        chart.extraTopOffset = 15f
                        dataSet.setDrawValues(false)
                        dashboardTabWeekViewModel.barChartDataPost(BarData(dataSet))
                    }

                }

            }

        })
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            DashboardTabMonth()
    }
}