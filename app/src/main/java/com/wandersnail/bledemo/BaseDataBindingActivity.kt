package com.wandersnail.bledemo

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider

/**
 *
 *
 * date: 2020/3/29 09:46
 * author: zengfansheng
 */
@SuppressLint("Registered")
abstract class BaseDataBindingActivity<VM : BaseViewModel, B : ViewDataBinding> : BaseViewBindingActivity<B>(),
    ViewModelPage<VM> {
    protected lateinit var viewModel: VM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[getViewModelClass()]
        viewModel.setContext(this)
        viewModel.bindLifecycle(lifecycle)
    }
}