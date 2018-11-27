package io.github.pulakdp.autodownloadplayer.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Toast
import io.github.pulakdp.autodownloadplayer.R
import io.github.pulakdp.autodownloadplayer.adapter.VideoFileAdapter
import kotlinx.android.synthetic.main.activity_video_list.*


class VideoListActivity : AppCompatActivity(),
    VideoFileAdapter.OnVideoItemClickListener {

    private lateinit var videoUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_list)
        setUpRecyclerView()
    }

    private fun setUpRecyclerView() {
        videoList.adapter = VideoFileAdapter(this, this)
        videoList.layoutManager = LinearLayoutManager(this)
        videoList.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))
    }

    override fun onVideoItemClick(videoUrl: String) {
        this.videoUrl = videoUrl
        requestPermissionAndPlayVideo()
    }

    private fun requestPermissionAndPlayVideo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                Toast.makeText(this, "Need storage permission to store videos while watching", Toast.LENGTH_SHORT).show()
                //Opens permissions screen of this app
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    14
                )
            }
        } else {
            // Permission has already been granted
            openVideoPlayerActivity()
        }
    }

    private fun openVideoPlayerActivity() {
        val intent = Intent(this, VideoPlayerActivity::class.java)
        intent.putExtra("video_url", videoUrl)
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != 14 || resultCode != Activity.RESULT_OK) {
            Toast.makeText(this, "Need storage permission to save video to storage", Toast.LENGTH_SHORT).show()
            return
        }
        openVideoPlayerActivity()
    }
}
