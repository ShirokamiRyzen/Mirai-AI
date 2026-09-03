package com.ryzumi.miraiai.domain.util

import android.util.Base64
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.Buffer

class DataUrlFetcher(
    private val dataUrl: String,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val base64Data = dataUrl.substringAfter("base64,")
        val bytes = Base64.decode(base64Data, Base64.DEFAULT)
        val buffer = Buffer().write(bytes)
        val mime = dataUrl.substringAfter("data:").substringBefore(";")
        return SourceResult(
            source = ImageSource(buffer, options.context),
            mimeType = mime.ifBlank { "image/jpeg" },
            dataSource = DataSource.MEMORY
        )
    }

    class Factory : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.startsWith("data:image/")) {
                return DataUrlFetcher(data, options)
            }
            return null
        }
    }
}
