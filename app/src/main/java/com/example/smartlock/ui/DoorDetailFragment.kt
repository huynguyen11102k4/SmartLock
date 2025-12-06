package com.example.smartlock.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.smartlock.databinding.DoorDetailFragmentBinding
import com.example.smartlock.model.Door
import kotlinx.coroutines.launch


class DoorDetailFragment : Fragment() {
    private var _binding: DoorDetailFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DoorViewModel by viewModels {
        DoorViewModelFactory(requireContext().applicationContext)
    }

    private val args: DoorDetailFragmentArgs by navArgs()

    private lateinit var door: Door

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DoorDetailFragmentBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getDoorById(args.doorId).collect { door ->
                    if (door == null) {
                        findNavController().popBackStack()
                        return@collect
                    }
                    binding.tvBattery.text = "Pin: ${door.battery}%"
                }
            }
        }

        binding.cardSendEKey.setOnClickListener {
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToSendEKeyFragment(args.doorId)
            findNavController().navigate(action)
        }

        binding.cardICCard.setOnClickListener {
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToIcCardFragment(args.doorId)
            findNavController().navigate(action)
        }

        binding.cardEKeys.setOnClickListener {
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToEKeysFragment(args.doorId)
            findNavController().navigate(action)
        }

        binding.cardPasscodes.setOnClickListener {
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToPasscodesFragment(args.doorId)
            findNavController().navigate(action)
        }

        binding.cardGeneratePasscode.setOnClickListener {
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToGenerateEKeyFragment(args.doorId)
            findNavController().navigate(action)
        }

        binding.cardAuthorizedAdmin.setOnClickListener {
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToAuthorizedAdminFragment(args.doorId)
            findNavController().navigate(action)
        }

        binding.cardRecords.setOnClickListener {
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToRecordsFragment(args.doorId)
            findNavController().navigate(action)
        }

        binding.cardSettings.setOnClickListener {
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToSettingsFragment(args.doorId)
            findNavController().navigate(action)
        }

        viewModel.requestSync(args.doorId)
    }
}