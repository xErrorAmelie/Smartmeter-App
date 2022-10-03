package com.example.myapplication.ui.dashboard.tabs

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myapplication.R

class DashboardTabMonth : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dashboard_tab_month, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            DashboardTabMonth()
    }
}