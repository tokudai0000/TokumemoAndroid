package com.tokudai0000.tokumemo.ui.main.fragment.news

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.tokudai0000.tokumemo.R
import com.tokudai0000.tokumemo.domain.model.NewsListData

class NewsListViewAdapter(context: Context, private val items: ArrayList<NewsListData>) : ArrayAdapter<NewsListData>(context,
    R.layout.layout_news_list, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.layout_news_list, parent, false)

        val textView = view?.findViewById<TextView>(R.id.item_title)
        val pubDate = view?.findViewById<TextView>(R.id.item_text)

        val item = items[position]
        textView?.text = item.title
        pubDate?.text = item.pubDate

        return view!!
    }

}