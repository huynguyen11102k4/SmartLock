package com.example.smartlock.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.smartlock.databinding.FragmentPasscodesBinding
import com.example.smartlock.viewmodel.DoorUiState
import com.example.smartlock.viewmodel.DoorViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PasscodesFragment : Fragment() {
    private var _binding: FragmentPasscodesBinding? = null
    private val binding get() = _binding!!

    private val args: PasscodesFragmentArgs by navArgs()
    private val doorViewModel: DoorViewModel by viewModels()

    private var currentDoorCode: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPasscodesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        doorViewModel.setCurrentDoor(args.doorId)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.btnChangePasscode.setOnClickListener {
            val oldCode = binding.etOldPasscode.text.toString().trim()
            val newCode = binding.etNewPasscode.text.toString().trim()

            if (!currentDoorCode.isNullOrEmpty()) {
                if (oldCode != currentDoorCode) {
                    binding.tilOldPasscode.error = "Mã cũ không chính xác"
                    return@setOnClickListener
                }
            }
            binding.tilOldPasscode.error = null

            if (newCode.length < 6) {
                binding.tilNewPasscode.error = "Mã mới phải có ít nhất 6 chữ số"
                return@setOnClickListener
            }
            binding.tilNewPasscode.error = null

            doorViewModel.updateDoorCode(args.doorId, newCode)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                doorViewModel.currentDoor.collect { door ->
                    door?.let {
                        currentDoorCode = it.doorCode

                        if (currentDoorCode.isNullOrEmpty()) {
                            binding.tvCurrentPasscode.text = "Chưa đặt"
                            binding.tilOldPasscode.visibility = View.GONE
                        } else {
                            binding.tvCurrentPasscode.text = "••••••"
                            binding.tilOldPasscode.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                doorViewModel.uiState.collect { state ->
                    when (state) {
                        is DoorUiState.Loading -> {
                            binding.btnChangePasscode.isEnabled = false
                            binding.btnChangePasscode.text = "ĐANG GỬI LỆNH..."
                        }
                        is DoorUiState.DoorCodeUpdated -> {
                            binding.btnChangePasscode.isEnabled = true
                            binding.btnChangePasscode.text = "CẬP NHẬT NGAY"
                            Toast.makeText(context, "Đã đổi mã khóa chính thành công!", Toast.LENGTH_SHORT).show()

                            binding.etOldPasscode.text?.clear()
                            binding.etNewPasscode.text?.clear()

                            doorViewModel.resetState()
                        }
                        is DoorUiState.Error -> {
                            binding.btnChangePasscode.isEnabled = true
                            binding.btnChangePasscode.text = "CẬP NHẬT NGAY"
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                            doorViewModel.resetState()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}