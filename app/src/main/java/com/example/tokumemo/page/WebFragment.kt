package com.example.tokumemo.page

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.tokumemo.PasswordActivity
import com.example.tokumemo.R
import com.example.tokumemo.WebActivity
import com.example.tokumemo.flag.MainModel
import com.example.tokumemo.manager.DataManager

class WebFragment: Fragment(R.layout.web_fragment) {

    private lateinit var webViewClient: WebViewClient
    private lateinit var viewModel: MainModel
    private var urlString = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webViewSetup()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        childFragmentManager.setFragmentResultListener(
            "request_key",
            viewLifecycleOwner
        ) { requestKey: String, result: Bundle ->
            val text = result.getString("key_text", "")

        }
    }

    // MainActivityからデータを受け取ったデータを基にURLを読み込んでサイトを開く
    private fun webViewLoadUrl() {
        var pageId = 99
        // MainActivityからデータを受け取る
        // どのWebサイトを開こうとしているかをIdで判別
        setFragmentResultListener("requestKey") { key, bundle ->
            pageId = bundle.getString("bundleKey")?.toInt()!!

        }
        webView.loadUrl(viewModel.isAnyWebsite(pageId))
    }

    // WebViewの設定
    private fun webViewSetup() {
        webView = view?.findViewById<WebView>(R.id.webView) ?:
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true

        viewModel = ViewModelProvider(this).get(MainModel::class.java)

        // 検索アプリで開かない
        webView.webViewClient = object : WebViewClient(){
            // URLの読み込みが始まった時の処理
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                if (url != null) {
                    urlString = url
                }
                // タイムアウトをしていた場合
                if (viewModel.isTimeout(urlString)) {
                    // ログイン処理を始める
                    DataManager.canExecuteJavascript = true
                    webView.loadUrl("https://eweb.stud.tokushima-u.ac.jp/Portal/")
                }
            }

            // URLの読み込みが終わった時の処理
            override fun onPageFinished(view: WebView?, url: String?) {
                if (url != null) {
                    urlString = url
                }

                when (viewModel.anyJavaScriptExecute(urlString)) {
                    // ログイン画面に飛ばされた場合
                    MainModel.JavaScriptType.loginIAS -> {

                        if (shouldShowPasswordView()) {
                            // パスワード登録画面を表示
                            val intent = Intent(applicationContext, PasswordActivity::class.java)
                            startActivity(intent)
                            // 戻ってきた時、startForPasswordActivityを呼び出す
//                          startForPasswordActivity.launch(intent)
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
                            // フラグを下ろす
                            DataManager.canExecuteJavascript = false
                        }
                        // 再度URLを読み込む
                        webViewLoadUrl()
                    }
                    else -> {}
                }
                super.onPageFinished(view, url)
            }
        }

        // 読み込み時にページ横幅を画面幅に無理やり合わせる
        webView.getSettings().setLoadWithOverviewMode( true );
        // ワイドビューポートへの対応
        webView.getSettings().setUseWideViewPort( true );
        // 拡大縮小対応
        webView.getSettings().setBuiltInZoomControls(true);

        webViewLoadUrl()
    }

    // PasswordActivityで登録ボタンを押した場合、再度ログイン処理を行う
//    private val startForPasswordActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
//        if (result.resultCode == Activity.RESULT_OK) {
//            // ログイン処理を行う
//            webView.loadUrl("https://my.ait.tokushima-u.ac.jp/portal/")
//        }
//    }

    // ハスワードを登録しているか判定し、パスワード画面の表示を行うべきか判定
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
            WebActivity.PREF_NAME,
            mainKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        return prefs.getString(KEY, "")!! // nilの場合は空白を返す
    }
}