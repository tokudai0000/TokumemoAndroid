package com.tokudai0000.tokumemo.ui.main.fragment.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.tokudai0000.tokumemo.common.AKLog
import com.tokudai0000.tokumemo.common.AKLogLevel
import com.tokudai0000.tokumemo.domain.model.AdItem
import kotlinx.coroutines.launch

class HomeViewModel: ViewModel() {

    val numberOfUsers = MutableLiveData<String>()

    var prItems = arrayListOf<AdItem>()
    var displayPrItem = MutableLiveData<AdItem>()

    var univItems = arrayListOf<AdItem>()
    var displayUnivItem = MutableLiveData<AdItem>()

    fun getNumberOfUsers() {
        viewModelScope.launch {
            try {
                val url = "https://tokudai0000.github.io/tokumemo_resource/api/v1/number_of_users.json"
                url.httpGet().responseJson { _, _, result  ->
                    when (result) {
                        is Result.Success -> {
                            val item: String = result.get().obj().getString("numberOfUsers")
                            numberOfUsers.postValue(item)
                        }
                        is Result.Failure -> {
                            val error = result.getException()
                            AKLog(AKLogLevel.ERROR, "Error: getNumberOfUsers API通信失敗 - ステータスコード: ${error.message}")
                        }
                    }
                }
            } catch (exception: Exception) {
                AKLog(AKLogLevel.ERROR, "Error: getCurrentTermVersion API通信失敗 - 例外: ${exception.message}")
            }
        }
    }

    fun getAdItems() {
        viewModelScope.launch {
            try {
                val url = "https://tokudai0000.github.io/tokumemo_resource/api/v1/ad_items.json"
                url.httpGet().responseJson { _, _, result  ->
                    when (result) {
                        is Result.Success -> {

                            // Jsonデータから内容物を取得
                            val prItems = result.get().obj().getJSONArray("prItems")
                            val univItems = result.get().obj().getJSONArray("univItems")

                            for (i in 0 until prItems.length()) {
                                prItems.getJSONObject(i)?.let {
                                    val jsonObject = prItems.getJSONObject(i)
                                    val prItem = AdItem(
                                        id = jsonObject.getInt("id"),
                                        clientName = jsonObject.getString("clientName"),
                                        imageUrlStr = jsonObject.getString("imageUrlStr"),
                                        targetUrlStr = jsonObject.getString("targetUrlStr"),
                                        imageDescription = jsonObject.getString("imageDescription"),
                                    )
                                    displayPrItem.postValue(prItem)
                                    this@HomeViewModel.prItems.add(prItem)
                                }
                            }

                            for (j in 0 until univItems.length()) {
                                univItems.getJSONObject(j)?.let {
                                    AKLog(AKLogLevel.DEBUG, "univItems $it")
                                    val jsonObject = univItems.getJSONObject(j)
                                    val univItem = AdItem(
                                        id = jsonObject.getInt("id"),
                                        clientName = jsonObject.getString("clientName"),
                                        imageUrlStr = jsonObject.getString("imageUrlStr"),
                                        targetUrlStr = jsonObject.getString("targetUrlStr"),
                                        imageDescription = jsonObject.getString("imageDescription"),
                                    )
                                    displayUnivItem.postValue(univItem)
                                    this@HomeViewModel.univItems.add(univItem)
                                }
                            }
                        }
                        is Result.Failure -> {
                            val error = result.getException()
                            AKLog(AKLogLevel.ERROR, "Error: getAdItems API通信失敗 - ステータスコード: ${error.message}")
                        }
                    }
                }
            } catch (exception: Exception) {
                AKLog(AKLogLevel.ERROR, "Error: getAdItems API通信失敗 - 例外: ${exception.message}")
            }
        }
    }
    
    fun randomChoiceForAdImage(adItems: ArrayList<AdItem>, displayAdItem: AdItem?): AdItem? {
        // 広告数が0か1の場合はローテーションする必要がない
        if (adItems.count() == 0) {
            return null
        }

        if (adItems.count() == 1) {
            return adItems[0]
        }

        displayAdItem?.let {
            // 必ず2つ以上の広告が存在することを確認済み
            while (true) {
                val ramdomItem = adItems.random()
                // 前回の画像表示番号と同じであれば、再度繰り返す
                if (ramdomItem != displayAdItem) {
                    return ramdomItem
                }
            }
        }
        return adItems.random()
    }
}