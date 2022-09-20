package com.example.myapplication.ui.einstellungen

import android.R
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
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
        binding.editTextMQTTBroker.setText(sharedPreferences.getString("broker", "empty"))
        binding.editTextMQTTPort.setText(sharedPreferences.getString("port", "1883"))
        binding.editTextMQTTTopic.setText(sharedPreferences.getString("topic", "empty"))
        binding.editTextMQTTUsername.setText(sharedPreferences.getString("username", "empty"))
        binding.editTextMQTTPassword.setText(sharedPreferences.getString("password", "empty"))

        binding.button.setOnClickListener {
            val editor = sharedPreferences.edit()
            val broker = binding.editTextMQTTBroker.text.toString()
            val port = binding.editTextMQTTPort.text.toString()
            val topic = binding.editTextMQTTTopic.text.toString()
            val username = binding.editTextMQTTUsername.text.toString()
            val password = binding.editTextMQTTPassword.text.toString()
            editor.putString("broker", broker)
            editor.putString("port", port)
            editor.putString("topic", topic)
            editor.putString("username", username)
            editor.putString("password", password)
            editor.apply()
            Toast.makeText(requireContext(), "uwu!", Toast.LENGTH_LONG).show()
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}