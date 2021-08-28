package com.penguinstudio.safecrypt.adapters

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.models.AlbumModel

class AlbumsAdapter(private var listener: AdapterListeners?,
                    private var fullRequest: RequestBuilder<Drawable>) :
    RecyclerView.Adapter<AlbumsAdapter.AlbumHolder>(),
    ListPreloader.PreloadModelProvider<AlbumModel> {

    interface AdapterListeners {
        fun onImageClickListener(position: Int, album: AlbumModel)
    }

    // Replace with data model
    private var albums: ArrayList<AlbumModel> = ArrayList()

    @SuppressLint("NotifyDataSetChanged")
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
        holder.bind(albums[position])
    }

    override fun getItemCount(): Int {
        return albums.size
    }

    inner class AlbumHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private lateinit var image: AlbumModel
        private var imageView: ImageView = itemView.findViewById(R.id.albumInnerImage)
        private var mainText: TextView = itemView.findViewById(R.id.albumMainContent)
        private var secondaryText: TextView = itemView.findViewById(R.id.albumSecondaryContent)

        init {
            imageView.setOnClickListener {
                val position = bindingAdapterPosition

                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener!!.onImageClickListener(position, image)
                }
            }
        }

        fun bind(image: AlbumModel) {
            this.image = image

            var currentAlbumName = image.name ?: "Unknown"

            if(currentAlbumName.length > 10) {
                currentAlbumName = currentAlbumName.substring(0, 10).plus("...")
            }
            mainText.text = currentAlbumName
            secondaryText.text = image.albumMedia.size.toString()

            fullRequest
                .load(image.coverUri)
                .placeholder(R.drawable.ic_baseline_image_24)
                .fitCenter()
                .into(imageView)
        }
    }

    override fun getPreloadItems(position: Int): MutableList<AlbumModel> {
        return albums.subList(position, position + 1)
    }

    override fun getPreloadRequestBuilder(item: AlbumModel): RequestBuilder<*> {
        return fullRequest.clone()
    }
}
