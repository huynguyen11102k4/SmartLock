package com.example.smartlock.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartlock.databinding.FragmentEkeysBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class EKeysFragment : Fragment() {

    private var _binding: FragmentEkeysBinding? = null
    private val binding get() = _binding!!

    private val args: EKeysFragmentArgs by navArgs()
    private val viewModel: DoorViewModel by activityViewModels {
        DoorViewModelFactory(requireActivity().applicationContext)
    }

    private lateinit var adapter: EKeyAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEkeysBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        adapter = EKeyAdapter { passcode ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa e-key?")
                .setMessage("Mã ${passcode.code} sẽ bị xóa khỏi danh sách")
                .setPositiveButton("Xóa") { _, _ ->
                    viewModel.deleteEKey(passcode.code, args.doorId)
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        binding.rvEkeys.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@EKeysFragment.adapter
            setHasFixedSize(true)
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.getEKeysForDoor(args.doorId).collect { list ->
                adapter.submitList(list.sortedByDescending { it.code })
                binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}