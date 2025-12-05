package com.example.smartlock.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartlock.MqttClientManager
import com.example.smartlock.R
import com.example.smartlock.databinding.FragmentPasscodesBinding
import com.example.smartlock.model.Passcode
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.getValue

class PasscodesFragment : Fragment() {
    private var _binding: FragmentPasscodesBinding? = null
    private val binding get() = _binding!!

    private val args: PasscodesFragmentArgs by navArgs()
    private val viewModel: DoorViewModel by activityViewModels{
        DoorViewModelFactory(requireContext().applicationContext)
    }

    private lateinit var adapter: PasscodeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPasscodesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.doors.collect { doorList ->
                val door = doorList.find { it.id == args.doorId } ?: return@collect
                val master = door.masterPasscode

                if (master.isNullOrBlank()) {
                    binding.tvCurrentPasscode.text = "Chưa đặt"
                    binding.tvHintUnset.visibility = View.VISIBLE
                    binding.tilOldPasscode.visibility = View.GONE
                } else {
                    binding.tvCurrentPasscode.text = "••••••"
                    binding.tvHintUnset.visibility = View.GONE
                    binding.tilOldPasscode.visibility = View.VISIBLE
                }
            }
        }

        binding.btnChangePasscode.setOnClickListener {
            val oldCode = binding.etOldPasscode.text.toString().trim()
            val newCode = binding.etNewPasscode.text.toString().trim()

            if (newCode.length != 6|| !newCode.all { it.isDigit() }) {
                binding.tilNewPasscode.error = "Mã phải 6 chữ số"
                return@setOnClickListener
            }
            binding.tilNewPasscode.error = null

            val door = viewModel.doors.value.find { it.id == args.doorId }!!
            val currentMaster = door.masterPasscode

            if (currentMaster == null) {
                if (oldCode.isNotEmpty()) {
                    binding.tilOldPasscode.error = "Lần đầu không cần nhập mã cũ"
                    return@setOnClickListener
                }
            } else {
                if (oldCode != currentMaster) {
                    binding.tilOldPasscode.error = "Mã cũ không đúng"
                    return@setOnClickListener
                }
            }
            binding.tilOldPasscode.error = null

            val payload = JSONObject().apply {
                put("action", "add")
                put("type", "permanent")
                put("code", newCode)
                if (currentMaster != null) put("old_code", oldCode)
            }.toString()

            if (!MqttClientManager.isConnected()) {
                Toast.makeText(context, "Không có kết nối mạng/MQTT", Toast.LENGTH_LONG).show()
                Log.e("MQTT PasscodeFragment", "Not connected")
                return@setOnClickListener
            }

            MqttClientManager.publish("${door.mqttTopicPrefix}/passcodes", payload)

            viewModel.updateMasterPasscode(args.doorId, newCode)

            Toast.makeText(requireContext(), "Đổi mã khóa chính thành công!", Toast.LENGTH_SHORT).show()
            binding.etOldPasscode.text?.clear()
            binding.etNewPasscode.text?.clear()
            binding.etNewPasscode.requestFocus()
        }

        viewModel.subscribeToPasscodeList(args.doorId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}