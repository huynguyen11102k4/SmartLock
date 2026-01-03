package com.example.smartlock.ui

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
import androidx.navigation.fragment.navArgs
import com.example.smartlock.R
import com.example.smartlock.databinding.FragmentEkeysBinding
import com.example.smartlock.model.entity.Passcode
import com.example.smartlock.viewmodel.DoorViewModel
import com.example.smartlock.viewmodel.PasscodeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EKeysFragment : Fragment() {

    private var _binding: FragmentEkeysBinding? = null
    private val binding get() = _binding!!

    private val args: EKeysFragmentArgs by navArgs()
    private val viewModel: PasscodeViewModel by viewModels()

    private val doorViewModel: DoorViewModel by viewModels()

    private lateinit var adapter: EKeyAdapter
    private var masterPasscode: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEkeysBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        doorViewModel.setCurrentDoor(args.doorId)
        setupRecyclerView()
        setupToolbar()
        observeViewModel()

        viewModel.setCurrentDoor(args.doorId)
        viewModel.loadPasscodes(args.doorId)
    }

    private fun setupRecyclerView() {
        adapter = EKeyAdapter(
            onEKeyClick = { ekey -> showVerifyMasterDialog(ekey) },
            onDelete = { passcode -> showDeleteDialog(passcode) }
        )
        binding.rvEkeys.adapter = adapter
    }

    private fun showVerifyMasterDialog(ekey: Passcode) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_unlock, null)
        val etPass = dialogView.findViewById<TextInputEditText>(R.id.etPasscode)

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogTheme)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<View>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<View>(R.id.btnConfirmUnlock).setOnClickListener {
            val input = etPass.text.toString()
            if (input == masterPasscode) {
                adapter.revealCode(ekey.code!!)
                dialog.dismiss()
            } else {
                etPass.error = "Master Passcode không đúng"
            }
        }
        dialog.show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                doorViewModel.currentDoor.collect { door ->
                    masterPasscode = door?.doorCode
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.passcodes.collect { list ->
                    Log.d("EKEYS_DEBUG", "Data từ DB: $list")

                    adapter.submitList(list)
                    binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun showDeleteDialog(passcode: Passcode) {
        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setTitle("Xóa eKey")
            .setMessage("Mã này sẽ bị vô hiệu hóa vĩnh viễn.")
            .setPositiveButton("Xóa") { _, _ -> viewModel.deletePasscode(args.doorId, passcode.code!!) }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}