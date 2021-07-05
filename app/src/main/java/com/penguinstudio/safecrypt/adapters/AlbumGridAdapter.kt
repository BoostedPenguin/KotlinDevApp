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
import com.penguinstudio.safecrypt.models.AlbumModel

class AlbumGridAdapter : RecyclerView.Adapter<AlbumGridAdapter.AlbumHolder>() {

    // Replace with data model
    private var albums: ArrayList<AlbumModel> = ArrayList()
    private var itemClickListener: OnItemClickListener? = null

    fun setAlbums(albums: ArrayList<AlbumModel>) {
        this.albums = albums
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumHolder {
        val itemView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.album_grid_item, parent, false)

        return AlbumHolder(itemView)
    }

    override fun onBindViewHolder(holder: AlbumHolder, position: Int) {
        val currentAlbum = albums[position]
        holder.image = currentAlbum
        val startTime = System.currentTimeMillis()

        Glide.with(holder.itemView)
            .load(currentAlbum.coverUri)
            .fitCenter()
            .apply(RequestOptions().override(100, 100))
            .into(holder.imageView);

        Log.d(
            "loadTime",
            "Time it took to create bitmap: ${System.currentTimeMillis() - startTime}ms"
        )

    }

    override fun getItemCount(): Int {
        return albums.size
    }

    interface OnItemClickListener {
        fun onContactButtonClick(position: Int, image: AlbumModel)
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        this.itemClickListener = listener
    }

    inner class AlbumHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal lateinit var image: AlbumModel
        internal var imageView: ImageView = itemView.findViewById(R.id.albumInnerImage)

        init {
            imageView.setOnClickListener {
                val position = adapterPosition

                if (itemClickListener != null && position != RecyclerView.NO_POSITION) {
                    itemClickListener!!.onContactButtonClick(position, image)
                }
            }
        }
    }
}
