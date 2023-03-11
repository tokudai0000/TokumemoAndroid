package com.example.tokumemo

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

class ClubListsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_club_lists, container, false)
        val webView = view.findViewById<WebView>(R.id.club_lists_webview)
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(Url.ClubList.urlString)
        webView.setWebChromeClient(object : WebChromeClient() {
            override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                result.cancel()

                val intent = Intent(requireContext(), WebActivity::class.java)
                intent.putExtra("PAGE_KEY",message)
                startActivity(intent)

                return true
            }
        })

        return view
    }

}