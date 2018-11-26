package io.github.pulakdp.autodownloadplayer.data

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.util.*

/**
 * Created by pulak on 24/11/18.
 */
interface VideoApi {

    @Streaming
    @GET
    fun downloadVideo(@Url videoUrl: String): Observable<ResponseBody>

}