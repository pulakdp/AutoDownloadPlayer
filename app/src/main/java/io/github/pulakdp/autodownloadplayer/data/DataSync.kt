package io.github.pulakdp.autodownloadplayer.data

/**
 * Created by pulak on 24/11/18.
 */
class DataSync {

    companion object {

        const val DATA_READY = 1
        const val DATA_DOWNLOADING = 2
        const val DATA_DOWNLOADED = 3
        const val DATA_NOT_AVAILABLE = 4

        var fileLength: Long = -1
        var readByte: Int = 0
        var consumeByte: Int = 0
        var dataStatus = DATA_NOT_AVAILABLE

        fun isDataReady(): Boolean {
            return when {
                fileLength == readByte.toLong() -> {
                    dataStatus = DATA_DOWNLOADED
                    false
                }
                readByte > consumeByte -> {
                    dataStatus = DATA_READY
                    true
                }
                readByte <= consumeByte -> {
                    dataStatus = DATA_DOWNLOADING
                    false
                }
                fileLength == -1L -> {
                    dataStatus = DATA_NOT_AVAILABLE
                    false
                }
                else -> {
                    false
                }
            }
        }

        fun reset() {
            readByte = 0
            consumeByte = 0
        }
    }
}