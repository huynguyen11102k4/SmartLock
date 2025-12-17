package com.example.smartlock.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartlock.databinding.FragmentRecordsBinding
import kotlin.getValue

class RecordsFragment : Fragment() {
    private var _binding: FragmentRecordsBinding? = null
    private val binding get() = _binding!!

    private val args: RecordsFragmentArgs by navArgs()

    private val viewModel: DoorViewModel by lazy {
        DoorViewModelFactory(requireActivity().applicationContext).create(DoorViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = RecordAdapter()
        binding.rvRecords.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }

//        viewModel.subscribeToState(args.doorId)
//        viewModel.subscribeToRecords(args.doorId)

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.getRecordsForDoor(args.doorId).collect { records ->
                adapter.submitList(records)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}