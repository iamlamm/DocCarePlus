package com.healthtech.doccareplus.ui.home

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.ActivityHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo navigation controller
        setupNavController()

        // Sử dụng thread khác để khởi tạo UI không quan trọng
        lifecycleScope.launch(Dispatchers.Default) {
            // Đảm bảo NavController đã sẵn sàng trước khi thiết lập bottom navigation
            withContext(Dispatchers.Main) {
                setupBottomNavigation()
                setupNavigation()
            }

            // Khởi tạo các sự kiện click không quan trọng
            withContext(Dispatchers.Main) {
                setupClickListeners()
            }

            // Quan sát dữ liệu người dùng (có thể làm sau)
            withContext(Dispatchers.Main) {
                observeCurrentUser()
                observerNotificationBadge()
            }
        }
    }

    private fun setupNavController() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
    }

    private fun observeCurrentUser() {
        lifecycleScope.launch {
            viewModel.currentUser.collect { user ->
                user?.let {
                    viewModel.updateUserUI(binding, it)
                }
            }
        }
    }

    private fun observerNotificationBadge() {
        lifecycleScope.launch {
            viewModel.unreadNotificationCount.collect {
                viewModel.updateNotificationBadge(binding)
            }
        }
    }

    private fun setupClickListeners() {
        // binding.ivUserAvatar.setOnClickListener {
        //     navController.navigate(R.id.action_global_profile)
        // }
        binding.apply {
            ivUserAvatar.setOnClickListener {
                navController.navigate(R.id.action_global_profile)
            }

            btnNotification.setOnClickListener {
                navController.navigate(R.id.action_global_notification)
            }
        }
    }

    private fun setupNavigation() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.allCategoriesFragment,
                R.id.allDoctorsFragment,
                R.id.allDoctorsFragment,
                R.id.profileFragment,
                R.id.notificationFragment,
                R.id.editProfileFragment -> {
                    hideAppBarAndBottomAppBar()
                }

                R.id.homeFragment -> {
                    showAppBarAndBottomAppBar()
                    binding.bottomNav.selectedItemId = R.id.nav_home
                }

                R.id.allDoctorsFragment -> {
                    showAppBarAndBottomAppBar()
                    binding.bottomNav.selectedItemId = R.id.nav_doctors
                }

                R.id.moreFragment -> {
                    showAppBarAndBottomAppBar()
                    binding.bottomNav.selectedItemId = R.id.nav_more
                }

                R.id.chatFragment -> {
                    hideAppBarAndBottomAppBar()
                    binding.bottomNav.selectedItemId = R.id.nav_messages
                }

                else -> Unit
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (navController.currentDestination?.id != R.id.homeFragment) {
                        // Quay về HomeFragment mà không tạo instance mới
                        navController.popBackStack(R.id.homeFragment, false)
                    }
                    true
                }

                R.id.nav_doctors -> {
                    if (navController.currentDestination?.id != R.id.allDoctorsFragment) {
                        if (navController.currentDestination?.id == R.id.homeFragment) {
                            navController.navigate(R.id.action_home_to_allDoctors)
                        } else {
                            navController.navigate(R.id.allDoctorsFragment)
                        }
                    }
                    true
                }

                R.id.nav_more -> {
                    if (navController.currentDestination?.id != R.id.moreFragment) {
                        if (navController.currentDestination?.id == R.id.homeFragment) {
                            navController.navigate(R.id.action_home_to_more)
                        } else {
                            navController.navigate(R.id.moreFragment)
                        }
                    }
                    true
                }

                R.id.nav_messages -> {
                    if (navController.currentDestination?.id != R.id.chatFragment) {
                        if (navController.currentDestination?.id == R.id.homeFragment) {
                            navController.navigate(R.id.action_home_to_chat)
                        } else {
                            navController.navigate(R.id.action_global_chat)
                        }
                    }
                    true
                }

                else -> false
            }
        }

        // Mặc định chọn tab home
        binding.bottomNav.selectedItemId = R.id.nav_home
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    fun hideAppBarAndBottomAppBar() {
        binding.topBar.visibility = View.GONE
        binding.bottomAppBar.visibility = View.GONE
        binding.fabCalendar.visibility = View.GONE
    }

    fun showAppBarAndBottomAppBar() {
        binding.topBar.visibility = View.VISIBLE
        binding.bottomAppBar.visibility = View.VISIBLE
        binding.fabCalendar.visibility = View.VISIBLE
    }

//    override fun onBackPressed() {
//        if (navController.currentDestination?.id == R.id.homeFragment) {
//            // Nếu đang ở Home Fragment, thoát app
//            finish()
//        } else if (navController.currentDestination?.id in listOf(
//                R.id.allDoctorsFragment,
//                R.id.moreFragment
//            )
//        ) {
//            // Nếu đang ở các tab bottom nav, quay về Home
//            navController.navigate(R.id.action_global_home)
//        } else {
//            // Các trường hợp khác, thực hiện back bình thường
//            super.onBackPressed()
//        }
//    }
}