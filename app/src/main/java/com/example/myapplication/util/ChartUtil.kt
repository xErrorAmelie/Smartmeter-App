package com.example.myapplication.util

import android.content.Context
import android.util.TypedValue
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.BarDataSet

object ChartUtil {
    fun formatBarChart (mContext: Context, valueCount: Int, chart:BarChart, dataSet:BarDataSet) {
        val themeColor = TypedValue()
        val primaryColor = TypedValue()
        val number = valueCount.toFloat()
        mContext.theme.resolveAttribute(com.google.android.material.R.attr.colorOnBackground, themeColor, true)
        mContext.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, primaryColor, true)
        dataSet.color = primaryColor.data
        dataSet.valueTextColor = themeColor.data
        dataSet.barBorderColor = primaryColor.data
        dataSet.valueTextSize = 10F
        chart.xAxis.textColor = themeColor.data
        chart.xAxis.axisMaximum = number -0.5f
        chart.xAxis.axisMinimum = -0.5f
        chart.xAxis.granularity = 1f
        val limitLineEnd = LimitLine(number -0.5f)
        limitLineEnd.lineColor = chart.xAxis.gridColor
        limitLineEnd.lineWidth = 0.4f
        chart.xAxis.addLimitLine(limitLineEnd)
        chart.legend.textColor = themeColor.data
        chart.axisLeft.axisMinimum = 0f
        chart.axisLeft.axisMaximum = dataSet.yMax * 1.15f
        chart.axisLeft.textColor = themeColor.data
        chart.setVisibleYRange(-1f, dataSet.yMax * 1.15f, chart.axisLeft.axisDependency)
        chart.setVisibleXRange(0f, number)
        chart.setFitBars(true)
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
    }
}