package com.healthtech.doccareplus.ui.chat

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.FragmentChatBinding
import com.healthtech.doccareplus.utils.SnackbarUtils
import com.zegocloud.zimkit.common.ZIMKitRouter
import com.zegocloud.zimkit.common.enums.ZIMKitConversationType
import com.zegocloud.zimkit.components.conversation.ui.ZIMKitConversationFragment
import com.zegocloud.zimkit.services.ZIMKit
import dagger.hilt.android.AndroidEntryPoint
import im.zego.zim.enums.ZIMErrorCode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Arrays

@AndroidEntryPoint
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private val args: ChatFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        requireActivity().onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigateBack()
                }
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Luôn thực hiện kết nối lại để đảm bảo phiên hoạt động
        binding.progressBar.visibility = View.VISIBLE
        binding.zimkitContainer.visibility = View.GONE
        reconnectZIMKit()
        observeUiState()
        setupToolbar()
        
        // Bắt đầu chat với ID người dùng đã được chuyển qua
        args.conversationId?.let { conversationId ->
            if (conversationId.isNotEmpty()) {
                viewModel.handleEvent(ChatEvent.StartChat(conversationId))
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.apply { 
            setNavigationOnClickListener { navigateBack() }
            title = args.title
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is ChatUiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    is ChatUiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                    }

                    is ChatUiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        SnackbarUtils.showErrorSnackbar(binding.root, state.message)
                    }

                    is ChatUiState.GroupCreated -> {
                        binding.progressBar.visibility = View.GONE
                        startChat(state.groupId, ZIMKitConversationType.ZIMKitConversationTypeGroup)
                    }

                    is ChatUiState.GroupJoined -> {
                        binding.progressBar.visibility = View.GONE
                        startChat(state.groupId, ZIMKitConversationType.ZIMKitConversationTypeGroup)
                    }

                    is ChatUiState.ChatStarted -> {
                        binding.progressBar.visibility = View.GONE
                        startChatWithUser(state.userId)
                    }

                    else -> {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun setupZIMKitFragment() {
        // Thêm ZIMKitConversationFragment vào container
        childFragmentManager.beginTransaction()
            .replace(R.id.zimkit_container, ZIMKitConversationFragment()).commit()

        binding.zimkitContainer.visibility = View.VISIBLE
    }

    private fun reconnectZIMKit() {
        // Lấy thông tin người dùng từ UserPreferences
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("user_id", "") ?: ""
        val userName = prefs.getString("user_name", "") ?: ""

        if (userId.isNotEmpty() && userName.isNotEmpty()) {
            binding.progressBar.visibility = View.VISIBLE
            val avatarUrl = "https://storage.zego.im/IMKit/avatar/avatar-0.png"

            ZIMKit.connectUser(userId, userName, avatarUrl) { error ->
                binding.progressBar.visibility = View.GONE
                if (error.code == ZIMErrorCode.SUCCESS) {
                    setupZIMKitFragment()
                } else {
                    SnackbarUtils.showErrorSnackbar(
                        binding.root, "Kết nối chat thất bại: ${error.message}"
                    )
                }
            }
        } else {
            binding.progressBar.visibility = View.GONE
            SnackbarUtils.showErrorSnackbar(
                binding.root, "Vui lòng đăng nhập lại để sử dụng tính năng chat"
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.chat_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.newChat -> {
                showNewChatDialog()
                return true
            }

            R.id.newGroup -> {
                showNewGroupDialog()
                return true
            }

            R.id.joinGroup -> {
                showJoinGroupDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showNewChatDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Bắt đầu cuộc trò chuyện mới")

        val targetUserEdt = EditText(requireContext())
        targetUserEdt.hint = "Nhập ID người dùng"
        builder.setView(targetUserEdt)

        builder.setPositiveButton("Bắt đầu") { _, _ ->
            val targetUserId = targetUserEdt.text.toString()
            if (targetUserId.isEmpty()) {
                SnackbarUtils.showErrorSnackbar(binding.root, "Vui lòng nhập ID người dùng")
                return@setPositiveButton
            }
            viewModel.handleEvent(ChatEvent.StartChat(targetUserId))
        }

        builder.setNegativeButton("Hủy", null)
        val dialog = builder.create()
        dialog.show()
    }

    private fun startChat(conversationID: String, type: ZIMKitConversationType) {
        ZIMKitRouter.toMessageActivity(requireContext(), conversationID, type)
    }

    private fun showNewGroupDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Tạo nhóm mới")

        val groupNameEdt = EditText(requireContext())
        groupNameEdt.hint = "Tên nhóm"
        val groupIdEdt = EditText(requireContext())
        groupIdEdt.hint = "ID nhóm"
        val groupUserIdEdt = EditText(requireContext())
        groupUserIdEdt.hint = "ID người dùng (ngăn cách bằng dấu ;)"

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            addView(groupNameEdt)
            addView(groupIdEdt)
            addView(groupUserIdEdt)
        }
        builder.setView(layout)

        builder.setPositiveButton("Tạo nhóm") { _, _ ->
            val groupName = groupNameEdt.text.toString()
            val groupId = groupIdEdt.text.toString()
            val groupUserId =
                groupUserIdEdt.text.toString().split(";".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()

            if (groupName.isEmpty() || groupId.isEmpty() || groupUserId.isEmpty()) {
                SnackbarUtils.showErrorSnackbar(binding.root, "Vui lòng điền đầy đủ thông tin")
                return@setPositiveButton
            }

            viewModel.handleEvent(
                ChatEvent.CreateGroup(
                    groupName, groupId, ArrayList(Arrays.asList(*groupUserId))
                )
            )
        }

        builder.setNegativeButton("Hủy", null)
        val dialog = builder.create()
        dialog.show()
    }

    private fun showJoinGroupDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Tham gia nhóm")

        val joinGroupIdEdt = EditText(requireContext())
        joinGroupIdEdt.hint = "Nhập ID nhóm"
        builder.setView(joinGroupIdEdt)

        builder.setPositiveButton("Tham gia") { _, _ ->
            val groupId = joinGroupIdEdt.text.toString()
            if (groupId.isEmpty()) {
                SnackbarUtils.showErrorSnackbar(binding.root, "Vui lòng nhập ID nhóm")
                return@setPositiveButton
            }
            viewModel.handleEvent(ChatEvent.JoinGroup(groupId))
        }

        builder.setNegativeButton("Hủy", null)
        val dialog = builder.create()
        dialog.show()
    }

    private fun navigateBack() {
        findNavController().navigate(R.id.action_chat_to_previous)
    }

    private fun startChatWithUser(userId: String) {
        // Log để debug
        Log.d("ChatFragment", "Starting chat with user ID: $userId")
        
        try {
            // Đảm bảo userId là string và có định dạng hợp lệ
            val zimKitUserId = userId.trim()
            
            // Mở cuộc hội thoại trực tiếp - giống với ví dụ ChatAppV1
            ZIMKitRouter.toMessageActivity(
                requireContext(),
                zimKitUserId,
                ZIMKitConversationType.ZIMKitConversationTypePeer
            )
        } catch (e: Exception) {
            Log.e("ChatFragment", "Error starting chat: ${e.message}", e)
            SnackbarUtils.showErrorSnackbar(binding.root, "Lỗi khi bắt đầu trò chuyện: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}