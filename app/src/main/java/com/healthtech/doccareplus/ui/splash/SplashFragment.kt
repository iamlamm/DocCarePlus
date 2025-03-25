package com.healthtech.doccareplus.ui.splash

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.FragmentSplashBinding
import com.healthtech.doccareplus.ui.home.HomeActivity
import com.healthtech.doccareplus.utils.AnimationUtils
import com.healthtech.doccareplus.utils.AnimationUtils.hideWithAnimation
import com.healthtech.doccareplus.utils.safeNavigate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : Fragment() {
    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SplashViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAnimation()
        binding.progressBarSplash.setLoading(true)
        viewModel.preLoadHomeData()
        viewModel.preloadLoginResources()
        observeStartDestination()
    }

    private fun setupAnimation() {
        binding.apply {
            imageView.alpha = 0f
            imageView2.alpha = 0f
            progressBarSplash.alpha = 0f
        }

        AnimationUtils.fadeInSequentially(
            views = listOf(
                binding.imageView,
                binding.imageView2,
                binding.progressBarSplash
            ),
            duration = 1000,
            delayBetween = 300,
            type = AnimationUtils.AnimationType.FADE
        )
    }

    private fun observeStartDestination() {
        viewLifecycleOwner.lifecycleScope.launch {
            val delayJob = launch { delay(3000) }
            delayJob.join()

            viewModel.startDestination.collectLatest { destination ->
                if (destination != 0) {
                    val views = listOf(
                        binding.progressBarSplash,
                        binding.imageView2,
                        binding.imageView
                    )

                    views.forEach { view ->
                        view.hideWithAnimation(
                            duration = 500,
                            type = AnimationUtils.AnimationType.FADE
                        )
                    }
                    delay(600)
                    binding.progressBarSplash.setLoading(false)
                    when (destination) {
                        R.id.loginFragment -> {
                            findNavController().safeNavigate(R.id.action_splash_to_login)
                        }

                        R.id.homeFragment -> {
                            val options = ActivityOptions.makeCustomAnimation(
                                requireContext(),
                                R.anim.ultra_smooth_fade_in,
                                R.anim.ultra_smooth_fade_out
                            )
                            val intent = Intent(requireContext(), HomeActivity::class.java)
                            startActivity(intent, options.toBundle())
                            requireActivity().finish()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}