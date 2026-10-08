package com.rb.background.features.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rb.background.databinding.ItemQuickToolBinding
import com.rb.background.features.home.models.QuickTool
import android.graphics.drawable.GradientDrawable

class QuickToolsAdapter(
    private val quickTools: List<QuickTool> = emptyList(),
    private val onToolClick: (QuickTool) -> Unit
) : RecyclerView.Adapter<QuickToolsAdapter.QuickToolViewHolder>() {

    class QuickToolViewHolder(
        val binding: ItemQuickToolBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): QuickToolViewHolder {

        val binding = ItemQuickToolBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return QuickToolViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: QuickToolViewHolder,
        position: Int
    ) {
        val quickTool = quickTools[position]

        holder.binding.tvToolTitle.text = quickTool.title
        holder.binding.tvToolDescription.text = quickTool.description
        holder.binding.imgToolIcon.setImageResource(quickTool.iconResId)
        val visualBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 18f
            setColor(quickTool.backgroundColor)
        }

        holder.binding.toolVisualContainer.background = visualBackground
        holder.itemView.setOnClickListener {
            onToolClick(quickTool)
        }
    }

    override fun getItemCount(): Int {
        return quickTools.size
    }
}