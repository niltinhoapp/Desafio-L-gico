@file:Suppress("unused")

package com.desafiolgico.information

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.desafiolgico.databinding.ItemOnboardingPageBinding

data class OnboardingItem(val imageResId: Int, val title: String, val description: String)

@Suppress("unused")
class OnboardingAdapter(private var onboardingItems: List<OnboardingItem>) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding =
            ItemOnboardingPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(onboardingItems[position])
    }

    override fun getItemCount(): Int = onboardingItems.size

    inner class OnboardingViewHolder(private val binding: ItemOnboardingPageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(onboardingItem: OnboardingItem) {
            binding.imageView.setImageResource(onboardingItem.imageResId)
            binding.textViewTitle.text = onboardingItem.title
            binding.textViewDescription.text = onboardingItem.description
        }
    }

    // Método para atualizar a lista de itens com DiffUtil
    fun updateOnboardingItems(newItems: List<OnboardingItem>) {
        val diffCallback = OnboardingDiffCallback(onboardingItems, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        onboardingItems = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    // Classe DiffUtil.Callback corrigida e implementada fora do ViewHolder
    class OnboardingDiffCallback(
        private val oldList: List<OnboardingItem>,
        private val newList: List<OnboardingItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].title == newList[newItemPosition].title

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }
}
