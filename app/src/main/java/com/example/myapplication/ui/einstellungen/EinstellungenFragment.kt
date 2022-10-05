package com.example.myapplication.ui.einstellungen

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentEinstellungenBinding


class EinstellungenFragment : Fragment() {

    private var _binding: FragmentEinstellungenBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentEinstellungenBinding.inflate(inflater, container, false)
        val root: View = binding.root


        val sharedPreferences = requireContext().getSharedPreferences(
            "com.mas.smartmeter.mqttpreferences",
            Context.MODE_PRIVATE
        )
        binding.editTextMQTTBroker.setText(sharedPreferences.getString("broker", ""))
        binding.editTextMQTTPort.setText(sharedPreferences.getInt("port", 8883).toString())
        binding.editTextMQTTTopic.setText(sharedPreferences.getString("topic", ""))
        binding.editTextMQTTUsername.setText(sharedPreferences.getString("username", ""))
        binding.editTextMQTTPassword.setText(sharedPreferences.getString("password", ""))
        binding.editTextStrom.setText(sharedPreferences.getFloat("strompreis", 0f).toString())
        binding.editTextDatenbankHost.setText(sharedPreferences.getString("database_host", ""))
        binding.editTextDatenbankPort.setText(sharedPreferences.getInt("database_port", 1).toString())
        binding.editTextAPIToken.setText(sharedPreferences.getString("database_api_token", ""))
        binding.button.setOnClickListener {
            val editor = sharedPreferences.edit()
            val broker = binding.editTextMQTTBroker.text.toString()
            val port = binding.editTextMQTTPort.text.toString().toIntOrNull()
            val topic = binding.editTextMQTTTopic.text.toString()
            val username = binding.editTextMQTTUsername.text.toString()
            val password = binding.editTextMQTTPassword.text.toString()
            val strompreis = binding.editTextStrom.text.toString().toFloatOrNull()
            val datenbankHost = binding.editTextDatenbankHost.text.toString()
            val datenbankPort = binding.editTextDatenbankPort.text.toString().toIntOrNull()
            val datenbankApiKey = binding.editTextAPIToken.text.toString()
            if(!URLUtil.isValidUrl(datenbankHost)) {
                Toast.makeText(
                    requireContext(),
                    "${getString(R.string.datenbank_adresse)} ${getString(R.string.not_valid_host)}",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            editor.putString("broker", broker)
            if(port!=null) {
                editor.putInt("port", port)
            }
            editor.putString("topic", topic)
            editor.putString("username", username)
            editor.putString("password", password)
                editor.putString("database_host", datenbankHost)
            if (datenbankPort != null) {
                editor.putInt("database_port", datenbankPort)
            }
            editor.putString("database_api_token", datenbankApiKey)
            if (strompreis != null) {
                editor.putFloat("strompreis", strompreis)
            }
            editor.apply()
            Toast.makeText(requireContext(), "Einstellungen übernommen!", Toast.LENGTH_SHORT).show()
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}