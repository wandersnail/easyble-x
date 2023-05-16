package com.wandersnail.bledemo

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import java.lang.ref.WeakReference

/**
 *
 *
 * date: 2019/12/9 10:44
 * author: zengfansheng
 */
abstract class BaseViewModel : ViewModel(), DefaultLifecycleObserver {
    private var weakContext: WeakReference<Context>? = null

    fun setContext(context: Context) {
        weakContext = WeakReference(context)
    }

    fun getContext(): Context {
        return weakContext?.get() ?: MyApplication.getInstance()
    }
    
    /**
     * 绑定页面生命周期
     */
    fun bindLifecycle(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
    }
    
    fun getString(@StringRes resId: Int): String {
        return getContext().getString(resId)
    }
    
    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String {
        return getContext().getString(resId, formatArgs)
    }
}