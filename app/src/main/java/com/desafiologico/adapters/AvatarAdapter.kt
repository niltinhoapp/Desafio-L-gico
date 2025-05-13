package com.desafiologico.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.desafiologico.R

class AvatarAdapter(
    private val avatars: List<Int>,
    private val onAvatarClick: (Int) -> Unit
) : RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder>() {

    inner class AvatarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatarImageView: ImageView = itemView.findViewById(R.id.avatarImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_avatar, parent, false)
        return AvatarViewHolder(view)
    }

    private var selectedPosition = RecyclerView.NO_POSITION

    override fun onBindViewHolder(holder: AvatarViewHolder, position: Int) {
        val avatar = avatars[position]
        holder.avatarImageView.setImageResource(avatar)

        // Aplicar estilo visual ao item selecionado
        holder.avatarImageView.alpha = if (position == selectedPosition) 1.0f else 0.5f

        holder.avatarImageView.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            val previousPosition = selectedPosition
            selectedPosition = adapterPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            onAvatarClick(avatars[adapterPosition])
        }
    }


    override fun getItemCount(): Int = avatars.size
}