package com.example.myapplication.ui.dashboard

import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentDashboardBinding
import com.example.myapplication.ui.dashboard.tabs.DashboardTab30Min
import com.example.myapplication.ui.dashboard.tabs.DashboardTabDay
import com.example.myapplication.ui.dashboard.tabs.DashboardTabMonth
import com.example.myapplication.ui.dashboard.tabs.DashboardTabWeek
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import info.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class DashboardFragment : Fragment() {
    private lateinit var mContext:Context
    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    companion object {
        private val mutableCurrentMqttData = MutableLiveData<Pair<Long,Float>?>().apply {
            value = null
        }
        val currentMqttData:LiveData<Pair<Long,Float>?> = mutableCurrentMqttData
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val dashboardViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        val textView: TextView = binding.textHome
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it.toString()
        }
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
        val sharedPreferences = requireContext().getSharedPreferences(
            "com.mas.smartmeter.mqttpreferences",
            Context.MODE_PRIVATE
        )
        val broker = sharedPreferences.getString("broker", "empty")
        val port = sharedPreferences.getInt("port", 8883)
        val topic = sharedPreferences.getString("topic", "empty")
        val qos = 1
        val clientId = MqttClient.generateClientId()
        val client = MqttAndroidClient(
            this.requireContext(), "$broker:$port",
            clientId
        )
        try {
            val options = MqttConnectOptions()
            options.userName = sharedPreferences.getString("username", "")
            options.password = sharedPreferences.getString("password", "")!!.toCharArray()
            options.connectionTimeout = 10
            val token = client.connect(options)
            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    try {
                        val subToken = client.subscribe(topic!!, qos)
                        subToken.actionCallback = object : IMqttActionListener {
                            override fun onSuccess(asyncActionToken: IMqttToken) {
                                Log.d(ContentValues.TAG, "MQTT subscribed")
                                client.addCallback(object : MqttCallback {
                                    override fun connectionLost(cause: Throwable?) {
                                        if (cause != null) {
                                            Log.e(ContentValues.TAG, cause.toString())
                                            Toast.makeText(requireContext(), cause.message, Toast.LENGTH_LONG).show()
                                        }
                                        client.reconnect()
                                    }
                                    override fun messageArrived(
                                        topic: String?,
                                        message: MqttMessage?
                                    ) {
                                        Log.d(
                                            ContentValues.TAG,
                                            "Topic: \"$topic\", Message: \"$message\""
                                        )
                                        mutableCurrentMqttData.postValue(Pair(System.currentTimeMillis(), message.toString().toFloat()))
                                        dashboardViewModel.postText(message.toString())
                                    }
                                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                                })
                            }
                            override fun onFailure(
                                asyncActionToken: IMqttToken,
                                exception: Throwable
                            ) {
                                Log.e(ContentValues.TAG, exception.toString())
                                Toast.makeText(requireContext(), exception.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: MqttException) {
                        e.printStackTrace()
                        Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.e(ContentValues.TAG, exception.toString())
                    if(IllegalArgumentException::class.java.isInstance(exception)) {
                        Toast.makeText(requireContext(), mContext.getString(R.string.mqtt_host_not_selected), Toast.LENGTH_LONG).show()
                    } else Toast.makeText(requireContext(), exception.message, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: MqttException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
        }
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