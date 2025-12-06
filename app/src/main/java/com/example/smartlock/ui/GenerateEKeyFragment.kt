package com.example.smartlock.ui

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.smartlock.databinding.FragmentGenerateEkeyBinding
import com.example.smartlock.model.Passcode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class GenerateEKeyFragment : Fragment() {

    private var _binding: FragmentGenerateEkeyBinding? = null
    private val binding get() = _binding!!

    private val args: GenerateEKeyFragmentArgs by navArgs()
    private val viewModel: DoorViewModel by activityViewModels {
        DoorViewModelFactory(requireContext().applicationContext)
    }

    private var selectedType = ""
    private var startTime = ""
    private var endTime = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGenerateEkeyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val types = arrayOf("Mã một lần", "Có thời hạn", "Hiệu lực 24 giờ")
        binding.actvEkeyType.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types)
        )

        binding.actvEkeyType.setOnItemClickListener { _, _, position, _ ->
            selectedType = when (position) {
                0 -> "one-time"
                1 -> "timed"
                2 -> "timed-24h"
                else -> ""
            }

            binding.layoutTimed.visibility = if (position == 1) View.VISIBLE else View.GONE
            binding.layout24h.visibility = if (position == 2) View.VISIBLE else View.GONE
        }

        binding.etStartTime.setOnClickListener { showDateTimePicker(true) }
        binding.etEndTime.setOnClickListener { showDateTimePicker(false) }

        binding.btnGenerate.setOnClickListener {
            if (selectedType.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng chọn loại e-key", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedType == "timed" && (startTime.isBlank() || endTime.isBlank())) {
                Toast.makeText(requireContext(), "Vui lòng chọn thời gian hiệu lực", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                generateAndSaveEKey()
            }
        }

        binding.btnCopy.setOnClickListener {
            val code = binding.tvGeneratedCode.text.toString().replace(" ", "")
            requireContext().copyToClipboard("e-key", code)
            Toast.makeText(requireContext(), "Đã sao chép $code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDateTimePicker(isStart: Boolean) {
        val c = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            TimePickerDialog(requireContext(), { _, h, min ->
                val timeStr = String.format("%02d/%02d/%04d %02d:%02d", d, m + 1, y, h, min)
                if (isStart) {
                    startTime = timeStr
                    binding.etStartTime.setText(timeStr)
                } else {
                    endTime = timeStr
                    binding.etEndTime.setText(timeStr)
                }
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    @SuppressLint("DefaultLocale")
    private suspend fun generateAndSaveEKey() {
        var code: String
        var attempts = 0

        do {
            code = String.format("%06d", (100000..999999).random())
            attempts++
            if (attempts > 20) {
                Toast.makeText(requireContext(), "Không thể tạo mã mới, vui lòng thử lại", Toast.LENGTH_LONG).show()
                return
            }
        } while (isCodeExists(code))

        val validity = when (selectedType) {
            "one-time" -> ""
            "timed-24h" -> {
                val c = Calendar.getInstance()
                c.add(Calendar.HOUR_OF_DAY, 24)
                val end = String.format(
                    "%02d/%02d/%04d %02d:%02d",
                    c.get(Calendar.DAY_OF_MONTH),
                    c.get(Calendar.MONTH) + 1,
                    c.get(Calendar.YEAR),
                    c.get(Calendar.HOUR_OF_DAY),
                    c.get(Calendar.MINUTE)
                )
                JSONObject().apply {
                    put("start", getCurrentDateTime())
                    put("end", end)
                }.toString()
            }
            "timed" -> {
                JSONObject().apply {
                    put("start", startTime)
                    put("end", endTime)
                }.toString()
            }
            else -> ""
        }

        val passcode = Passcode(
            code = code,
            doorId = args.doorId,
            type = if (selectedType == "timed-24h") "timed" else selectedType,
            validity = validity,
            status = "Active"
        )

        binding.tvGeneratedCode.text = code
        binding.tvInfo.text = when (selectedType) {
            "one-time" -> "Loại: Mã dùng 1 lần\nSẽ tự xóa sau khi mở khóa"
            "timed-24h" -> "Loại: Hiệu lực 24 giờ\nTừ bây giờ đến 24h sau"
            else -> "Loại: Có thời hạn\nTừ: $startTime\nĐến: $endTime"
        }
        binding.cardResult.visibility = View.VISIBLE

        viewModel.addEKey(args.doorId, passcode)
    }

    @SuppressLint("DefaultLocale")
    private fun getCurrentDateTime(): String {
        val c = Calendar.getInstance()
        return String.format(
            "%02d/%02d/%04d %02d:%02d",
            c.get(Calendar.DAY_OF_MONTH),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.YEAR),
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE)
        )
    }

    private suspend fun isCodeExists(code: String): Boolean {
        val list = viewModel.getEKeysForDoor(args.doorId).first()
        return list.any { it.code == code }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
}