package com.rb.background.features.backgroundremover.ui

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rb.background.databinding.ItemFontOptionBinding
import com.rb.background.features.backgroundremover.utils.GoogleFontsHelper

class FontOptionAdapter(
    private val fontNames: List<String>,
    private val onFontClick: (String) -> Unit
) : RecyclerView.Adapter<FontOptionAdapter.FontViewHolder>() {

    // Family name of the font currently applied to the selected text,
    // null for the app's default typeface. Used to highlight the
    // active chip.
    var selectedFontName: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class FontViewHolder(
        val binding: ItemFontOptionBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FontViewHolder {

        val binding = ItemFontOptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return FontViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: FontViewHolder,
        position: Int
    ) {

        val fontName =
            if (position == 0) "Default" else fontNames[position - 1]

        holder.binding.tvFontName.text = fontName

        val isSelected =
            if (position == 0) {
                selectedFontName == null
            } else {
                selectedFontName == fontName
            }

        holder.binding.tvFontName.setBackgroundResource(
            if (isSelected) {
                com.rb.background.R.drawable.bg_format_chip_active
            } else {
                com.rb.background.R.drawable.bg_format_chip
            }
        )

        holder.binding.tvFontName.setTextColor(
            if (isSelected) 0xFFFFFFFF.toInt() else 0xFF403A48.toInt()
        )

        holder.binding.tvFontName.typeface =
            Typeface.DEFAULT

        if (position != 0) {

            GoogleFontsHelper.load(
                holder.itemView.context,
                fontName
            ) { typeface ->

                // The view may have been rebound to a different
                // position by the time the async load finishes.
                if (holder.bindingAdapterPosition == position) {
                    holder.binding.tvFontName.typeface = typeface
                }
            }
        }

        holder.binding.tvFontName.setOnClickListener {

            onFontClick(
                if (position == 0) "" else fontName
            )
        }
    }

    override fun getItemCount(): Int {

        return fontNames.size + 1
    }
}
