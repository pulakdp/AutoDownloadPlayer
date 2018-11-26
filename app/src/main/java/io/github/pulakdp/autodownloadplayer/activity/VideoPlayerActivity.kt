package io.github.pulakdp.autodownloadplayer.activity

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import io.github.pulakdp.autodownloadplayer.R
import io.github.pulakdp.autodownloadplayer.data.DataSync
import io.github.pulakdp.autodownloadplayer.data.DataSync.Companion.fileLength
import io.github.pulakdp.autodownloadplayer.data.DataSync.Companion.readByte
import io.github.pulakdp.autodownloadplayer.data.StreamingServer
import io.github.pulakdp.autodownloadplayer.data.VideoApi
import io.github.pulakdp.autodownloadplayer.data.VideoDownloadClient
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_video_player.*
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream


class VideoPlayerActivity : AppCompatActivity(),
    StreamingServer.LocalServerCallback, Player.EventListener {

    private val LOG_TAG = VideoPlayerActivity::class.java.simpleName

    private val videoApi = VideoDownloadClient.getClient().create(VideoApi::class.java)

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var httpFactory: DefaultHttpDataSourceFactory

    private lateinit var videoUrl: String
    private lateinit var videoFile: File

    private var isPlayingLocally: Boolean = false

    private val compositeDisposable = CompositeDisposable()
    private var streamingServer: StreamingServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_video_player)
        supportActionBar?.hide()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        videoUrl = intent.getStringExtra("video_url")

        exoPlayer = ExoPlayerFactory.newSimpleInstance(this, DefaultTrackSelector())
        exoPlayer.addListener(this)
        httpFactory = DefaultHttpDataSourceFactory(packageName, DefaultBandwidthMeter())
        playerView.player = exoPlayer

        videoFile =
                File(
                    "${Environment.getExternalStorageDirectory()}" +
                            "${File.separator}${videoUrl.substring(videoUrl.lastIndexOf("/") + 1)}"
                )

        //onServerStart(videoUrl)
        if (getDownloadInformation(videoUrl.substring(videoUrl.lastIndexOf("/") + 1))) {
            playLocally(videoFile.path)
        } else {
            fetchVideo()
        }
    }

    private fun fetchVideo() {
        compositeDisposable.add(
            videoApi.downloadVideo(videoUrl)
                .subscribeOn(Schedulers.io())
                .subscribe(
                    this::saveVideo,
                    this::handleError
                )
        )
    }

    override fun onStop() {
        super.onStop()
        streamingServer?.stopService()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
        exoPlayer.release()
    }

    private fun saveVideo(responseBody: ResponseBody) {
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            DataSync.reset()
            streamingServer = StreamingServer(videoFile, "localhost", this)
            streamingServer?.setUpLocalServer()
            streamingServer?.startServer()

            val fileReader = ByteArray(1024 * 50)

            val fileSize = responseBody.contentLength()
            fileLength = fileSize
            var fileSizeDownloaded: Long = 0

            inputStream = responseBody.byteStream()
            outputStream = FileOutputStream(videoFile)

            while (true) {
                val read = inputStream!!.read(fileReader)
                if (read == -1) {
                    break
                }
                readByte += read
                outputStream.write(fileReader, 0, read)
                fileSizeDownloaded += read.toLong()
            }
            if (fileSizeDownloaded == fileSize) {
                Log.d(LOG_TAG, "Download Complete")
                saveDownloadInformation(videoUrl.substring(videoUrl.lastIndexOf("/") + 1))
            }
            outputStream.flush()

        } catch (e: Exception) {
            Log.d(LOG_TAG, "Exception occurred: $e")
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }

    private fun handleError(t: Throwable) {
        Toast.makeText(this, "Couldn't fetch video\n${t.message}", Toast.LENGTH_SHORT).show()
    }

    override fun onServerStart(streamUrl: String) {
        Log.d(LOG_TAG, "Streaming at $streamUrl")
        runOnUiThread {
            val mediaSource = ExtractorMediaSource.Factory(httpFactory).createMediaSource(Uri.parse(streamUrl))
            exoPlayer.prepare(mediaSource)
            exoPlayer.playWhenReady = true
            isPlayingLocally = false
            Toast.makeText(this, "Playing from web", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playLocally(streamUrl: String) {
        val mediaSource = ExtractorMediaSource.Factory(DefaultDataSourceFactory(this, packageName))
            .setExtractorsFactory(DefaultExtractorsFactory())
            .createMediaSource(Uri.parse(streamUrl + "sss"))
        exoPlayer.prepare(mediaSource)
        exoPlayer.playWhenReady = true
        isPlayingLocally = true
        Toast.makeText(this, "Playing locally", Toast.LENGTH_SHORT).show()
    }

    override fun onPlayerError(error: ExoPlaybackException?) {
        if (isPlayingLocally && ExoPlaybackException.TYPE_SOURCE == error?.type &&
            error?.sourceException.message!!.contains("java.io.FileNotFoundException")) {
            Toast.makeText(this, "Couldn't play locally, playing from web", Toast.LENGTH_SHORT).show()
            fetchVideo()
        }
    }

    override fun onLoadingChanged(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun saveDownloadInformation(fileName: String) {
        val sharedPref = getSharedPreferences("downloadInfo", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean(fileName, true).apply()
    }

    private fun getDownloadInformation(fileName: String): Boolean {
        val sharedPref = getSharedPreferences("downloadInfo", Context.MODE_PRIVATE)
        return sharedPref.getBoolean(fileName, false)
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        if ((playbackState == Player.STATE_READY) && playWhenReady) {
            onLoadingChanged(false)
        }
    }
}
