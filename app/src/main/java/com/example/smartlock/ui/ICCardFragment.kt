package com.example.smartlock.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartlock.databinding.FragmentIcCardBinding
import com.example.smartlock.model.entity.ICCard
import com.example.smartlock.viewmodel.ICCardUiState
import com.example.smartlock.viewmodel.ICCardViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ICCardFragment : Fragment() {
    private var _binding: FragmentIcCardBinding? = null
    private val binding get() = _binding!!

    private val args: ICCardFragmentArgs by navArgs()

    private val viewModel: ICCardViewModel by viewModels()

    private lateinit var adapter: ICCardAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIcCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        viewModel.setCurrentDoor(args.doorId)
        viewModel.loadICCards(args.doorId)
    }

    private fun setupRecyclerView() {
        adapter = ICCardAdapter { card ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa thẻ IC?")
                .setMessage("Thẻ ${card.name} sẽ bị xóa khỏi hệ thống.")
                .setPositiveButton("Xóa") { _, _ ->
                    viewModel.deleteICCard(args.doorId, card.id)
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        binding.rvICCards.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ICCardFragment.adapter
        }
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.fabAddICCard.setOnClickListener {
            val modes = arrayOf("Nhập tay UID", "Kích hoạt chế độ quẹt thẻ trên khóa")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Thêm thẻ IC mới")
                .setItems(modes) { _, which ->
                    when (which) {
                        0 -> showManualEntryDialog()
                        1 -> viewModel.startSwipeAdd(args.doorId)
                    }
                }
                .show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.icCards.collect { list ->
                    adapter.submitList(list)
                    binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        // Quan sát trạng thái UI (Loading, Success, Error)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is ICCardUiState.CardAdded -> {
                            Toast.makeText(context, "Thêm thẻ thành công", Toast.LENGTH_SHORT).show()
                        }
                        is ICCardUiState.CardDeleted -> {
                            Toast.makeText(context, "Đã xóa thẻ", Toast.LENGTH_SHORT).show()
                        }
                        is ICCardUiState.SwipeAddMode -> {
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Chế độ quẹt thẻ")
                                .setMessage(state.message)
                                .setPositiveButton("Đã hiểu", null)
                                .show()
                        }
                        is ICCardUiState.Error -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun showManualEntryDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Ví dụ: 04A1B2C3"
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nhập UID thẻ")
            .setView(input)
            .setPositiveButton("Thêm") { _, _ ->
                val uid = input.text.toString().trim().uppercase()
                if (uid.isNotEmpty()) {
                    viewModel.addICCard(args.doorId, uid, "Thẻ #${uid.take(4)}")
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}