package com.example.tokumemo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import com.example.tokumemo.flag.MainModel

class OthersActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var viewModel: MainModel

    override fun onBackPressed() {
        // Android戻るボタン無効
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_others)

        val settingsScreen = findViewById<LinearLayout>(R.id.settings_screen)
        val title = findViewById<TextView>(R.id.settings_title)
        val bar = findViewById<ConstraintLayout>(R.id.backBar)
        val back = findViewById<Button>(R.id.backButton2)
        val aboutThisAppText = findViewById<ScrollView>(R.id.aboutThisAppText)

        // メニューバー
        val Home = findViewById<Button>(R.id.home)
        Home.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        val News = findViewById<Button>(R.id.news)
        News.setOnClickListener{
            val intent = Intent(this, NewsActivity::class.java)
            startActivity(intent)
            finish()
        }

        val Others = findViewById<Button>(R.id.others)
        Others.setOnClickListener{
            val intent = Intent(this, OthersActivity::class.java)
            startActivity(intent)
            finish()
        }

        // パスワード設定を押したとき
        val passwordSetting = findViewById<Button>(R.id.passwordSetting)
        passwordSetting.setOnClickListener{
            val intent = Intent(this, PasswordActivity::class.java)
            startActivity(intent)
        }

        // 公式SNSを押したとき
        val sns = findViewById<Button>(R.id.sns)
        sns.setOnClickListener{
            openWeb("https://twitter.com/tokumemo0000")
        }

        // ホームページを押したとき
        val homePage = findViewById<Button>(R.id.homePage)
        homePage.setOnClickListener{
            openWeb("https://lit.link/developers")
        }

        // 利用規約を押したとき
        val termsOfService = findViewById<Button>(R.id.termsOfService)
        termsOfService.setOnClickListener{
            openWeb("https://github.com/tokudai0000/document/blob/main/tokumemo/terms/TermsOfService.txt")
        }

        // プライバシーポリシーを押したとき
        val privacyPolicy = findViewById<Button>(R.id.privacyPolicy)
        privacyPolicy.setOnClickListener{
            openWeb("https://github.com/tokudai0000/document/blob/main/tokumemo/terms/PrivacyPolicy.txt")
        }

        // このアプリについてを押したとき
        val aboutThisApp = findViewById<Button>(R.id.aboutThisApp)
        aboutThisApp.setOnClickListener{
            settingsScreen.visibility = View.INVISIBLE
            title.visibility = View.INVISIBLE
            bar.visibility = View.VISIBLE
            aboutThisAppText.visibility = View.VISIBLE
        }

        // 戻るボタンを押したとき
        back.setOnClickListener{
            settingsScreen.visibility = View.VISIBLE
            title.visibility = View.VISIBLE
            // 「このアプリについて」から戻るときはwebviewは関係ないのでif文で処理
            if (aboutThisAppText.visibility == View.INVISIBLE){
                webView.visibility = View.GONE
            }

            aboutThisAppText.visibility = View.INVISIBLE
            bar.visibility = View.INVISIBLE
        }
    }

    private fun openWeb(url: String) {

        val settingsScreen = findViewById<LinearLayout>(R.id.settings_screen)
        val title = findViewById<TextView>(R.id.settings_title)
        val bar = findViewById<ConstraintLayout>(R.id.backBar)

        settingsScreen.visibility = View.INVISIBLE
        title.visibility = View.INVISIBLE
        bar.visibility = View.VISIBLE


        webView = findViewById(R.id.web)
        webView.settings.javaScriptEnabled = true
        viewModel = ViewModelProvider(this).get(MainModel::class.java)

        // 検索アプリで開かない
        webView.webViewClient = object : WebViewClient(){
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // webビューを表示モードに
                webView.visibility = View.VISIBLE
            }
        }
        // 読み込み時にページ横幅を画面幅に無理やり合わせる
        webView.getSettings().setLoadWithOverviewMode( true );
        // ワイドビューポートへの対応
        webView.getSettings().setUseWideViewPort( true );
        // 拡大縮小対応
        webView.getSettings().setBuiltInZoomControls(true);

        webView.loadUrl(url)
    }
}