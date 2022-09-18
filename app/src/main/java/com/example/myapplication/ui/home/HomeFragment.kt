package com.example.myapplication.ui.home

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
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.databinding.FragmentHomeBinding
import info.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        val sharedPreferences = requireContext().getSharedPreferences(
            "com.mas.smartmeter.mqttpreferences",
            Context.MODE_PRIVATE
        )
        val broker = sharedPreferences.getString("broker", "empty")
        val port = sharedPreferences.getString("port", "emtpy")
        val topic = sharedPreferences.getString("topic", "empty")
        val qos = 1
        val clientId = MqttClient.generateClientId()
        val client = MqttAndroidClient(
            this.requireContext(), "$broker:$port",
            clientId
        )
        try {
            val options = MqttConnectOptions()
            options.userName = sharedPreferences.getString("username", "empty")
            options.password = sharedPreferences.getString("password", "empty")!!.toCharArray()
            options.connectionTimeout = 10
            val token = client.connect(options)
            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    // We are connected
                    Log.d(ContentValues.TAG, "MQTT verbunden")
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
                                    }

                                    override fun messageArrived(
                                        topic: String?,
                                        message: MqttMessage?
                                    ) {
                                        Log.d(
                                            ContentValues.TAG,
                                            "Topic: \"$topic\", Message: \"$message\""
                                        )
                                        homeViewModel.meow(message.toString())
                                    }

                                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                                        TODO("Not yet implemented")
                                    }

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
                    Toast.makeText(requireContext(), exception.message, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: MqttException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
        }
        return root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
