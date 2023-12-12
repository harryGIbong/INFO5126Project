package com.skovalenko.info5126project.viewModel

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.skovalenko.info5126project.APIFormat
import com.skovalenko.info5126project.Article
import com.skovalenko.info5126project.RecyclerView.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.net.ssl.HttpsURLConnection

class MainViewModel : ViewModel() {
    val newsItems = MutableLiveData<List<NewsItem>>()
    val toastMessage = MutableLiveData<String>()
    private val db = FirebaseFirestore.getInstance()
    private var nextPageToken: String? = null
    private var currentTitle: String? = null

    private val apiKey = "pub_34594903b16fd42f65ff104149592b55c014e"

    fun fetchNewsData(title: String, nextPage: String? = null) {
        currentTitle = title
        nextPageToken = nextPage
        viewModelScope.launch(Dispatchers.IO) {
            val accumulatedArticles = ArrayList<Article>()
            var nextPage: String? = null

            try {
                val apiUrl = buildApiUrl(title, nextPage ?: nextPageToken)
                val response = fetchNewsDataFromApi(apiUrl)

                response?.results?.let { results ->
                    val newsItemsList = results.map { article ->
                        NewsItem(article.title, article.link)
                    }
                    newsItems.postValue(newsItemsList)
                }

                nextPageToken = response?.nextPage
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching news data", e)
                toastMessage.postValue("Error fetching news data")
            }
        }
    }
    fun fetchNextPage() {
        currentTitle?.let { title ->
            fetchNewsData(title, nextPageToken)
        }
    }
    private fun buildApiUrl(title: String, nextPage: String?): String {
        return "https://newsdata.io/api/1/news?apikey=$apiKey&qInTitle=$title&language=en&size=8" +
                (nextPage?.let { "&page=$it" } ?: "")
    }

    private suspend fun fetchNewsDataFromApi(apiUrl: String): APIFormat? {
        val url = URL(apiUrl)
        val connection = url.openConnection() as HttpsURLConnection
        if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
            connection.inputStream.bufferedReader().use { reader ->
                return Gson().fromJson(reader, APIFormat::class.java)
            }
        } else {
            Log.e("HTTP Error", "HTTP ${connection.responseCode}")
            return null
        }
    }


    fun saveTitleToDatabase(title: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val refinedTitle = title.substringAfter(") ")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("America/Toronto")
            }
            val date = dateFormat.format(Date())

            val titleWithDate = "[$date]  $refinedTitle"

            val userDocumentRef = db.collection("news_topics").document(currentUser.uid)

            val titleUpdate = hashMapOf<String, Any>(
                "news" to FieldValue.arrayUnion(titleWithDate)
            )

            db.runTransaction { transaction ->
                transaction.set(userDocumentRef, titleUpdate, SetOptions.merge())
            }.addOnSuccessListener {
                Log.d("Firebase", "News title appended successfully for user ${currentUser.uid}")
            }.addOnFailureListener { e ->
                Log.d("Firebase", "Error appending news title for user ${currentUser.uid}", e)
            }
        } else {
            toastMessage.value = "User must be logged in to save news title."
        }
    }

}