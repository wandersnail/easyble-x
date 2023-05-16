package com.wandersnail.bledemo

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import androidx.viewbinding.ViewBinding

/**
 *
 *
 * date: 2020/3/29 09:46
 * author: zengfansheng
 */
@SuppressLint("Registered")
abstract class BaseViewBindingActivity<B : ViewBinding> : BaseActivity(), ViewBindingClassProvider<B> {
    protected lateinit var binding: B

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        beforeSetContentView(savedInstanceState)
        try {
            binding = viewBindingClass.getMethod("inflate", LayoutInflater::class.java).invoke(
                null,
                layoutInflater
            ) as B
            setContentView(binding.root)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
    
    open fun beforeSetContentView(savedInstanceState: Bundle?) {

    }
}