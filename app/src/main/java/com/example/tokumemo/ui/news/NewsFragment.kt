package com.example.tokumemo.ui.news

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.viewModels
import com.example.tokumemo.R
import com.example.tokumemo.ui.web.WebActivity

class NewsFragment : Fragment() {

    private val viewModel by viewModels<NewsViewModel>()

    private lateinit var listView: ListView
    private lateinit var adapter: NewsListViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        var view =  inflater.inflate(R.layout.fragment_news, container, false)
        listView = view.findViewById<ListView>(R.id.list_view)

        listView.setOnItemClickListener {_, _, position, _ ->
            val intent = Intent(requireContext(), WebActivity::class.java)
            val newsItem = viewModel.newsItems.value?.get(position)
            newsItem?.let {
                val intent = Intent(requireContext(), WebActivity::class.java)
                intent.putExtra("PAGE_KEY", it.link.toString())
                startActivity(intent)
            }
        }

        viewModel.newsItems.observe(viewLifecycleOwner) { newsList ->
            listView.adapter = NewsListViewAdapter(requireContext(), newsList)
        }

        viewModel.fetchNews()

        return view
    }
}

