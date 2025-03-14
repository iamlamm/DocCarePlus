package com.healthtech.doccareplus.ui.notification.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.ItemNotificationBinding
import com.healthtech.doccareplus.domain.model.Notification
import com.healthtech.doccareplus.domain.model.NotificationType
import com.healthtech.doccareplus.utils.getTimeAgo

class NotificationAdapter :
    ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            binding.apply {
                // Thay đổi background dựa trên trạng thái đọc
                root.setBackgroundResource(
                    if (notification.read)
                        R.color.white
                    else
                        R.color.unread_notification_bg
                )

                // Thay đổi style text title và message
                tvNotificationTitle.apply {
                    setTypeface(null, if (notification.read) Typeface.NORMAL else Typeface.BOLD)
                    setTextColor(
                        context.getColor(
                            if (notification.read) R.color.text_primary
                            else R.color.text_dark
                        )
                    )
                }

                tvNotificationMessage.apply {
                    setTextColor(
                        context.getColor(
                            if (notification.read) R.color.text_secondary
                            else R.color.text_primary
                        )
                    )
                }

                // Nội dung thông báo
                tvNotificationTitle.text = notification.title
                tvNotificationMessage.text = notification.message
                tvNotificationTime.text = notification.time.getTimeAgo()

                // Icon thông báo
                ivNotificationIcon.apply {
                    setImageResource(
                        when (notification.type) {
                            NotificationType.APPOINTMENT_BOOKED -> R.drawable.calendar_menu
                            NotificationType.NEW_APPOINTMENT -> R.drawable.calendar_menu
                            NotificationType.APPOINTMENT_CANCELLED -> R.drawable.ic_cancel
                            NotificationType.APPOINTMENT_REMINDER -> R.drawable.notification
                            NotificationType.SYSTEM -> R.drawable.ic_info
                            NotificationType.APPOINTMENT_COMPLETED -> TODO()
                            NotificationType.ADMIN_NEW_APPOINTMENT -> TODO()
                        }
                    )
                    // Thay đổi alpha của icon nếu đã đọc
                    alpha = if (notification.read) 0.7f else 1.0f
                }

                // Click listener
                root.setOnClickListener {
                    if (!notification.read) {
                        onNotificationClick?.invoke(notification.id)
                    }
                }
            }
        }
    }

    private var onNotificationClick: ((String) -> Unit)? = null

    fun setOnNotificationClickListener(listener: (String) -> Unit) {
        onNotificationClick = listener
    }
}

