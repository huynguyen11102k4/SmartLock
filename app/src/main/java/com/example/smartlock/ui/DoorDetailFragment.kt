package com.example.smartlock.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
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
import com.example.smartlock.MqttClientManager
import com.example.smartlock.R
import com.example.smartlock.databinding.DoorDetailFragmentBinding
import com.example.smartlock.model.Door
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

// Thêm imports cần thiết cho Animation
import android.graphics.Color
import android.os.Build
import android.view.animation.DecelerateInterpolator
import androidx.annotation.RequiresApi


class DoorDetailFragment : Fragment() {
    private var _binding: DoorDetailFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DoorViewModel by viewModels {
        DoorViewModelFactory(requireContext().applicationContext)
    }

    private val args: DoorDetailFragmentArgs by navArgs()

    private var isLocked: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DoorDetailFragmentBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                binding.cardStatus.apply {
                    translationY = 100f
                    alpha = 0f
                    animate().translationY(0f).alpha(1f).setDuration(400).start()
                }

                viewModel.getDoorById(args.doorId).collect { door ->
                    if (door == null) {
                        findNavController().popBackStack()
                        return@collect
                    }
                    if (door.id == args.doorId) {
                        viewModel.subscribeToState(args.doorId)
                        viewModel.subscribeToRecords(args.doorId)
                        binding.tvDoorName.text = door.name ?: "Door Detail"
                    }
                    binding.tvBattery.text = "${door.battery}%"
                }

                viewModel.currentStates.collect { states ->
                    val state = states[args.doorId] ?: "locked"
                    if (state == "unlocked" && isLocked) {
                        animateToUnlocked()
                    } else if (state == "locked" && !isLocked) {
                        animateToLocked()
                    }
                }
            }
        }

        binding.thumb.setOnTouchListener { v, event ->
            val maxTranslationX = binding.lockSlider.width - binding.thumb.width - (binding.lockSlider.paddingStart + binding.lockSlider.paddingEnd)
            val minTranslationX = 0f

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                }
                MotionEvent.ACTION_MOVE -> {
                    var newX = event.rawX - binding.lockSlider.x - v.width / 2
                    newX = newX.coerceIn(minTranslationX, maxTranslationX.toFloat())
                    v.translationX = newX
                    if (isLocked && newX > maxTranslationX * 0.9f) {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    } else if (!isLocked && newX < maxTranslationX * 0.1f) {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                    updateIconAlpha(newX, maxTranslationX.toFloat())
                }
                MotionEvent.ACTION_UP -> {
                    if (isLocked) {
                        if (v.translationX > maxTranslationX * 0.75f) {
                            showUnlockDialog()
                        } else {
                            animateToLocked()
                        }
                    } else {
                        if (v.translationX < maxTranslationX * 0.25f) {
                            v.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                            performLockAction()
                        } else {
                            animateToUnlocked()
                        }
                    }
                    v.performClick()
                }
                else -> return@setOnTouchListener false
            }
            true
        }

        binding.cardSendEKey.setOnClickListener {
            it.pressAnim()
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToSendEKeyFragment(args.doorId)
            findNavController().navigate(action)
        }
        binding.cardICCard.setOnClickListener {
            it.pressAnim()
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToIcCardFragment(args.doorId)
            findNavController().navigate(action)
        }
        binding.cardEKeys.setOnClickListener {
            it.pressAnim()
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToEKeysFragment(args.doorId)
            findNavController().navigate(action)
        }
        binding.cardPasscodes.setOnClickListener {
            it.pressAnim()
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToPasscodesFragment(args.doorId)
            findNavController().navigate(action)
        }
        binding.cardGeneratePasscode.setOnClickListener {
            it.pressAnim()
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToGenerateEKeyFragment(args.doorId)
            findNavController().navigate(action)
        }
        binding.cardAuthorizedAdmin.setOnClickListener {
            it.pressAnim()
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToAuthorizedAdminFragment(args.doorId)
            findNavController().navigate(action)
        }
        binding.cardRecords.setOnClickListener {
            it.pressAnim()
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToRecordsFragment(args.doorId)
            findNavController().navigate(action)
        }
        binding.cardSettings.setOnClickListener {
            it.pressAnim()
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToSettingsFragment(args.doorId)
            findNavController().navigate(action)
        }
        binding.btnSettingsIcon.setOnClickListener {
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToSettingsFragment(args.doorId)
            findNavController().navigate(action)
        }

        view.post {
            animateToLocked()
        }

        binding.btnBackIcon.setOnClickListener {
            findNavController().popBackStack()
        }

    }


    private fun showUnlockDialog(){
        val input = EditText(requireContext()).apply{
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Nhập mã mở cửa(6 chữ số)"
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Mở cửa")
            .setMessage("Vui lòng nhập mật khẩu chính để mở cửa.")
            .setView(input)
            .setPositiveButton("Mở cửa") { _, _ ->
                val enteredPass = input.text.toString()
                if (enteredPass.length != 6 || !enteredPass.all { it.isDigit() }) {
                    Toast.makeText(requireContext(), "Mật khẩu phải là 6 chữ số!", Toast.LENGTH_SHORT).show()
                    animateToLocked()
                    return@setPositiveButton
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.getDoorById(args.doorId).collect { door ->
                        if(door?.masterPasscode == enteredPass){
                            val payload = JSONObject().apply {
                                put("action", "unlock")
                            }.toString()
                            MqttClientManager.publish("${door.mqttTopicPrefix}/control", payload)

                            performUnlockAnimation()
                            animateToUnlocked()
                            Toast.makeText(requireContext(), "Khóa đã mở!", Toast.LENGTH_SHORT).show()

                            delay(8000)
                        }else {
                            Toast.makeText(
                                requireContext(),
                                "Mật khẩu không đúng!",
                                Toast.LENGTH_SHORT
                            ).show()
                            animateToLocked()
                        }
                    }
                }
            }
            .setNegativeButton("Hủy") { dialog, _ ->
                animateToLocked()
                dialog.dismiss()
            }
            .show()
    }

    private fun animateToLocked() {
        binding.thumb.animate()
            .translationX(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()

        binding.icLockInactive.animate()
            .alpha(0.3f)
            .setDuration(150)
            .start()

        binding.icUnlockInactive.animate()
            .alpha(0f)
            .setDuration(150)
            .start()

        binding.tvArrow.text = "› › ›"
        binding.lockSlider.setBackgroundResource(R.drawable.bg_slider_pill)
        binding.icThumb.setImageResource(R.drawable.ic_lock)

        isLocked = true
    }


    private fun animateToUnlocked() {
        val maxTranslationX =
            binding.lockSlider.width - binding.thumb.width -
                    (binding.lockSlider.paddingStart + binding.lockSlider.paddingEnd)

        binding.thumb.animate()
            .translationX(maxTranslationX.toFloat())
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()

        binding.icLockInactive.animate()
            .alpha(0f)
            .setDuration(150)
            .start()

        binding.icUnlockInactive.animate()
            .alpha(0.3f)
            .setDuration(150)
            .start()

        binding.tvArrow.text = "‹ ‹ ‹"
        binding.lockSlider.setBackgroundResource(R.drawable.bg_slider_pill_locked)
        binding.icThumb.setImageResource(R.drawable.ic_unlock)

        isLocked = false
    }


    private fun View.pressAnim() {
        animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
            animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()
    }

    private fun updateIconAlpha(translationX: Float, maxX: Float) {
        val progress = (translationX / maxX).coerceIn(0f, 1f)

        if (isLocked) {
            binding.icLockInactive.alpha = 0.3f * (1f - progress)
            binding.icUnlockInactive.alpha = 0.3f * progress
        } else {
            val reversedProgress = 1f - progress

            binding.icLockInactive.alpha = 0.3f * (1f - reversedProgress)
            binding.icUnlockInactive.alpha = 0.3f * reversedProgress
        }
    }

    private fun performUnlockAnimation() {
        binding.ivLock.animate()
            .translationY(-20f)
            .alpha(0.7f)
            .setDuration(300)
            .withEndAction {
                binding.ivLock.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    private fun performLockAction(){
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getDoorById(args.doorId).collect { door ->
                if(door!=null){
                    val payload = JSONObject().apply {
                        put("action", "lock")
                    }.toString()
                    MqttClientManager.publish("${door.mqttTopicPrefix}/control", payload)
                    animateToLocked() // Animation slider
                    Toast.makeText(requireContext(), "Khóa đã khóa!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}