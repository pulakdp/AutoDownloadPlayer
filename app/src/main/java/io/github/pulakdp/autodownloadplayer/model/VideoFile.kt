package io.github.pulakdp.autodownloadplayer.model

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.Charset


/**
 * Created by pulak on 25/11/18.
 */
class VideoFile(
    val title: String,
    val description: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val videoUrl: String
) {

    companion object {
        fun parseJsonToVideoFile(context: Context): List<VideoFile> {
            val videoFileList = ArrayList<VideoFile>()
            try {
                val jsonObject = JSONObject(loadJSONFromAsset(context, "video_objects.json"))
                val jsonArray = jsonObject.getJSONArray("videos")

                for (i in 0 until jsonArray.length()) {
                    val innerJsonObject = jsonArray.getJSONObject(i)
                    videoFileList.add(
                        VideoFile(
                            innerJsonObject.optString("title"),
                            innerJsonObject.optString("description"),
                            innerJsonObject.optString("subtitle"),
                            innerJsonObject.optString("thumb"),
                            innerJsonObject.optString("sources")
                        )
                    )
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return videoFileList
        }

        private fun loadJSONFromAsset(context: Context, fileName: String): String? {
            val json: String?
            try {
                val inputStream = context.assets.open(fileName)
                val size = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                inputStream.close()
                json = String(buffer, Charset.forName("UTF-8"))
            } catch (ex: IOException) {
                ex.printStackTrace()
                return null
            }
            return json
        }
    }
}