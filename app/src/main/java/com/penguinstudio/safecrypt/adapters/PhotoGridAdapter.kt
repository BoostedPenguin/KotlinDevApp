package com.penguinstudio.safecrypt.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.models.PhotoModel

class PhotoGridAdapter : RecyclerView.Adapter<PhotoGridAdapter.ImageHolder>() {

    // Replace with data model
    private var images: ArrayList<PhotoModel> = ArrayList()
    private var itemClickListener: OnItemClickListener? = null



    fun setImages(images: ArrayList<PhotoModel>?) {
        if (images != null) {
            this.images = images
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageHolder {
        val itemView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.pictures_grid_fragment, parent, false)

        return ImageHolder(itemView)
    }

    override fun onBindViewHolder(holder: ImageHolder, position: Int) {
        val currentImage = images[position]
        holder.image = currentImage
        val startTime = System.currentTimeMillis()

        Glide.with(holder.itemView)
            .load(currentImage.photoUri)
            .fitCenter()
            .apply(RequestOptions().override(100, 100))
            .into(holder.imageView);

        Log.d("loadTime", "Time it took to create bitmap: ${System.currentTimeMillis() - startTime}ms")

    }

    override fun getItemCount(): Int {
        return images.size
    }

    interface OnItemClickListener {
        fun onContactButtonClick(position: Int, image: PhotoModel)
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        this.itemClickListener = listener
    }

    inner class ImageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal lateinit var image: PhotoModel
        internal var imageView: ImageView = itemView.findViewById(R.id.picturesInnerImage)

        init {
            imageView.setOnClickListener {
                val position = adapterPosition

                if( itemClickListener != null && position != RecyclerView.NO_POSITION) {
                    itemClickListener!!.onContactButtonClick(position, image)
                }
            }
        }
    }
}