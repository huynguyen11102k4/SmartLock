package com.example.smartlock.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartlock.MqttClientManager
import com.example.smartlock.R
import com.example.smartlock.databinding.FragmentIcCardBinding
import com.example.smartlock.model.ICCard
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONArray

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
        adapter = ICCardAdapter{ catd ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa thẻ IC?")
                .setPositiveButton("Xóa") {_, _ ->
                    viewModel.deleteICCard(args.doorId, catd.id)
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
        binding.rvICCards.layoutManager = LinearLayoutManager(context)
        binding.rvICCards.adapter = adapter
        viewModel.syncICCards(args.doorId)

        MqttClientManager.subscribe("${viewModel.doors.value.find { it.id == args.doorId }?.mqttTopicPrefix}/iccards/list") { publish ->
            val jsonArray = JSONArray(String(publish.payloadAsBytes))
            val cards = (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                ICCard(obj.getString("id"), obj.getString("name"), obj.getString("status"))
            }
            viewModel.icCards.value = cards
        }

        binding.fabAddICCard.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Thêm thẻ IC")
                .setMessage("Đặt thẻ vào đầu đọc (demo: nhập UID)")
                .setView(EditText(requireContext()).apply { hint = "UID" })
                .setPositiveButton("Thêm") { dialog, _ ->
                    val et = (dialog as AlertDialog).findViewById<EditText>(android.R.id.custom)!!
                    val uid = et.text.toString()
                    if (uid.isNotEmpty()) {
                        val card = ICCard(uid, "Thẻ #$uid", "Active")
                        viewModel.addICCard(args.doorId, card)
                    }
                }
                .show()
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}