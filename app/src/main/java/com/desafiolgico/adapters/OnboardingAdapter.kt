package com.desafiolgico.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.desafiolgico.R


/**
 * Adapter responsável por exibir os slides de introdução (onboarding).
 * Cada slide é representado por um OnboardingItem (imagem, título e descrição).
 */

data class OnboardingItem(
    val imageRes: Int,      // ID do recurso da imagem
    val title: String,      // Título do slide
    val description: String // Descrição do slide
)

class OnboardingAdapter(
    private val onboardingItems: List<OnboardingItem>
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    inner class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView: ImageView = view.findViewById(R.id.onboardingImage)
        private val titleView: TextView = view.findViewById(R.id.onboardingTitle)
        private val descriptionView: TextView = view.findViewById(R.id.onboardingDescription)

        fun bind(item: OnboardingItem) {
            imageView.setImageResource(item.imageRes)
            titleView.text = item.title
            descriptionView.text = item.description
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(onboardingItems[position])
    }

    override fun getItemCount(): Int = onboardingItems.size
}
