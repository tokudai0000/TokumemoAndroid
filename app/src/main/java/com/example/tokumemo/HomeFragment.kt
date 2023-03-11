package com.example.tokumemo

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.Settings.Global.putString
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import encrypt
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate
import decrypt
import java.time.LocalDateTime


class HomeFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var weatherText: TextView
    private lateinit var weatherIcon: ImageView
    private lateinit var webViewForLogin: WebView
    private lateinit var listView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view =  inflater.inflate(R.layout.fragment_home, container, false)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        weatherText = view.findViewById<TextView>(R.id.weatherText)
        weatherIcon = view.findViewById<ImageView>(R.id.weatherIcon)
        webViewForLogin = view.findViewById<WebView>(R.id.webView_for_login)
        listView = view.findViewById<RecyclerView>(R.id.menu_recycler_view)

        savePassword(view.context,"c611821006","KEY_cAccount")
        savePassword(view.context,"S7Nk9D9H2a","KEY_password")

        val contactUs = view.findViewById<Button>(R.id.studentCard)
        contactUs.setOnClickListener {
            val intent = Intent(requireContext(), WebActivity::class.java)
            intent.putExtra("PAGE_KEY", Url.ContactUs.urlString)
            startActivity(intent)
        }

        viewModel.getPRItemsFromGithub()

        listViewInitSetting(view)
        loginWebViewInitSetting(view)
        adImagesRotationTimerON(view)
        getWeatherNews(view)

        return view

    }


    private fun listViewInitSetting(view: View) {
        val displayMenuLists = viewModel.displayMenuList()
        // 最初はログイン完了していないので鍵マークを表示
        val adapter = MenuListsAdapter(displayMenuLists)
        listView.layoutManager = GridLayoutManager(context, 3, RecyclerView.VERTICAL, false)

        // 書籍情報セルのクリック処理
        adapter.setOnBookCellClickListener(object : MenuListsAdapter.OnBookCellClickListener {
            override fun onItemClick(book: MenuData) {
                when(book.id) {
                    MenuListItemType.CurrentTermPerformance -> {
                        // API24以下ではこれ API26以上ではLocalDate.now()が使用できる
                        val calendar = Calendar.getInstance()
                        var year = calendar.get(Calendar.YEAR)
                        val month = calendar.get(Calendar.MONTH) + 1 //月は0から始まるため、+1する

                        // 1月から3月までは前年度のURLを表示
                        if (month < 4){
                            year -= 1
                        }
                        val intent = Intent(requireContext(), WebActivity::class.java)
                        intent.putExtra("PAGE_KEY",book.url + year)
                        startActivity(intent)

                    }
                    MenuListItemType.Syllabus -> {
                        val intent = Intent(requireContext(), PasswordActivity::class.java)
                        intent.putExtra("hogemon", PasswordActivity.DisplayType.Syllabus)
                        startActivity(intent)
                    }
                    MenuListItemType.LibraryCalendar -> {
                        val calendar = Calendar.getInstance()
                        var year = calendar.get(Calendar.YEAR)

                        // ダイアログの表示
                        val alertDialog: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(requireContext(), R.style.FirstDialogStyle)
                        alertDialog.setTitle("図書館の所在を選択")
                        alertDialog.setMessage("こちらは最新情報ではありません。最新の情報は図書館ホームページをご覧ください。")
                        alertDialog.setNegativeButton("蔵本",
                            DialogInterface.OnClickListener { dialog, whichButton ->
                                val libraryURL = "https://docs.google.com/viewer?url=https://www.lib.tokushima-u.ac.jp/pub/pdf/calender/calender_kura_$year.pdf&embedded=true"
                                val intent = Intent(requireContext(), WebActivity::class.java)
                                intent.putExtra("PAGE_KEY",libraryURL)
                                startActivity(intent)
                            })
                        alertDialog.setPositiveButton("常三島",
                            DialogInterface.OnClickListener { dialog, whichButton ->
                                val libraryURL = "https://docs.google.com/viewer?url=https://www.lib.tokushima-u.ac.jp/pub/pdf/calender/calender_main_$year.pdf&embedded=true"
                                val intent = Intent(requireContext(), WebActivity::class.java)
                                intent.putExtra("PAGE_KEY",libraryURL)
                                startActivity(intent)
                            })
                        alertDialog.setOnCancelListener(DialogInterface.OnCancelListener {
                            // キャンセルの処理
                        })
                        alertDialog.show()
                    }
                    else -> {
                        val intent = Intent(requireContext(), WebActivity::class.java)
                        intent.putExtra("PAGE_KEY",book.url)
                        startActivity(intent)
                    }

                }
            }
        })
        listView.adapter = adapter
    }

    /// 広告を一定時間ごとに読み込ませる処理のスイッチ
    private fun adImagesRotationTimerON(view: View) {
        val imageView = view.findViewById<ImageView>(R.id.pr_image_button)

        // ImageViewにクリックリスナーを設定
        imageView.setOnClickListener {
            viewModel.displayPRImagesNumber?.let {
                viewModel.prItems[it].let {
                    val intent = Intent(context, PublicRelationsActivity::class.java)
                    intent.putExtra("PR_imageURL",it.imageURL)
                    intent.putExtra("PR_introduction",it.introduction)
                    intent.putExtra("PR_description",it.description)
                    intent.putExtra("PR_tappedURL",it.tappedURL)
                    intent.putExtra("PR_organization_name",it.organization_name)
                    startActivity(intent)
                }
            }
        }

        // 5000 ms 毎に実行
        Timer().scheduleAtFixedRate(0, 5000) {
            val num = viewModel.selectPRImageNumber()
            if (num != null) {
                viewModel.displayPRImagesNumber = num
                val imageTask: GetImage = GetImage(imageView)
                imageTask.execute(viewModel.prItems[num].imageURL)
            }
        }
    }

    // 天気を取得
    private fun getWeatherNews(view: View): Job = GlobalScope.launch {
        // 徳島大学本部の所在地の緯度経度
        var lat = 34.07003444012803
        var lon = 134.55981101249947
        // APIを使う際に必要なKEY
        val API_KEY = "e0578cd3fb0d436dd64d4d5d5a404f08"
        // URL。場所と言語・API_KEYを添付
        val urlStr = "https://api.openweathermap.org/data/2.5/weather?" +
                "units=metric&" +
                "lat=" + lat + "&" +
                "lon=" + lon + "&" +
                "lang=" + "ja" + "&" +
                "APPID=" + API_KEY

        try {
            val str = URL(urlStr).readText()
            val json = JSONObject(str)

            // 天気を取得
            val weatherList = json.getJSONArray("weather").getJSONObject(0)

            // 天気を取得
            val descriptionText = weatherList.getString("description")
            val icon = weatherList.getString("icon")
            val temp = json.getJSONObject("main").getString("temp")

            val imageTask: GetImage = GetImage(weatherIcon)
            imageTask.execute("https://openweathermap.org/img/wn/" + icon + ".png")
            weatherText.text = "$descriptionText\n$temp℃"
        } catch (e: Exception) {
//            weatherText.text = "現在天気を取得できません"
        }
    }


    private fun loginWebViewInitSetting(view: View) {
        // 自動ログイン用WebView
        val webView: WebView = view.findViewById(R.id.webView_for_login)
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(Url.UniversityTransitionLogin.urlString)
        DataManager.canExecuteJavascript = true
        DataManager.loginState.isProgress = true // ログイン処理の開始(iOSでは必要ないが、Androidでは必要)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {

                val urlString = guard(url) { throw return false }

                // ログインが完了しているかフラグ更新
                checkLoginComplete(urlString)

                // パスワードを登録しているか
                if (hasRegisteredPassword() == false) {
                    updateLoginFlag(flagType.NotStart)
                }

                // タイムアウトの判定
                if (isTimeout(urlString)) {
                    relogin()
                }

                // ログインに失敗していた場合
                if (isLoginFailure(urlString)) {
                    updateLoginFlag(flagType.LoginFailure)
                    if (view != null) {
                        Toast.makeText(view.context, "学生番号もしくはパスワードが間違っている為、ログインできませんでした", Toast.LENGTH_SHORT).show()
                    }
                }

                // ログイン完了時に鍵マークを外す(画像更新)為に、collectionViewのCellデータを更新
                if (DataManager.loginState.completeImmediately) {
                    if (view != null) {
                        if (listView.adapter != null) {
                            listView.adapter!!.notifyDataSetChanged()
                        }

                        Toast.makeText(
                            view.context,
                            "ログイン終了",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
//                    listView.adapter =
                }

                // ログイン中のアニメーションを消す
                if (DataManager.loginState.isProgress == false) {
//                    viewActivityIndicator.stopAnimating() // クルクルストップ
//                    loginGrayBackGroundView.isHidden = true
                }

                return false
            }


            override fun onPageFinished(view: WebView?, url: String?) {
                // アンラップ
                val urlString = guard(url) { throw return }

                // JavaScriptを動かしたいURLかどうかを判定し、必要なら動かす
                if (canExecuteJS(urlString)) {
                    val view = guard(view) {
                        throw return
                    }
                    val cAccount = getPassword(view.context, "KEY_cAccount")
                    val password = getPassword(view.context, "KEY_password")

                    // 徳島大学　統合認証システムサイト(ログインサイト)に自動ログインを行う。JavaScriptInjection
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

                    // フラグ管理
                    updateLoginFlag(flagType.ExecutedJavaScript)
                    return
                }
            }
        }
    }

    /// 大学統合認証システム(IAS)のページを読み込む
    /// ログインの処理はWebViewのdidFinishで行う
    private fun relogin() {
//        viewActivityIndicator.startAnimating() // クルクルスタート
//        loginGrayBackGroundView.isHidden = false
        updateLoginFlag(flagType.LoginStart)

        webViewForLogin.loadUrl(Url.UniversityTransitionLogin.urlString)
    }


    /*
     * 以下はViewModelに記述したいが、SharedPreferencesの扱いに慣れていない為、一時的にここに入れる
     */

    /// 大学統合認証システム(IAS)へのログインが完了したか判定
    private fun checkLoginComplete(urlStr: String) {
        // ログイン後のURL
        val check1 = urlStr.contains(Url.SkipReminder.urlString)
        val check2 = urlStr.contains(Url.CourseManagementPC.urlString)
        val check3 = urlStr.contains(Url.CourseManagementMobile.urlString)
        // 上記から1つでもtrueがあれば、result = true
        val result = (check1 || check2 || check3)

        // ログイン処理中かつ、ログイン後のURL
        if (DataManager.loginState.isProgress && result) {
            updateLoginFlag(flagType.LoginSuccess)
            return
        }
        return
    }

    enum class flagType {
        NotStart,
        LoginStart,
        LoginSuccess,
        LoginFailure,
        ExecutedJavaScript
    }
    // Dos攻撃を防ぐ為、1度ログインに失敗したら、JavaScriptを動かすフラグを下ろす
    private fun updateLoginFlag(type: flagType) {
        when (type){
            flagType.NotStart -> {
                DataManager.canExecuteJavascript = false
                DataManager.loginState.isProgress = false
                DataManager.loginState.completed = false
            }
            flagType.LoginStart -> {
                DataManager.canExecuteJavascript = true // ログイン用のJavaScriptを動かす
                DataManager.loginState.isProgress = true // ログイン処理中
                DataManager.loginState.completed = false // ログインが完了していない
            }
            flagType.LoginSuccess -> {
                DataManager.canExecuteJavascript = false
                DataManager.loginState.isProgress = false
                DataManager.loginState.completeImmediately = true
                DataManager.loginState.completed = true
//                lastLoginTime = Date() // 最終ログイン時刻の記録
            }
            flagType.LoginFailure -> {
                DataManager.canExecuteJavascript = false
                DataManager.loginState.isProgress = false
                DataManager.loginState.completed = false
            }
            flagType.ExecutedJavaScript -> {
                // Dos攻撃を防ぐ為、1度ログインに失敗したら、JavaScriptを動かすフラグを下ろす
                DataManager.canExecuteJavascript = false
                DataManager.loginState.isProgress = true
                DataManager.loginState.completed = false
            }
        }
    }

    // 学生番号、パスワードを登録しているか判定
    private fun hasRegisteredPassword(): Boolean {
        val view = guard(view) { throw return false}
        val cAccount = getPassword(view.context,"KEY_cAccount")
        val password = getPassword(view.context,"KEY_password")

        return !(cAccount == null || password == null)
    }

    /// 最新の利用規約同意者か判定し、同意画面の表示を行うべきか判定
//    private fun shouldShowTermsAgreementView(): Boolean {
//        return (DataManager.agreementVersion != ConstStruct.latestTermsVersion)
//    }

    /// タイムアウトのURLであるか判定
    private fun isTimeout(urlStr: String): Boolean {
        return urlStr == Url.UniversityServiceTimeOut.urlString || urlStr == Url.UniversityServiceTimeOut2.urlString
    }

    /// 大学統合認証システム(IAS)へのログインが失敗したか判定
    private fun isLoginFailure(urlStr: String): Boolean {
        // ログイン失敗した時のURL
        val check1 = urlStr.contains(Url.UniversityLogin.toString())
        val check2 = (urlStr.takeLast(2) != "s1")
        // 上記からどちらもtrueであれば、result = true
        val result = check1 && check2

        // ログイン処理中かつ、ログイン失敗した時のURL
        if (DataManager.loginState.isProgress && result) {
            DataManager.loginState.isProgress = false
            return true
        }
        return false
    }

    /// JavaScriptを動かしたい指定のURLか判定
    private fun canExecuteJS(urlString: String): Boolean {
        // JavaScriptを実行するフラグ
        if (DataManager.canExecuteJavascript == false) { return false }

        // cアカウント、パスワードの登録確認
        if (hasRegisteredPassword() == false) { return false }

        // 大学統合認証システム(IAS)のログイン画面の判定
        if (urlString.contains(Url.UniversityLogin.urlString)) { return true }

        // それ以外なら
        return false
    }

    // パスワードを暗号化して保存する
    private fun savePassword(context: Context, plainPassword: String, KEY: String) {
        // 暗号化
        val encryptedPassword = encrypt(context, "password", plainPassword)

        // shaaredPreferencesに保存
        var editor = context.getSharedPreferences("my_settings", Context.MODE_PRIVATE).edit().apply {
            putString(KEY, encryptedPassword).commit()
        }
    }

    // パスワードを復号化して取得する
    private fun getPassword(context: Context, KEY: String): String? {
        // shaaredPreferencesから読み込み
        val encryptedPassword = context.getSharedPreferences("my_settings", Context.MODE_PRIVATE).getString(KEY, null)
        // アンラップ
        encryptedPassword ?: return null
        // 復号化
        return decrypt("password", encryptedPassword)
    }
}