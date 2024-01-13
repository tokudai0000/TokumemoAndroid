package com.example.tokumemo.ui.agreement

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tokumemo.data.DataManager
import com.example.tokumemo.R
import com.example.tokumemo.common.AKLog
import com.example.tokumemo.common.AKLogLevel
import com.example.tokumemo.common.Url
import com.example.tokumemo.domain.model.AdItem
import com.example.tokumemo.ui.RootActivity
import com.example.tokumemo.ui.splash.SplashActivity
import com.example.tokumemo.ui.web.WebActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class AgreementActivity : AppCompatActivity() {

    companion object {

        const val EXTRA_RESULT = "result"

        fun createIntent(context: Context) =
            Intent(context, AgreementActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agreement)

        val termText: TextView = findViewById(R.id.agreement_text_view)

        GlobalScope.launch {
            try {
                termText.text = getTermText()
            } catch (e: Exception) {
                println("Error occurred: ${e.message}")
            }
        }

        findViewById<Button>(R.id.terms_button).setOnClickListener {
            val intent = Intent(this, WebActivity::class.java)
            intent.putExtra("PAGE_KEY", Url.TermsOfService.urlString)
            startActivity(intent)
        }
        findViewById<Button>(R.id.privacy_button).setOnClickListener {
            val intent = Intent(this, WebActivity::class.java)
            intent.putExtra("PAGE_KEY", Url.PrivacyPolicy.urlString)
            startActivity(intent)
        }
        findViewById<Button>(R.id.agreement_button).setOnClickListener {
            val KEY = "KEY_agreementVersion"

            AKLog(AKLogLevel.DEBUG, DataManager.agreementVer)
            getSharedPreferences("my_settings", Context.MODE_PRIVATE).edit().apply {
                putString(KEY, DataManager.agreementVer).commit()
            }

            val returnIntent = Intent()
            returnIntent.putExtra(RootActivity.EXTRA_NEXT_ACTIVITY, "MainActivity")
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }
    }

    private suspend fun getTermText(): String {
        val apiUrl = "https://tokudai0000.github.io/tokumemo_resource/api/v1/term_text.json"

        return withContext(Dispatchers.IO) {
            try {
                val responseData = URL(apiUrl).readText()
                val jsonData = JSONObject(responseData)

                // Jsonデータから内容物を取得
                jsonData.getString("termText")
            } catch (e: Exception) {
                // エラー処理、適宜エラーメッセージを返す
                "Error: ${e.message}"
            }
        }
    }
}