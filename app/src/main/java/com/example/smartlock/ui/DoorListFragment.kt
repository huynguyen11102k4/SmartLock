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
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.bumptech.glide.Glide
import com.example.smartlock.R
import com.example.smartlock.databinding.DoorListFragmentBinding
import com.example.smartlock.model.entity.Door
import com.example.smartlock.viewmodel.DoorUiState
import com.example.smartlock.viewmodel.DoorViewModel
import com.example.smartlock.viewmodel.UserViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DoorListFragment : Fragment() {

    private var _binding: DoorListFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DoorViewModel by viewModels()

    private val userViewModel: UserViewModel by viewModels()

    private lateinit var doorAdapter: DoorAdapter

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            parseQrCode(result.contents)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DoorListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
        observeUserProfile()

        userViewModel.getUserProfile()
        viewModel.loadDoor()
    }

    private fun setupRecyclerView() {
        doorAdapter = DoorAdapter(
            onDoorClick = { door ->
                val action = DoorListFragmentDirections.actionDoorListFragmentToDoorDetailFragment(door.id)
                findNavController().navigate(action)
            },
            onMoreClick = { view, door ->
                showPopupMenu(view, door)
            }
        )

        binding.rvDoors.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = doorAdapter
            itemAnimator = null
        }
    }

    private fun setupListeners() {
        binding.fabAddDoor.setOnClickListener {
            showAddDoorDialog()
        }

        binding.ivProfile.setOnClickListener {
            findNavController().navigate(R.id.action_doorListFragment_to_accountSettingsFragment)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.doors.collect { list ->
                    doorAdapter.submitList(list)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is DoorUiState.Loading -> {
                            binding.shimmerViewContainer.visibility = View.VISIBLE
                            binding.shimmerViewContainer.startShimmer()
                            binding.rvDoors.visibility = View.GONE
                        }
                        is DoorUiState.Success -> {
                            hideLoading()
                        }
                        is DoorUiState.Error -> {
                            hideLoading()
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                        is DoorUiState.DoorCreated -> {
                            hideLoading()
                            Toast.makeText(requireContext(), "Thêm thiết bị thành công", Toast.LENGTH_SHORT).show()
                            viewModel.loadDoor()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun showPopupMenu(view: View, door: Door) {
        val popup = androidx.appcompat.widget.PopupMenu(requireContext(), view)
        // Bạn cần tạo file menu_door_item.xml trong res/menu
        popup.menuInflater.inflate(R.menu.menu_door_item, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    showEditDoorDialog(door)
                    true
                }
                R.id.action_delete -> {
                    showDeleteConfirmDialog(door)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showEditDoorDialog(door: Door) {
        // Sử dụng concept dialog tương tự như dialog_add_door của bạn
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_door, null)
        val edtName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edtDoorName)
        edtName.setText(door.name)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<View>(R.id.btnSave).setOnClickListener {
            val newName = edtName.text.toString().trim()
            if (newName.isNotEmpty()) {
                viewModel.updateDoor(door.id, newName)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showDeleteConfirmDialog(door: Door) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xóa thiết bị")
            .setMessage("Bạn có chắc chắn muốn xóa '${door.name}'? Hành động này không thể hoàn tác.")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteDoor(door.id)
            }
            .show()
    }

    private fun observeUserProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                userViewModel.currentUser.collect { user ->
                    user?.let {
                        val welcomeTv = binding.tvWelcome
                        welcomeTv?.text = "Welcome Home, ${it.name}"

                        binding.ivProfile.load(it.avatarUrl) {
                            crossfade(true)
                            placeholder(R.drawable.ic_person)
                            transformations(CircleCropTransformation())
                        }
                    }
                }
            }
        }
    }

    private fun hideLoading() {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
        binding.rvDoors.visibility = View.VISIBLE
    }

    private fun showAddDoorDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_door, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<View>(R.id.btnQrScan).setOnClickListener {
            dialog.dismiss()
            val options = ScanOptions()
                .setPrompt("Quét mã QR trên hộp hoặc thân khóa")
                .setBeepEnabled(true)
                .setOrientationLocked(false)
            scanLauncher.launch(options)
        }

        dialogView.findViewById<View>(R.id.btnBleScan).setOnClickListener {
            dialog.dismiss()
            findNavController().navigate(R.id.action_doorListFragment_to_bleProvisionFragment)
        }

        dialog.show()
    }

    private fun parseQrCode(qrContent: String) {
        val parts = qrContent.split("|")
        if (parts.size >= 4 && parts[0] == "SMARTLOCK") {
            viewModel.createDoor(
                doorCode = parts[3],
                name = parts[1],
                mqttTopicPrefix = parts[2],
                macAddress = parts[3]
            )
        } else {
            Toast.makeText(requireContext(), "Mã QR không đúng định dạng thiết bị", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}