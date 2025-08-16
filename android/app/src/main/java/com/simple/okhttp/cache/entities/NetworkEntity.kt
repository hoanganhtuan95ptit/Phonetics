package com.simple.okhttp.cache.entities

data class NetworkEntity(
    val id: String,

    val code: Int,

    val body: String,
    val message: String,
    val createdTime: Long
)