package com.rb.background.features.templates.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rb.background.databinding.ItemTemplateBinding
import com.rb.background.features.templates.model.Photo
import com.bumptech.glide.Glide

class TemplateAdapter : RecyclerView.Adapter<TemplateAdapter.TemplateViewHolder>() {

    private val templateList = mutableListOf<Photo>()
    private var onTemplateClick: ((Photo) -> Unit)? = null


    fun setOnTemplateClickListener(
        listener: (Photo) -> Unit
    ) {
        onTemplateClick = listener
    }

    class TemplateViewHolder(
        val binding: ItemTemplateBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TemplateViewHolder {

        val binding =
            ItemTemplateBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return TemplateViewHolder(binding)

    }

    override fun onBindViewHolder(
        holder: TemplateViewHolder,
        position: Int
    ) {

        val photo = templateList[position]

        Glide.with(holder.itemView.context)
            .load(photo.src.medium)
            .into(holder.binding.imgTemplate)
        holder.itemView.setOnClickListener {

            onTemplateClick?.invoke(photo)

        }

    }
    override fun getItemCount(): Int {

        return templateList.size

    }

    fun submitList(
        photos: List<Photo>
    ) {

        templateList.clear()

        templateList.addAll(photos)

        notifyDataSetChanged()

    }

}