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
//                // Thay đổi background dựa trên trạng thái đọc
//                root.setBackgroundResource(
//                    if (notification.read)
//                        R.color.white
//                    else
//                        R.color.gray
//                )
//
//                // Thay đổi style text title
//                tvNotificationTitle.apply {
//                    setTypeface(null, if (notification.read) Typeface.NORMAL else Typeface.BOLD)
//                }

                tvNotificationTitle.text = notification.title
                tvNotificationMessage.text = notification.message
                tvNotificationTime.text = notification.time.getTimeAgo()
                
                // Set icon dựa vào type
                ivNotificationIcon.setImageResource(
                    when (notification.type) {
                        NotificationType.APPOINTMENT -> R.drawable.calendar_menu
                        NotificationType.SYSTEM -> R.drawable.ic_info
                        NotificationType.REMINDER -> R.drawable.notification
                    }
                )

                // Thêm click listener để đánh dấu là đã đọc
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

