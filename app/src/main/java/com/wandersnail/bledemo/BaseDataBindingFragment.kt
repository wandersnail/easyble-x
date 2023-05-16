package com.wandersnail.bledemo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider

/**
 *
 *
 * date: 2019/12/11 17:26
 * author: zengfansheng
 */
abstract class BaseDataBindingFragment<VM : BaseViewModel, B : ViewDataBinding> : BaseViewBindingFragment<B>(),
    ViewModelPage<VM> {
    protected lateinit var viewModel: VM
    private val waitingRunnableList = ArrayList<Runnable>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[getViewModelClass()]
        viewModel.bindLifecycle(lifecycle)
        waitingRunnableList.forEach { 
            it.run()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (bindLifecycle()) {
            binding.lifecycleOwner = viewLifecycleOwner
        }
        viewModel.setContext(requireContext())
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    fun postToViewModelInitialized(runnable: Runnable) {
        if (::viewModel.isInitialized) {
            runnable.run()
        } else {
            waitingRunnableList.add(runnable)
        }
    }
    
    fun isViewModelInitialized(): Boolean {
        return ::viewModel.isInitialized
    }

    open fun bindLifecycle(): Boolean {
        return true
    }
}