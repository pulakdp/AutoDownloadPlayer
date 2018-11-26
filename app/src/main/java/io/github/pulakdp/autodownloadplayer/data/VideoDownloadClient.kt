package io.github.pulakdp.autodownloadplayer.data

import io.reactivex.schedulers.Schedulers
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory

/**
 * Created by pulak on 24/11/18.
 */
class VideoDownloadClient {

    companion object {

        private var retrofit: Retrofit? = null

        fun getClient(): Retrofit {
            if (retrofit == null)
                retrofit = Retrofit.Builder()
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                    .baseUrl("http://google.com")
                    .build()
            return retrofit!!
        }
    }
}