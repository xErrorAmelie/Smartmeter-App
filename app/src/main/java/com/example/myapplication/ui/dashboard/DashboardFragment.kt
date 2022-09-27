package com.example.myapplication.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentDashboardBinding
import com.example.myapplication.ui.dashboard.tabs.DashboardTab30Min
import com.example.myapplication.ui.dashboard.tabs.DashboardTabDay
import com.example.myapplication.ui.dashboard.tabs.DashboardTabMonth
import com.example.myapplication.ui.dashboard.tabs.DashboardTabWeek
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener

class DashboardFragment : Fragment() {
    private lateinit var mContext:Context
    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        parentFragmentManager.beginTransaction()
            .replace(R.id.dashboard_tab_fragment, DashboardTab30Min.newInstance())
            .commit()
        val root: View = binding.root
        val tabLayout = binding.tablayoutDashboard
        binding.tablayoutDashboard.addOnTabSelectedListener(object:OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val transFragment =
                    if(tabLayout.getTabAt(0)?.isSelected == true) DashboardTab30Min.newInstance()
                    else if(tabLayout.getTabAt(1)?.isSelected == true) DashboardTabDay.newInstance()
                    else if(tabLayout.getTabAt(2)?.isSelected == true) DashboardTabWeek.newInstance()
                    else DashboardTabMonth.newInstance()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.dashboard_tab_fragment, transFragment)
                    .commit()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        return root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}