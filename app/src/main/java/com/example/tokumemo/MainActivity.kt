package com.example.tokumemo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelFileDescriptor.open
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.bumptech.glide.Glide
import com.example.tokumemo.databinding.ActivityMainBinding
import com.example.tokumemo.flag.MainModel
import com.example.tokumemo.manager.DataManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.nio.channels.AsynchronousFileChannel.open
import java.nio.channels.AsynchronousSocketChannel.open
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var viewModel: MainModel
    private lateinit var weatherWebView: WebView
    private lateinit var weatherViewModel: MainModel
    private var urlString = ""

    lateinit var binding : ActivityMainBinding
    private var resultText = ""
    // 徳島大学本部の所在地の緯度経度
    private var placeLat = 34.07003444012803
    private var placeLon = 134.55981101249947

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 天気情報を表示
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 天気と時刻を取得
        getWeatherNews()

        weatherWebView = findViewById(R.id.weatherIcon)
        weatherWebView.settings.javaScriptEnabled = true
        weatherViewModel = ViewModelProvider(this).get(MainModel::class.java)
        weatherWebView.webViewClient = WebViewClient()
        // 読み込み時にページ横幅を画面幅に無理やり合わせる
        weatherWebView.getSettings().setLoadWithOverviewMode( true )
        // ワイドビューポートへの対応
        weatherWebView.getSettings().setUseWideViewPort( true )

        var iconUrl = "https://openweathermap.org/img/wn/" + encryptedLoad("icon") + ".png"

        weatherWebView.loadUrl(iconUrl)

        // 隠れWebビューここから（ここで先にログイン処理のみしておく）
        webView = findViewById(R.id.loginView)
        webView.settings.javaScriptEnabled = true
        viewModel = ViewModelProvider(this).get(MainModel::class.java)

        // 検索アプリで開かない
        webView.webViewClient = object : WebViewClient(){
            override fun onPageFinished(view: WebView?, url: String?) {
                if (url != null) {
                    urlString = url
                }

                when (viewModel.anyJavaScriptExecute(urlString)) {
                    MainModel.JavaScriptType.loginIAS -> {

                        if (shouldShowPasswordView()) {
                            // パスワード登録画面を表示
                            val intent = Intent(applicationContext, PasswordActivity::class.java)
                            startActivity(intent)
                        }
                        else if (DataManager.canExecuteJavascript) {
                            val cAccount = encryptedLoad("KEY_cAccount")
                            val password = encryptedLoad("KEY_password")

                            webView.evaluateJavascript(
                                "document.getElementById('username').value= '$cAccount'",
                                null
                            )
                            webView.evaluateJavascript(
                                "document.getElementById('password').value= '$password'",
                                null
                            )
                            webView.evaluateJavascript(
                                "document.getElementsByClassName('form-element form-button')[0].click();",
                                null
                            )
                        }
                    }
                    else -> {}
                }

                super.onPageFinished(view, url)

                // ついでに天気情報を更新しておく
                binding.weatherText.text = resultText
            }
        }
        webView.loadUrl("https://eweb.stud.tokushima-u.ac.jp/Portal/")
        // 隠れWebビューここまで

        // メニューバー
        val Home = findViewById<Button>(R.id.home)
        Home.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            DataManager.canExecuteJavascript = true
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

        // キャリアセンターを押したとき
        val Button0 = findViewById<Button>(R.id.carrierCenter)
        Button0.setOnClickListener{
            goWeb("0")
        }
        // マナバを押したとき
        val Button1 = findViewById<Button>(R.id.manaba)
        Button1.setOnClickListener{
            goWeb("1")
        }
        // 教務システムを押したとき
        val Button2 = findViewById<Button>(R.id.academicAffairsSystem)
        Button2.setOnClickListener{
            goWeb("2")
        }
        // メールを押したとき
        val Button3 = findViewById<Button>(R.id.email)
        Button3.setOnClickListener{
            goWeb("3")
        }
        // 図書館を押したとき
        val Button4 = findViewById<Button>(R.id.library)
        Button4.setOnClickListener{
            goWeb("4")
        }
        // 生協を押したとき
        val Button5 = findViewById<Button>(R.id.seikyou)
        Button5.setOnClickListener{
            goWeb("5")
        }
        // 成績を押したとき
        val Button6 = findViewById<Button>(R.id.result)
        Button6.setOnClickListener{
            goWeb("6")
        }
        // 時間割を押したとき
        val Button7 = findViewById<Button>(R.id.timetable)
        Button7.setOnClickListener{
            goWeb("7")
        }
        // シラバスを押したとき
        val Button8 = findViewById<Button>(R.id.syllabus)
        Button8.setOnClickListener{
            goWeb("8")
        }
    }


    // パスワードを登録しているか判定し、パスワード画面の表示を行うべきか判定
    private fun shouldShowPasswordView():Boolean {
        val cAccount = encryptedLoad("KEY_cAccount")
        val password = encryptedLoad("KEY_password")
        return (cAccount == "" || password == "")
    }

    // 以下、暗号化してデバイスに保存する(PasswordActivityにも存在するので今後、統一)
    companion object {
        const val PREF_NAME = "encrypted_prefs"
    }
    // 読み込み
    private fun encryptedLoad(KEY: String): String {
        val mainKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            applicationContext,
            MainActivity.PREF_NAME,
            mainKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        return prefs.getString(KEY, "")!! // nilの場合は空白を返す
    }
    // 保存
    private fun encryptedSave(KEY: String, text: String) {
        val mainKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            applicationContext,
            MainActivity.PREF_NAME,
            mainKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        with(prefs.edit()) {
            putString(KEY, text)
            apply()
        }
    }

    // 押されたWebサイトにとぶ
    private fun goWeb(pageId: String) {
        val intent = Intent(this, WebActivity::class.java)
        // WebActivityにどのWebサイトを開こうとしているかをIdとして送信して知らせる
        intent.putExtra("PAGE_KEY",pageId)
        // 自動入力のフラグを上げる
        DataManager.canExecuteJavascript = true
        startActivity(intent)
    }

    // unixtimeからフォーマットの日付に変換
    @RequiresApi(Build.VERSION_CODES.N)
    private fun unixTimeChange(unixTime: String): String {
        var sdf = SimpleDateFormat("yyyy/MM/dd HH")
        var nowTime = Date(unixTime.toInt() * 1000L)
        return sdf.format(nowTime)
    }

    // 天気を取得
    @RequiresApi(Build.VERSION_CODES.N)
    private fun getWeatherNews(): Job = GlobalScope.launch {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH")
        val currentTime = sdf.format(Date())

        if (currentTime != encryptedLoad("dateTime")) {
            val sdfForText = SimpleDateFormat("yyyy/MM/dd現在")
            val currentTimeForText = sdfForText.format(Date())
            // 結果を初期化
            resultText = ""
            // APIを使う際に必要なKEY
            var API_KEY = "e0578cd3fb0d436dd64d4d5d5a404f08"
            // URL。場所と言語・API_KEYを添付
            var API_URL = "https://api.openweathermap.org/data/2.5/weather?" +
                    "units=metric&" +
                    "lat=" + placeLat + "&" +
                    "lon=" + placeLon + "&" +
                    "lang=" + "ja" + "&" +
                    "APPID=" + API_KEY
            var url = URL(API_URL)
            //APIから情報を取得する.
            var br = BufferedReader(InputStreamReader(url.openStream()))
            // 所得した情報を文字列化
            var str = br.readText()
            //json形式のデータとして識別
            var json = JSONObject(str)
            // hourlyの配列を取得
//        var hourly = json.getJSONArray("hourly")

            // 天気予報を取得
//        var firstObject = hourly.getJSONObject(0)
            var weatherList = json.getJSONArray("weather").getJSONObject(0)
            // unixtime形式で保持されている時刻を取得
            var time = json.getString("dt")
            // 天気を取得
            var descriptionText = weatherList.getString("description")
            var icon = weatherList.getString("icon")
            var temp = json.getJSONObject("main").getString("temp")
            encryptedSave("dateTime", unixTimeChange(time))
            encryptedSave("dateTimeForText", currentTimeForText)
            encryptedSave("descriptionText", descriptionText)
            encryptedSave("icon", icon)
            encryptedSave("temp", temp)
            Log.i("weatherList: ", weatherList.toString())
            resultText += "$currentTimeForText\n徳島市\n$descriptionText $temp℃"
        } else {
            resultText += "${encryptedLoad("dateTimeForText")}\n徳島市\n${encryptedLoad("descriptionText")} ${encryptedLoad("temp")}℃"
        }

    }
}