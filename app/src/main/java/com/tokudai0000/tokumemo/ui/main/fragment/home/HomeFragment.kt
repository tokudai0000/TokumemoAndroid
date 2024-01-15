package com.tokudai0000.tokumemo.ui.main.fragment.home

import java.util.*
import kotlin.concurrent.scheduleAtFixedRate
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tokudai0000.tokumemo.R
import com.tokudai0000.tokumemo.common.AKLog
import com.tokudai0000.tokumemo.common.AKLogLevel
import com.tokudai0000.tokumemo.common.AppConstants
import com.tokudai0000.tokumemo.data.DataManager
import com.tokudai0000.tokumemo.domain.model.MenuDetailItem
import com.tokudai0000.tokumemo.domain.model.MenuItem
import com.tokudai0000.tokumemo.ui.pr.PublicRelationsActivity
import com.tokudai0000.tokumemo.ui.web.WebActivity
import com.tokudai0000.tokumemo.utility.GetImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL


class HomeFragment : Fragment() {

    private lateinit var view: View
    private lateinit var menuRecyclerView: RecyclerView

    private val viewModel by viewModels<HomeViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        view =  inflater.inflate(R.layout.fragment_home, container, false)
        
        configureAdImages()
        configureMenuRecyclerView()
        configureHomeMiniSettingsListView()

        // PR画像(広告)の取得
        viewModel.getAdItems()

        val termText: TextView = view.findViewById(R.id.number_of_users_text_view)
        GlobalScope.launch {
            try {
                termText.text = getNumberOfUsers()
            } catch (e: Exception) {
                AKLog(AKLogLevel.ERROR, "Error: ${e.message}")
            }
        }

        return view
    }

    private fun configureAdImages() {
        val prImageView = view.findViewById<ImageView>(R.id.pr_image_button)
        prImageView.setOnClickListener {

            // displayPrItemのnullチェック
            viewModel.displayPrItem?.let { item ->
                val intent = Intent(requireContext(), PublicRelationsActivity::class.java)
                intent.putExtra("PAGE_KEY",item)
                startActivity(intent)
            }
        }

        val univImageView = view.findViewById<ImageView>(R.id.univ_image_button)
        univImageView.setOnClickListener {
            viewModel.displayUnivItem?.let { item ->
                val intent = Intent(requireContext(), WebActivity::class.java)
                intent.putExtra("PAGE_KEY",item.targetUrlStr)
                startActivity(intent)
            }
        }

        // 広告を5000 msごとに読み込ませる
        Timer().scheduleAtFixedRate(0, 5000) {
            viewModel.randomChoiceForAdImage(adItems = viewModel.prItems, displayAdItem = viewModel.displayPrItem)?.let {
                viewModel.displayPrItem = it
                GetImage(prImageView).execute(it.imageUrlStr)
            }
            viewModel.randomChoiceForAdImage(adItems = viewModel.univItems, displayAdItem = viewModel.displayUnivItem)?.let {
                viewModel.displayPrItem = it
                GetImage(univImageView).execute(it.imageUrlStr)
            }
        }
    }


    /// RecyclerViewの初期設定
    private fun configureMenuRecyclerView() {
        menuRecyclerView = view.findViewById<RecyclerView>(R.id.menu_recycler_view)
        val displayMenuLists = AppConstants.menuItems
        val adapter = HomeMenuRecyclerAdapter(displayMenuLists)
        // 横3列に指定する
        menuRecyclerView.layoutManager = NoScrollGridLayoutManager(requireContext(), 3)

        adapter.setOnBookCellClickListener(object :
            HomeMenuRecyclerAdapter.OnBookCellClickListener {
            override fun onItemClick(item: MenuItem) {

                DataManager.canExecuteJavascript = true
                when(item.id) {

                    // 講義関連
                    MenuItem.Type.AcademicRelated -> {
                        showMenuDetailDialog(AppConstants.academicRelatedItems)
                    }

                    // 図書館関連
                    MenuItem.Type.LibraryRelated -> {
                        showMenuDetailDialog(AppConstants.libraryRelatedItems)
                    }

                    // その他
                    MenuItem.Type.Etc -> {
                        showMenuDetailDialog(AppConstants.etcItems)
                    }

                    else -> {
                        val intent = Intent(requireContext(), WebActivity::class.java)
                        intent.putExtra("PAGE_KEY",item.url)
                        startActivity(intent)
                    }
                }
            }
        })
        menuRecyclerView.adapter = adapter
    }

    private fun configureHomeMiniSettingsListView() {
        val homeMiniSettingsRecyclerView = view.findViewById<ListView>(R.id.home_mini_settings_recycler_view)
        val displayMenuLists = AppConstants.homeMiniSettingsItems
        homeMiniSettingsRecyclerView.adapter = HomeMiniSettingsRecyclerAdapter(requireContext(), displayMenuLists)
        homeMiniSettingsRecyclerView.setOnItemClickListener {_, _, position, _ ->
            val intent = Intent(requireContext(), WebActivity::class.java)
            intent.putExtra("PAGE_KEY", displayMenuLists[position].targetUrl.toString())
            startActivity(intent)
        }
    }

    private fun showMenuDetailDialog(items: List<MenuDetailItem>) {
        val builder = AlertDialog.Builder(requireContext())

        // MenuDetailItem の配列から表示用の文字列の配列を生成
        val itemNames = items.map { it.title }.toTypedArray()

        builder.setItems(itemNames) { dialog, which ->
            val intent = Intent(requireContext(), WebActivity::class.java)
            intent.putExtra("PAGE_KEY",items[which].targetUrl)
            startActivity(intent)
        }

        val dialog = builder.create()
        dialog.show()
    }

    private suspend fun getNumberOfUsers(): String {
        val apiUrl = "https://tokudai0000.github.io/tokumemo_resource/api/v1/number_of_users.json"

        return withContext(Dispatchers.IO) {
            try {
                val responseData = URL(apiUrl).readText()
                val jsonData = JSONObject(responseData)

                // Jsonデータから内容物を取得
                jsonData.getString("numberOfUsers")
            } catch (e: Exception) {
                // エラー処理、適宜エラーメッセージを返す
                "Error: ${e.message}"
            }
        }
    }

    // 学生番号、パスワードを登録しているか判定
//    private fun hasRegisteredPassword(): Boolean {
//        val view = guard(view) { throw return false}
//        val cAccount = getPassword(view.context,"KEY_cAccount")
//        val password = getPassword(view.context,"KEY_password")
//
//        return !(cAccount == null || password == null)
//    }
    //                    // 図書館カレンダー
//                    MenuListItemType.LibraryCalendar -> {
//                        val calendar = Calendar.getInstance()
//                        val year = calendar.get(Calendar.YEAR)
//
//                        // ダイアログの表示(常三島と蔵本を)
//                        val alertDialog: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(requireContext(),
//                            R.style.FirstDialogStyle
//                        )
//                        alertDialog.setTitle("図書館の所在を選択")
//                        alertDialog.setMessage("こちらは最新情報ではありません。最新の情報は図書館ホームページをご覧ください。")
//                        alertDialog.setNegativeButton("蔵本",
//                            DialogInterface.OnClickListener { _, _ ->
//                                val libraryURL = "https://docs.google.com/viewer?url=https://www.lib.tokushima-u.ac.jp/pub/pdf/calender/calender_kura_$year.pdf&embedded=true"
//                                val intent = Intent(requireContext(), WebActivity::class.java)
//                                intent.putExtra("PAGE_KEY",libraryURL)
//                                startActivity(intent)
//                            })
//                        alertDialog.setPositiveButton("常三島",
//                            DialogInterface.OnClickListener { _, _ ->
//                                val libraryURL = "https://docs.google.com/viewer?url=https://www.lib.tokushima-u.ac.jp/pub/pdf/calender/calender_main_$year.pdf&embedded=true"
//                                val intent = Intent(requireContext(), WebActivity::class.java)
//                                intent.putExtra("PAGE_KEY",libraryURL)
//                                startActivity(intent)
//                            })
//                        alertDialog.show()
//                    }
}
class NoScrollGridLayoutManager(context: Context, spanCount: Int) : GridLayoutManager(context, spanCount) {
    override fun canScrollVertically(): Boolean {
        // 縦方向のスクロールを禁止
        return false
    }

    override fun canScrollHorizontally(): Boolean {
        // 横方向のスクロールを禁止
        return false
    }
}