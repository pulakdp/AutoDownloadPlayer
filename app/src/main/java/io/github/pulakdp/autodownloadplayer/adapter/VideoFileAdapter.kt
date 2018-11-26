package io.github.pulakdp.autodownloadplayer.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import io.github.pulakdp.autodownloadplayer.R
import io.github.pulakdp.autodownloadplayer.model.VideoFile
import kotlinx.android.synthetic.main.item_video_file.view.*

/**
 * Created by pulak on 25/11/18.
 */
class VideoFileAdapter(private val context: Context, private val onVideoItemClickListener: OnVideoItemClickListener): RecyclerView.Adapter<VideoFileAdapter.VideoFileViewHolder>() {

    private var videoList: List<VideoFile> = VideoFile.parseJsonToVideoFile(context)

    interface OnVideoItemClickListener {
        fun onVideoItemClick(videoUrl: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoFileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video_file, parent, false)
        return VideoFileViewHolder(view)
    }

    override fun getItemCount(): Int {
        return videoList.size
    }

    override fun onBindViewHolder(viewHolder: VideoFileViewHolder, position: Int) {
        viewHolder.title.text = videoList[position].title
        viewHolder.subtitle.text = videoList[position].subtitle
        viewHolder.description.text = videoList[position].description
        Glide.with(context)
            .load(videoList[position].thumbnailUrl)
            .apply(RequestOptions().centerCrop())
            .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE))
            .into(viewHolder.thumbnail)
    }

    inner class VideoFileViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.title
        val subtitle: TextView = itemView.subtitle
        val description: TextView = itemView.description
        val thumbnail: ImageView = itemView.thumbnail

        init {
            itemView.setOnClickListener {
                onVideoItemClickListener.onVideoItemClick(videoList[adapterPosition].videoUrl)
            }
        }
    }
}