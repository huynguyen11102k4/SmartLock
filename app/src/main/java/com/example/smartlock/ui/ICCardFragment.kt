package com.example.smartlock.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartlock.MqttClientManager
import com.example.smartlock.MqttService
import com.example.smartlock.R
import com.example.smartlock.databinding.FragmentIcCardBinding
import com.example.smartlock.model.ICCard
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONArray
import org.json.JSONObject

class ICCardFragment : Fragment() {
    private var _binding: FragmentIcCardBinding? = null
    private val binding get() = _binding!!

    private val args: ICCardFragmentArgs by navArgs()
    private val viewModel: DoorViewModel by activityViewModels{
        DoorViewModelFactory(requireActivity().applicationContext)
    }

    private lateinit var adapter: ICCardAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentIcCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        viewModel.subscribeToICCardList(args.doorId)
        viewModel.requestICCardSync(args.doorId)

        adapter = ICCardAdapter{ card ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa thẻ IC?")
                .setMessage("Thẻ ${card.name} sẽ bị xóa khỏi danh sách")
                .setPositiveButton("Xóa") { _, _ ->
                    viewModel.deleteICCard(args.doorId, card.id)
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        binding.rvICCards.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ICCardFragment.adapter
            setHasFixedSize(true)
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            viewModel.getICCardsForDoor(args.doorId).collect { list ->
                adapter.submitList(list.sortedBy { it.name })
                binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        binding.fabAddICCard.setOnClickListener {
            val modes = arrayOf("Nhập tay UID", "Quẹt thẻ 2 lần vào khóa")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chọn cách thêm thẻ IC")
                .setItems(modes) { _, which ->
                    when (which) {
                        0 -> showManualEntryDialog()
                        1 -> startSwipeMode()
                    }
                }
                .show()
        }
    }

    private fun showManualEntryDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Nhập UID thẻ (hex, ví dụ: 04A1B2C3D4E5F6)"
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Thêm thẻ IC thủ công")
            .setMessage("Nhập UID của thẻ (có thể lấy từ log khi quẹt thử)")
            .setView(input)
            .setPositiveButton("Thêm") { _, _ ->
                val uid = input.text.toString().trim().uppercase()
                if (uid.isNotEmpty()) {
                    val card = ICCard(
                        id = uid,
                        doorId = args.doorId,
                        name = "Card #${uid.take(8)}",
                        status = "Active"
                    )
                    viewModel.addICCard(args.doorId, card)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun startSwipeMode() {
        val door = viewModel.doors.value.find { it.id == args.doorId } ?: return
        val payload = JSONObject().apply {
            put("action", "start_swipe_add")
        }.toString()

        MqttClientManager.publish("${door.mqttTopicPrefix}/iccards", payload)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Chế độ thêm thẻ bằng quẹt")
            .setMessage("Vui lòng quẹt thẻ mới 2 lần liên tiếp vào mặt khóa để xác nhận thêm.\n\nFirmware sẽ tự động thêm nếu 2 UID trùng nhau.")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}