package com.tokudai0000.tokumemo.domain.model

import java.io.Serializable

data class AdItem(
    val id: Int,
    val clientName: String,
    val imageUrlStr: String,
    val targetUrlStr: String,
    val imageDescription: String,
) : Serializable
// Serializable は putExtra でデータ送信する際のバイト列変換のライブラリ
