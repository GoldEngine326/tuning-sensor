package com.example.util

import android.util.Base64
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.Buffer

/**
 * Custom Coil Fetcher that natively supports "data:image/..." Data URIs with Base64 encoding.
 * Enables SubcomposeAsyncImage and AsyncImage to load Base64 photos directly.
 */
class DataUriFetcher(
  private val dataUri: String,
  private val options: Options
) : Fetcher {

  override suspend fun fetch(): FetchResult {
    val base64Data = if (dataUri.contains("base64,")) {
      dataUri.substringAfter("base64,")
    } else {
      dataUri.substringAfter(",")
    }
    val bytes = Base64.decode(base64Data, Base64.DEFAULT)
    val mimeType = if (dataUri.startsWith("data:")) {
      dataUri.substringAfter("data:").substringBefore(";").trim()
    } else {
      "image/jpeg"
    }

    val buffer = Buffer().write(bytes)
    return SourceResult(
      source = ImageSource(source = buffer, context = options.context),
      mimeType = mimeType.ifBlank { "image/jpeg" },
      dataSource = DataSource.MEMORY
    )
  }

  class Factory : Fetcher.Factory<String> {
    override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
      if (data.startsWith("data:image", ignoreCase = true)) {
        return DataUriFetcher(data, options)
      }
      return null
    }
  }
}
