package com.example.myapplication.util

import android.content.Context
import android.util.TypedValue
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarDataSet

object ChartUtil {
    fun formatBarChart (mContext: Context, valueCount: Int, chart:BarChart, dataSet:BarDataSet, offset:Boolean) {
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
        chart.legend.textColor = themeColor.data
        chart.axisLeft.textColor = themeColor.data
        chart.axisLeft.axisMinimum = 0f
        chart.axisLeft.axisMaximum = dataSet.yMax * 1.15f
        chart.setVisibleYRange(-1f, if(dataSet.yMax<1000) 1000f else dataSet.yMax * 1.15f, chart.axisLeft.axisDependency)
        if(offset) {
            chart.xAxis.axisMinimum = -0.5f
            chart.xAxis.axisMaximum = number-0.5f
            chart.setVisibleXRange(-1f, number)
        } else {
            chart.xAxis.axisMaximum = number
            chart.xAxis.axisMinimum = 0f
            chart.setVisibleXRange(0f, number+0.001f)
        }
        chart.axisRight.setDrawLabels(false)
        chart.axisRight.setDrawGridLines(false)
        chart.description.isEnabled = false
    }
}