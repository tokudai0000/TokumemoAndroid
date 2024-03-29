package com.tokudai0000.tokumemo.ui.agreement

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.tokudai0000.tokumemo.R
import com.tokudai0000.tokumemo.common.AKLog
import com.tokudai0000.tokumemo.common.AKLogLevel
import com.tokudai0000.tokumemo.common.Url
import com.tokudai0000.tokumemo.data.repository.AcceptedTermVersionRepository
import com.tokudai0000.tokumemo.ui.RootActivity
import com.tokudai0000.tokumemo.ui.web.WebActivity

class AgreementActivity : AppCompatActivity(R.layout.activity_agreement) {

    private val viewModel by viewModels<AgreementViewModel>()

    companion object {
        var currentTermVersion: String = ""

        fun createIntent(context: Context) = Intent(context, AgreementActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureTermText()

        // 利用規約のボタン
        findViewById<Button>(R.id.terms_button).setOnClickListener {
            val intent = Intent(this, WebActivity::class.java)
            intent.putExtra(WebActivity.KEY_URL, Url.TermsOfService.urlString)
            startActivity(intent)
        }

        // プライバシーポリシーのボタン
        findViewById<Button>(R.id.privacy_button).setOnClickListener {
            val intent = Intent(this, WebActivity::class.java)
            intent.putExtra(WebActivity.KEY_URL, Url.PrivacyPolicy.urlString)
            startActivity(intent)
        }

        // 同意するのボタン
        findViewById<Button>(R.id.agreement_button).setOnClickListener {
            AKLog(AKLogLevel.DEBUG, "同意したcurrentTermVersion: $currentTermVersion")

            // 同意した規約のバージョンを保存
            AcceptedTermVersionRepository(this).setAcceptedTermVersion(currentTermVersion)

            // 現状は、RootActivityにAgreementActivityが乗っている状態
            // Agreementを終了させ、RootからMainを呼び出す
            val returnIntent = Intent()
            returnIntent.putExtra(RootActivity.EXTRA_NEXT_ACTIVITY, "MainActivity")
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }
    }

    private fun configureTermText() {
        val termTextView: TextView = findViewById(R.id.agreement_text_view)

        viewModel.termText.observe(this) { item ->
            termTextView.text = item
        }

        viewModel.fetchTermText()
    }
}
