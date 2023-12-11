package com.skovalenko.info5126project.RecyclerView

import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.skovalenko.info5126project.MainActivity
import com.skovalenko.info5126project.R

class NewsAdapter(private var items: ArrayList<NewsItem>) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    inner class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)

        init {
            titleTextView.setOnClickListener {
                val item = items[adapterPosition]
                item.isExpanded = !item.isExpanded
                notifyItemChanged(adapterPosition)
                if (item.isExpanded) {
                    itemClickListener?.onNewsItemExpanded(item.title)
                }
            }
        }
    }

    interface OnNewsItemClickListener {
        fun onNewsTitleClicked(title: String)
        fun onNewsItemExpanded(title: String)
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.newsitem_layout, parent, false)
        return NewsViewHolder(view)
    }

    class CustomURLSpan(private val title: String, private val onClick: (String) -> Unit) : URLSpan(title) {
        override fun onClick(widget: View) {
            Log.d("NewsAdapter", "CustomURLSpan clicked for title: $title")
            onClick(title)
        }
    }


    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val item = items[position]
        holder.titleTextView.text = item.title

        if (item.isExpanded) {
            val urlSpannable = SpannableString(item.url)
            val customURLSpan = CustomURLSpan(item.title) { title ->
                itemClickListener?.onNewsTitleClicked(title)
            }
            urlSpannable.setSpan(customURLSpan, 0, item.url.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            holder.contentTextView.apply {
                text = urlSpannable
                movementMethod = LinkMovementMethod.getInstance()
                visibility = View.VISIBLE
            }
        } else {
            holder.contentTextView.visibility = View.GONE
        }
    }

    // Listener for item clicks
    var itemClickListener: OnNewsItemClickListener? = null


    fun updateItems(newItems: List<NewsItem>) {
        items = ArrayList(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size
}
