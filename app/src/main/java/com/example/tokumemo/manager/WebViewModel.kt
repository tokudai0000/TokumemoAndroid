package com.example.tokumemo.manager

import androidx.lifecycle.ViewModel

class WebViewModel: ViewModel() {
    // Safariで開く用として、現在表示しているURLを保存する
    public var loadingUrlStr = "https://www.google.com/?hl=ja"

//    private val dataManager = DataManager.singleton

//    private val safariUrls = ["https://teams.microsoft.com/l/meetup-join", "https://zoom.us/meeting/register/","https://join.skype.com/"]

    /// タイムアウトのURLであるか判定
    public fun isTimeout(urlStr: String): Boolean {
        return urlStr == Url.universityServiceTimeOut.string() || urlStr == Url.universityServiceTimeOut2.string()
    }

    /// Safariで開きたいURLであるか判定
    public func shouldOpenSafari(urlStr: String) -> Bool {
        for url in safariUrls {
            if urlStr.contains(url) {
                return true
            }
        }
        return false
    }

    /// JavaScriptを動かす種類
    enum JavaScriptType {
        case skipReminder // アンケート解答の催促画面
        case syllabus // シラバスの検索画面
        case loginIAS // 大学統合認証システム(IAS)のログイン画面
        case loginOutlook // メール(Outlook)のログイン画面
        case loginCareerCenter // 徳島大学キャリアセンターのログイン画面
        case none // JavaScriptを動かす必要がない場合
    }
    /// JavaScriptを動かしたい指定のURLかどうかを判定し、動かすJavaScriptの種類を返す
    ///
    /// - Note: canExecuteJavascriptが重要な理由
    ///   ログインに失敗した場合に再度ログインのURLが表示されることになる。
    ///   canExecuteJavascriptが存在しないと、再度ログインの為にJavaScriptが実行され続け無限ループとなってしまう。
    public func anyJavaScriptExecute(_ urlString: String) -> JavaScriptType {
        // JavaScriptを実行するフラグが立っていない場合はnoneを返す
        if dataManager.canExecuteJavascript == false {
            return .none
        }
        // アンケート解答の催促画面
        if urlString == Url.skipReminder.string() {
            return .skipReminder
        }
        // 大学統合認証システム(IAS)のログイン画面
        if urlString.contains(Url.universityLogin.string()) {
            return .loginIAS
        }
        // シラバスの検索画面
        if urlString == Url.syllabus.string() {
            return .syllabus
        }
        // メール(Outlook)のログイン画面
        if urlString.contains(Url.outlookLoginForm.string()) {
            return .loginOutlook
        }
        // 徳島大学キャリアセンターのログイン画面
        if urlString == Url.tokudaiCareerCenter.string() {
            return .loginCareerCenter
        }
        // それ以外なら
        return .none
    }
}