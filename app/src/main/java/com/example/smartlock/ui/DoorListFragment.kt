package com.example.smartlock.ui

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartlock.model.Door
import com.example.smartlock.databinding.DoorListFragmentBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

class DoorListFragment : Fragment() {
    private var _binding: DoorListFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DoorViewModel by viewModels {
        DoorViewModelFactory(requireContext().applicationContext)
    }

    private lateinit var rvDoors: RecyclerView
    private val doors = mutableListOf<Door>()

    private val scanLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents != null) parseQrCode(result.contents)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DoorListFragmentBinding.inflate(inflater, container, false)

        Log.d("DoorList", "Binding created: ${_binding != null}")
        Log.d("DoorList", "FAB exists: ${binding.fabAddDoor != null}")
        Log.d("DoorList", "RV exists: ${binding.rvDoors != null}")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvDoors = binding.rvDoors
        rvDoors.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        val adapter = DoorAdapter { door ->
            Log.d("DoorList", "Door clicked: ${door.id}")
            val action = DoorListFragmentDirections.actionDoorListFragmentToDoorDetailFragment(door.id)
            findNavController().navigate(action)
        }
        binding.rvDoors.adapter = adapter

        if (viewModel.doors.value.isEmpty()) {
            val dummyDoor = Door(
                id = "12345",
                name = "Cửa chính",
                permission = "Chủ sở hữu",
                battery = 85,
                macAddress = "AA:BB:CC:DD:EE:FF",
                mqttTopicPrefix = "door/123"
            )
            viewModel.insertDoor(dummyDoor)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.doors.collect { list ->
                    adapter.submitList(list)
                }
            }
        }

        binding.fabAddDoor.setOnClickListener {
            showAddDoorDialog()
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

//        viewModel.reSubscribeAll()
    }

    private fun showAddDoorDialog() {
        val options = arrayOf("Quét mã QR", "Kết nối Bluetooth")
        AlertDialog.Builder(requireContext())
            .setTitle("Thêm khóa mới")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startQrScanner()
                    1 -> startBleScan()
                }
            }
            .show()
    }

    private fun startQrScanner() {
        val options = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt("Đặt mã QR vào khung")
        scanLauncher.launch(options)
    }

    private fun parseQrCode(qrContent: String) {
        val parts = qrContent.split("|")
        if (parts.size >= 4 && parts[0] == "SMARTLOCK") {
            val door = Door(
                id = parts[3],
                name = parts[1],
                permission = "Chủ sở hữu",
                battery = 100,
                macAddress = parts[3],
                mqttTopicPrefix = "door/${parts[2]}"
            )
            viewModel.insertDoor(door)
        } else {
            Toast.makeText(context, "Mã QR không hợp lệ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startBleScan() {
        val action = DoorListFragmentDirections.actionDoorListFragmentToBleProvisionFragment()
        findNavController().navigate(action)
    }

    private fun addDoorFromQr(door: Door) {
        viewModel.insertDoor(door)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}