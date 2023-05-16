package com.wandersnail.bledemo;

import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

/**
 * date: 2022/10/4 01:04
 * author: zengfansheng
 */
public interface ViewBindingClassProvider<VB extends ViewBinding> {
    @NonNull
    Class<VB> getViewBindingClass();
}
