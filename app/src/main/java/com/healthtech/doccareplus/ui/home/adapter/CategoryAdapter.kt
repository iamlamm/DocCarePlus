package com.healthtech.doccareplus.ui.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.domain.model.Category
import com.healthtech.doccareplus.databinding.ItemCategoryBinding

class CategoryAdapter : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {
    private var categories = listOf<Category>()
    private var onCategoryClickListener: ((Category) -> Unit)? = null

    // Sử dụng DiffUtil để cập nhật RecyclerView hiệu quả hơn
    fun setCategories(newCategories: List<Category>) {
        val diffCallback = CategoryDiffCallback(categories, newCategories)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        categories = newCategories
        diffResult.dispatchUpdatesTo(this)
    }

    // Thêm listener để xử lý sự kiện click
    fun setOnCategoryClickListener(listener: (Category) -> Unit) {
        onCategoryClickListener = listener
    }

    inner class CategoryViewHolder(private val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            binding.apply {
                // Cập nhật text
                tvCategory.text = category.name

                // Tối ưu hóa Glide với cache và placeholder
                val requestOptions = RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.drawable.cardiology)

                // Load ảnh với cấu hình đã tối ưu
                Glide.with(root.context)
                    .load(category.icon)
                    .apply(requestOptions)
                    .into(ivCategory)

                // Xử lý sự kiện click
                root.setOnClickListener {
                    onCategoryClickListener?.invoke(category)
                }

                // Click cho cả hình ảnh
                ivCategory.setOnClickListener {
                    onCategoryClickListener?.invoke(category)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun getItemCount(): Int = categories.size

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }
}