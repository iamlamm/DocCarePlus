package com.healthtech.doccareplus.ui.success

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.FragmentSuccessBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SuccessFragment : Fragment() {
    private var _binding: FragmentSuccessBinding? = null
    private val binding get() = _binding!!
    private val args: SuccessFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Xử lý nút back
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Navigate to home với cùng hành vi như btnBack
                    findNavController().navigate(
                        SuccessFragmentDirections.actionSuccessToHome()
                    )
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupAnimations()
    }

    private fun setupViews() {
        with(binding) {
            tvMessage.text = "Mã cuộc hẹn: ${args.appointmentId}"

            btnBack.setOnClickListener {
                findNavController().navigate(
                    SuccessFragmentDirections.actionSuccessToHome()
                )
            }
        }
    }

    private fun setupAnimations() {
        with(binding) {
            successBannerAnimation.apply {
                setAnimation(R.raw.success_3)
                playAnimation()
            }

            successIconAnimation.apply {
                setAnimation(R.raw.success_1)
                playAnimation()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}