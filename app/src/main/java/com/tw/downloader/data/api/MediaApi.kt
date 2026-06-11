package com.tw.downloader.data.api

import com.tw.downloader.data.model.MediaResponse
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface MediaApi {
    @GET("api/media")
    suspend fun getMedia(@QueryMap params: Map<String, String>): MediaResponse
}
