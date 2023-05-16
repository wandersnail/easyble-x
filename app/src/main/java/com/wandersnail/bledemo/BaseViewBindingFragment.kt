package com.wandersnail.bledemo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 *
 *
 * date: 2019/12/11 17:26
 * author: zengfansheng
 */
abstract class BaseViewBindingFragment<B : ViewBinding> : Fragment(), ViewBindingClassProvider<B> {
    protected lateinit var binding: B
    
    @Suppress("UNCHECKED_CAST")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        try {
            binding = viewBindingClass.getMethod(
                "inflate",
                LayoutInflater::class.java,
                ViewGroup::class.java,
                Boolean::class.javaPrimitiveType
            ).invoke(null, inflater, container, false) as B
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return binding.root
    }
    
    fun isBindingInflated(): Boolean {
        return ::binding.isInitialized
    }
}