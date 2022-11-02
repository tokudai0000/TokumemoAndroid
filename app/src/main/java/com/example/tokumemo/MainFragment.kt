package com.example.tokumemo

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import com.example.tokumemo.databinding.MainFragmentBinding
import com.example.tokumemo.flag.MainModel
import com.example.tokumemo.manager.DataManager

class MainFragment: Fragment(R.layout.main_fragment) {
//    private val vm: MainModel by viewModels()
    private val vm : ViewModel by activityViewModels()

    private var _binding: MainFragmentBinding? = null
    private val binding: MainFragmentBinding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this._binding = MainFragmentBinding.bind(view)

        binding.carrierCenter.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_newsFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        this._binding = null
    }

    // 押されたWebサイトにとぶ
    private fun goWeb(pageId: String) {
//        val intent = Intent(this, WebActivity::class.java)
//        // WebActivityにどのWebサイトを開こうとしているかをIdとして送信して知らせる
//        intent.putExtra("PAGE_KEY",pageId)
        // Use the Kotlin extension in the fragment-ktx artifact
        setFragmentResult("requestKey", bundleOf("bundleKey" to pageId))
        // 自動入力のフラグを上げる
        DataManager.canExecuteJavascript = true
        findNavController().navigate(R.id.action_mainFragment_to_newsFragment)
    }
}