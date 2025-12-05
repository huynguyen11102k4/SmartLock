package com.example.smartlock.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.smartlock.R
import com.example.smartlock.databinding.FragmentRecordsBinding
import com.example.smartlock.databinding.SendEkeyFragmentBinding
import kotlin.getValue

class SendEKeyFragment : Fragment() {
    private var _binding: SendEkeyFragmentBinding? = null
    private val binding get() = _binding!!

    private val args: SendEKeyFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = SendEkeyFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}