package com.example.smartlock.ui

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.smartlock.databinding.FragmentGenerateEkeyBinding
import com.example.smartlock.viewmodel.PasscodeUiState
import com.example.smartlock.viewmodel.PasscodeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class GenerateEKeyFragment : Fragment() {

    private var _binding: FragmentGenerateEkeyBinding? = null
    private val binding get() = _binding!!

    private val args: GenerateEKeyFragmentArgs by navArgs()
    private val viewModel: PasscodeViewModel by viewModels()

    private var selectedTypeCode = -1
    private var startTime: Calendar? = null
    private var endTime: Calendar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGenerateEkeyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        val types = arrayOf("Mã một lần", "Mã có thời hạn (Timed)", "Mã 24 giờ")
        binding.actvEkeyType.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types))

        binding.actvEkeyType.setOnItemClickListener { _, _, position, _ ->
            selectedTypeCode = when (position) {
                0 -> 0
                1 -> 1
                2 -> 1
                else -> -1
            }
            binding.layoutTimed.visibility = if (position == 1) View.VISIBLE else View.GONE
            binding.layout24h.visibility = if (position == 2) View.VISIBLE else View.GONE
        }

        binding.etStartTime.setOnClickListener { showDateTimePicker(true) }
        binding.etEndTime.setOnClickListener { showDateTimePicker(false) }

        binding.btnGenerate.setOnClickListener {
            val code = String.format("%06d", (100000..999999).random())
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

            val selectedPosition = binding.actvEkeyType.text.toString().let { types.indexOf(it) }

            val (finalStart, finalEnd) = when (selectedPosition) {
                0 -> null to null
                1 -> {
                    if (startTime == null || endTime == null) {
                        Toast.makeText(context, "Vui lòng chọn thời gian", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    isoFormat.format(startTime?.time) to isoFormat.format(endTime?.time)
                }
                2 -> {
                    val start = Calendar.getInstance()
                    val end = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 24) }
                    isoFormat.format(start.time) to isoFormat.format(end.time)
                }
                else -> {
                    Toast.makeText(context, "Vui lòng chọn loại mã", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val typeToSend = if (selectedPosition == 0) 0 else 1
            viewModel.addPasscode(args.doorId, code, typeToSend, finalStart, finalEnd)
            binding.tvGeneratedCode.text = code.chunked(3).joinToString(" ")
        }

        binding.btnCopy.setOnClickListener {
            val code = binding.tvGeneratedCode.text.toString().replace(" ", "")
            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("E-key", code)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Đã sao chép: ${code}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is PasscodeUiState.PasscodeAdded -> {
                            binding.cardResult.visibility = View.VISIBLE
                            binding.btnGenerate.isEnabled = true
                            binding.btnGenerate.text = "TẠO MÃ KHÁC"
                            Toast.makeText(context, "Yêu cầu tạo eKey thành công", Toast.LENGTH_SHORT).show()

                            viewModel.resetState()
                        }
                        is PasscodeUiState.Error -> {
                            binding.btnGenerate.isEnabled = true
                            binding.btnGenerate.text = "TẠO MÃ NGAY"
                            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()

                            viewModel.resetState()
                        }
                        is PasscodeUiState.Loading -> {
                            binding.btnGenerate.isEnabled = false
                            binding.btnGenerate.text = "ĐANG XỬ LÝ..."
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun showDateTimePicker(isStart: Boolean) {
        val c = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            TimePickerDialog(requireContext(), { _, h, min ->
                val selected = Calendar.getInstance().apply { set(y, m, d, h, min) }
                val displayFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                if (isStart) {
                    startTime = selected
                    binding.etStartTime.setText(displayFormat.format(selected.time))
                } else {
                    endTime = selected
                    binding.etEndTime.setText(displayFormat.format(selected.time))
                }
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }
}