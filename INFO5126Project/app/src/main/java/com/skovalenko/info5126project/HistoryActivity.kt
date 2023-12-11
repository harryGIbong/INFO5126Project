package com.skovalenko.info5126project

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.skovalenko.info5126project.RecyclerView.NewsAdapter
import com.skovalenko.info5126project.RecyclerView.NewsItem

class HistoryActivity : AppCompatActivity() {
    private lateinit var newsAdapter: NewsAdapter
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Initialize the adapter with an empty list of news items
        newsAdapter = NewsAdapter(arrayListOf())

        val recyclerViewHistory: RecyclerView = findViewById(R.id.recyclerViewHistory)
        recyclerViewHistory.adapter = newsAdapter
        recyclerViewHistory.layoutManager = LinearLayoutManager(this)

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // Fetch titles from Firestore
            db.collection("news_topics").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val newsList = document["news"] as? List<String> ?: listOf()
                        val newsItems = newsList.map { title -> NewsItem(title, "") }
                        newsAdapter.updateItems(ArrayList(newsItems))
                    } else {
                        Log.d("HistoryActivity", "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w("HistoryActivity", "Error getting documents: ", exception)
                }
        }
    }

    fun backToMainPage(view: View) {
        finish()
    }
}

