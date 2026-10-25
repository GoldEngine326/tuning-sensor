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

  override suspend fun fetch(): FetchResult? {
    return try {
      val bytes = ImageUtils.decodeBase64Bytes(dataUri)
      if (bytes == null || bytes.isEmpty()) return null

      val mimeType = if (dataUri.startsWith("data:")) {
        dataUri.substringAfter("data:").substringBefore(";").trim()
      } else {
        "image/jpeg"
      }

      val buffer = Buffer().write(bytes)
      SourceResult(
        source = ImageSource(source = buffer, context = options.context),
        mimeType = mimeType.ifBlank { "image/jpeg" },
        dataSource = DataSource.MEMORY
      )
    } catch (e: Exception) {
      android.util.Log.e("DataUriFetcher", "Failed to fetch data URI photo: ${e.message}")
      null
    }
  }

  class Factory : Fetcher.Factory<String> {
    override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
      if (ImageUtils.isLikelyBase64(data)) {
        return DataUriFetcher(data, options)
      }
      return null
    }
  }
}
