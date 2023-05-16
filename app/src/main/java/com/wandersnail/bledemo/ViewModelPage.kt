package com.wandersnail.bledemo

/**
 *
 *
 * date: 2020/3/29 12:29
 * author: zengfansheng
 */
interface ViewModelPage<VM : BaseViewModel> {
    fun getViewModelClass(): Class<VM>
}