package com.skovalenko.info5126project

data class APIFormat(
    val status: String,
    val totalResults: Int,
    val results: List<Article>,
    val nextPage: String?
)

data class Article(
    val article_id: String,
    val title: String,
    val link: String,
    val keywords: List<String>?, // nullable because it can be "null" in the JSON
    val creator: List<String>?, // nullable for the same reason
    val video_url: String?, // nullable
    val description: String,
    val content: String,
    val pubDate: String,
    val image_url: String,
    val source_id: String,
    val source_priority: Int,
    val country: List<String>,
    val category: List<String>,
    val language: String
)
