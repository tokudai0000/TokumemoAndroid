package com.example.tokumemo

import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.SimpleAdapter
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.*
import org.json.JSONArray
import org.json.JSONObject

class News : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_news, container, false)
        var titleArray = arrayListOf<Data>()

        // xmlにて実装したListViewの取得
        val listView = view.findViewById<ListView>(R.id.list_view)

        /// リクエストURL
        val url = "https://api.rss2json.com/v1/api.json?rss_url=https://www.tokushima-u.ac.jp/recent/rss.xml"

        url.httpGet().responseJson { request, response, result ->
            when (result) {
                is Result.Success -> {

                    val items:JSONArray = result.get().obj().getJSONArray("items")

                    for(i in 0..items.length() - 1 )
                        titleArray.add(Data().apply {
                            title = items.getJSONObject(i)["title"].toString()
                        })

                }
                is Result.Failure -> {
                    val ex = result.getException()

                    JSONObject(mapOf("message" to ex.toString()))
                }
            }
            listView.adapter = CustomAdapter(requireContext(), titleArray)
        }
        return view
    }

}

