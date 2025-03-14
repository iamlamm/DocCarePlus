package com.healthtech.doccareplus.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.ActivityHomeBinding
import com.healthtech.doccareplus.utils.PermissionManager
import com.healthtech.doccareplusadmin.utils.AnimationUtils.showWithAnimation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

        // Kiểm tra và yêu cầu quyền thông báo
        checkAndRequestNotificationPermission()

        // Khởi tạo navigation controller
        setupNavController()


        // Xử lý thông báo nếu mở từ notification
        handleNotificationIntent(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Cập nhật intent hiện tại
        setIntent(intent)
        // Xử lý notification intent nếu có
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent) {
        try {
            // Xử lý thông báo thông thường
            if (intent.getBooleanExtra("OPEN_NOTIFICATIONS", false)) {
                val notificationId = intent.getStringExtra("NOTIFICATION_ID")
                if (notificationId != null) {
                    try {
                        viewModel.markNotificationAsRead(notificationId)
                    } catch (e: Exception) {
                        Log.e("HomeActivity", "Error marking notification as read", e)
                    }
                }

                // Đảm bảo NavController đã khởi tạo trước khi điều hướng
                if (::navController.isInitialized) {
                    navController.navigate(R.id.notificationFragment)
                } else {
                    // Lưu lại action để thực hiện sau khi NavController khởi tạo
                    lifecycleScope.launch {
                        delay(300) // Đợi một chút để NavController khởi tạo
                        if (::navController.isInitialized) {
                            navController.navigate(R.id.notificationFragment)
                        }
                    }
                }
            }

            // Xử lý mở chi tiết cuộc hẹn - Điều hướng đến màn hình thích hợp
            if (intent.getBooleanExtra("OPEN_APPOINTMENT_DETAIL", false)) {
                val appointmentId = intent.getStringExtra("APPOINTMENT_ID")
                if (appointmentId != null && ::navController.isInitialized) {
                    // Vì không có appointmentFragment, điều hướng đến màn hình thích hợp
                    // Ví dụ: Điều hướng đến màn hình lịch
                    navController.navigate(R.id.homeFragment)  // Hoặc màn hình lịch nếu có

                    // Lưu ID cuộc hẹn để fragment có thể truy cập
                    viewModel.setSelectedAppointmentId(appointmentId)

                    // Hiển thị thông báo cho người dùng
                    Toast.makeText(
                        this,
                        "Đang mở thông tin cuộc hẹn: $appointmentId",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e("HomeActivity", "Error handling notification intent", e)
            Toast.makeText(this, "Có lỗi xảy ra khi mở thông báo", Toast.LENGTH_SHORT).show()
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
        binding.apply {
            ivUserAvatar.setOnClickListener {
                navController.navigate(R.id.action_global_profile)
            }

            btnNotification.setOnClickListener {
                navController.navigate(R.id.action_global_notification)
            }

            // Đầu tiên, thiết lập click listener cho FAB
            fabCalendar.setOnClickListener {
                // Preload dữ liệu nếu cần
                viewModel.preloadAppointmentsScreen()

                // Điều hướng đến màn hình cuộc hẹn
                navController.navigate(R.id.action_global_appointments)
            }

            // Sau đó, hiển thị FAB với animation (nếu cần)
            fabCalendar.showWithAnimation(duration = 2000)
        }
    }

    private fun setupNavigation() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.allCategoriesFragment,
                R.id.allDoctorsFragment,
                R.id.profileFragment,
                R.id.notificationFragment,
                R.id.editProfileFragment,
                R.id.appointmentsFragment -> {
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

    private fun checkAndRequestNotificationPermission() {
        // Kiểm tra phiên bản Android 13 (Tiramisu) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionManager.hasNotificationPermission(this)) {
                PermissionManager.requestNotificationPermissions(this)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionManager.NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d("Permissions", "Notification permission granted")
                    // Cập nhật UI nếu cần
                } else {
                    Log.d("Permissions", "Notification permission denied")
                    Toast.makeText(
                        this,
                        "Bạn sẽ không nhận được thông báo về lịch hẹn",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}