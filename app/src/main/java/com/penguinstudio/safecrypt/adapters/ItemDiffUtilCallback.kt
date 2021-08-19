package com.penguinstudio.safecrypt.adapters

import androidx.recyclerview.widget.DiffUtil
import com.penguinstudio.safecrypt.services.glide_service.IPicture


class ItemDiffUtilCallback(private val old: List<IPicture>,
                           private val new: List<IPicture>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = old.size

    override fun getNewListSize(): Int = new.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return old[oldItemPosition].uri === new[newItemPosition].uri
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return old[oldItemPosition] === new[newItemPosition]
    }
}