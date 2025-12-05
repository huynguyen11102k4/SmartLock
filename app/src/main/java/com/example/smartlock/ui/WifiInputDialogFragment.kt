package com.example.smartlock.ui

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.smartlock.R
import com.example.smartlock.databinding.FragmentWifiInputDialogBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class WifiInputDialogFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentWifiInputDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWifiInputDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefill = arguments?.getString(ARG_PREFILL_SSID).orEmpty()
        val ssid = if (prefill.isNotEmpty()) prefill else getCurrentSsidOrEmpty()
        binding.etSsid.setText(ssid)

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnOk.setOnClickListener {
            val ssidText = binding.etSsid.text?.toString()?.trim().orEmpty()
            val passText = binding.etPass.text?.toString().orEmpty()

            var ok = true
            if (ssidText.isEmpty()) {
                binding.tilSsid.error = "SSID không được trống"
                ok = false
            } else binding.tilSsid.error = null

            if(passText.length < 8){
                binding.tilPass.error = "Mật khẩu phải có ít nhất 8 ký tự"
                ok = false
            } else {
                binding.tilPass.error = null
            }

            parentFragmentManager.setFragmentResult(REQ_WIFI_INPUT, Bundle().apply {
                putString(KEY_SSID, ssidText)
                putString(KEY_PASS, passText)
            })
            dismiss()
        }

    }

    private fun getCurrentSsidOrEmpty(): String {
        return try {
            val wm = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wm.connectionInfo?.ssid?.trim('"') ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQ_WIFI_INPUT = "req_wifi_input"
        const val KEY_SSID = "key_ssid"
        const val KEY_PASS = "key_pass"
        private const val ARG_PREFILL_SSID = "arg_prefill_ssid"

        fun new(prefillSsid: String? = null) = WifiInputDialogFragment().apply {
            arguments = Bundle().apply { putString(ARG_PREFILL_SSID, prefillSsid) }
        }
    }

}