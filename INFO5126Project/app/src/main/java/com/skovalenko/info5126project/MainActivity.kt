package com.skovalenko.info5126project

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.Response
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.gson.Gson
import com.skovalenko.info5126project.RecyclerView.NewsAdapter
import com.skovalenko.info5126project.RecyclerView.NewsItem
import com.skovalenko.info5126project.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var newsAdapter: NewsAdapter
    private var newsItems: List<NewsItem> = listOf()
    val db = Firebase.firestore
    val newsKey:String = "news"
    val todoKey:String = "TODO"
    companion object {
        val titles: ArrayList<String> = ArrayList()
    }

    lateinit var todoList: MutableList<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //binding.textViewUser.text = intent.getStringExtra("userid")
        binding.textViewEmail.text = intent.getStringExtra("email")

        newsItems = ArrayList()

        // Initialize the adapter with the (currently empty) list of news items
        newsAdapter = NewsAdapter(newsItems as ArrayList<NewsItem>)

        // Setup RecyclerView
        binding.newsRecyclerView.apply {
            adapter = newsAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)

            // Add the divider
            val dividerItemDecoration = DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL)
            addItemDecoration(dividerItemDecoration)
        }

        newsAdapter.itemClickListener = object : NewsAdapter.OnNewsItemClickListener {
            override fun onNewsTitleClicked(title: String) {
                Log.d("MainActivity", "News title clicked: $title")
                saveTitleToDatabase(title)
            }

            override fun onNewsItemExpanded(title: String) {
                saveTitleToDatabase(title)
            }
        }

        binding.buttonLogout.setOnClickListener {
            // logs out of Firebase
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this,"You are logged out", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this,LoginActivity::class.java))
            finish()
        }

        binding.buttonAbout.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }

        binding.buttonHistory.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        binding.buttonRead.setOnClickListener {
            if (binding.editTextAdd.text.toString() != "")
            {
                CoroutineScope(Dispatchers.Main).launch {
                    val request: APIFormat? = getNewsDataFromCoroutine(binding.editTextAdd.text.toString())
                }
            }
        }
    }

    fun saveTitleToDatabase(title: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val refinedTitle = title.substringAfter(") ")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = dateFormat.format(Date())

            val titleWithDate = "[$date]  $refinedTitle"

            val userDocumentRef = db.collection("news_topics").document(currentUser.uid)

            // Append the refined news title to the 'news' array field in the document
            val titleUpdate = hashMapOf<String, Any>(
                "news" to FieldValue.arrayUnion(titleWithDate)
            )

            // Run a transaction to ensure that the news array is updated atomically
            db.runTransaction { transaction ->
                transaction.set(userDocumentRef, titleUpdate, SetOptions.merge())
            }.addOnSuccessListener {
                Log.d("Firebase", "News title appended successfully for user ${currentUser.uid}")
            }.addOnFailureListener { e ->
                Log.d("Firebase", "Error appending news title for user ${currentUser.uid}", e)
            }
        } else {
            Toast.makeText(this, "User must be logged in to save news title.", Toast.LENGTH_SHORT).show()
        }
    }



    private suspend fun getNewsDataFromCoroutine(title: String): APIFormat? {
        val accumulatedArticles = ArrayList<Article>()
        var nextPage: String? = null

        do {
            val apiUrl = if (nextPage == null) {
                "https://newsdata.io/api/1/news?apikey=pub_34588f443dae3ba06f40aa83a746bb57100b7&q=$title&language=en&size=10"
            } else {
                "https://newsdata.io/api/1/news?apikey=pub_34588f443dae3ba06f40aa83a746bb57100b7&q=$title&language=en&size=10&page=$nextPage"
            }

            val response = fetchNewsData(apiUrl)
            response?.results?.forEach { result ->
                if (result.title.contains(title, ignoreCase = true)) {
                    accumulatedArticles.add(result) // Add the entire Article object
                }
            }
            nextPage = response?.nextPage

        } while (nextPage != null)

        updateUI(accumulatedArticles, title)
        return null
    }


    private suspend fun fetchNewsData(apiUrl: String): APIFormat? {
        return withContext(Dispatchers.IO) {
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpsURLConnection
            if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { reader ->
                    Gson().fromJson(reader, APIFormat::class.java)
                }
            } else {
                Log.e("HTTP Error", "HTTP ${connection.responseCode}")
                null
            }
        }
    }

    fun updateUI(accumulatedArticles: List<Article>, titleToCheck: String) {
        runOnUiThread {
            // Convert the accumulatedArticles to a list of NewsItem objects
            val updatedNewsItems = accumulatedArticles.mapIndexed { index, article ->
                NewsItem("${index + 1}) ${article.title}", article.link, isExpanded = false)
            }

            newsAdapter.updateItems(updatedNewsItems)
        }
    }
}