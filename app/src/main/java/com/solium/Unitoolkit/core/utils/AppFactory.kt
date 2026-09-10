package com.solium.Unitoolkit.core.utils

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/** 基于 AndroidViewModel 的通用 Factory，自动注入 application */
inline fun <reified T : AndroidViewModel> appFactory(crossinline create: (Application) -> T) =
    viewModelFactory { initializer { create(this[APPLICATION_KEY]!!) } }