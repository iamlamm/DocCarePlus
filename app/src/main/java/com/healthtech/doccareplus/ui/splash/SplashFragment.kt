package com.healthtech.doccareplus.ui.splash

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.FragmentSplashBinding
import com.healthtech.doccareplus.ui.home.HomeActivity
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
        binding.progressBarSplash.setLoading(true)
        viewModel.preLoadHomeData()
        viewModel.preloadLoginResources()
        observeStartDestination()
    }

    private fun observeStartDestination() {
        viewLifecycleOwner.lifecycleScope.launch {
            val delayJob = launch { delay(3000) } // Giảm xuống 1.5s thay vì 3s
            delayJob.join()
            viewModel.startDestination.collectLatest { destination ->
                if (destination != 0) {
                    binding.progressBarSplash.setLoading(false)

                    when (destination) {
                        R.id.loginFragment -> {
                            findNavController().navigate(
                                R.id.action_splash_to_login
                            )
                        }

                        R.id.homeFragment -> {
                            // Sử dụng Intent với FLAG_ACTIVITY_NO_ANIMATION
                            val intent = Intent(requireContext(), HomeActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                            startActivity(intent)

                            // Sử dụng overridePendingTransition để tránh hiệu ứng giật
                            requireActivity().overridePendingTransition(
                                R.anim.fade_in, R.anim.fade_out
                            )
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