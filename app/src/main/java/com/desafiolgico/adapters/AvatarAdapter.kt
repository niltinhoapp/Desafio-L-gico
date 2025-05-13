@file:Suppress("DEPRECATION")

package com.desafiolgico.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.desafiolgico.R
import com.desafiolgico.utils.CoinManager

class AvatarAdapter(
    private val avatars: List<Int>,
    private val onAvatarClick: (Int) -> Unit
) : RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    inner class AvatarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatarImage: ImageView = itemView.findViewById(R.id.avatarImageView)
        val lockIcon: ImageView = itemView.findViewById(R.id.lockIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_avatar, parent, false)
        return AvatarViewHolder(view)
    }

    override fun onBindViewHolder(holder: AvatarViewHolder, position: Int) {
        val avatarResId = avatars[position]
        holder.avatarImage.setImageResource(avatarResId)

        val isFreeAvatar = avatarResId == R.drawable.avatar1
        val isUnlocked = isFreeAvatar ||
            CoinManager.isAvatarUnlocked(holder.itemView.context, avatarResId)

        holder.lockIcon.visibility = if (isUnlocked) View.GONE else View.VISIBLE
        holder.avatarImage.alpha = when {
            !isUnlocked -> 0.35f
            position == selectedPosition -> 1.0f
            else -> 0.6f
        }

        holder.itemView.setOnClickListener {
            // ✅ pega a posição REAL no momento do clique
            val clickedPos = holder.bindingAdapterPosition
            if (clickedPos == RecyclerView.NO_POSITION) return@setOnClickListener

            val clickedAvatarResId = avatars[clickedPos]
            val free = clickedAvatarResId == R.drawable.avatar1
            val unlocked = free || CoinManager.isAvatarUnlocked(holder.itemView.context, clickedAvatarResId)
            if (!unlocked) return@setOnClickListener

            val previous = selectedPosition
            selectedPosition = clickedPos

            if (previous != RecyclerView.NO_POSITION) notifyItemChanged(previous)
            notifyItemChanged(selectedPosition)

            onAvatarClick(clickedAvatarResId)
        }
    }

    override fun getItemCount(): Int = avatars.size
}
