package com.healthtech.doccareplus.ui.category

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.ItemAllCategoryBinding
import com.healthtech.doccareplus.domain.model.Category

class AllCategoriesAdapter : RecyclerView.Adapter<AllCategoriesAdapter.AllCategoriesViewHolder>() {
    private var categories = listOf<Category>()

    @SuppressLint("NotifyDataSetChanged")
    fun setCategories(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    inner class AllCategoriesViewHolder(private val binding: ItemAllCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(category: Category) {
            binding.apply {
                tvCategoryName.text = category.name
                Glide.with(root.context).load(category.icon).error(R.drawable.cardiology)
                    .into(ivCategory)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllCategoriesViewHolder {
        val binding =
            ItemAllCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AllCategoriesViewHolder(binding)
    }

    override fun getItemCount(): Int = categories.size

    override fun onBindViewHolder(holder: AllCategoriesViewHolder, position: Int) {
        holder.bind(categories[position])
    }
}