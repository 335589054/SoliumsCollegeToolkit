package com.solium.Unitoolkit.ui.life

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solium.Unitoolkit.UniToolkitApp
import com.solium.Unitoolkit.core.database.LedgerCategoryEntity
import com.solium.Unitoolkit.core.database.LedgerEntryEntity
import com.solium.Unitoolkit.core.utils.appFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LedgerViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as UniToolkitApp).database.ledgerDao()

    val entries: StateFlow<List<LedgerEntryEntity>> =
        dao.entries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenseCategories: StateFlow<List<LedgerCategoryEntity>> =
        dao.categories("expense").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val incomeCategories: StateFlow<List<LedgerCategoryEntity>> =
        dao.categories("income").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            seedCategories("expense", listOf("餐饮", "交通", "购物", "学习", "娱乐", "其他"))
            seedCategories("income", listOf("生活费", "兼职", "其他"))
        }
    }

    private suspend fun seedCategories(type: String, defaults: List<String>) {
        val existing = dao.categories(type).first()
        if (existing.isEmpty()) {
            defaults.forEachIndexed { i, name ->
                dao.insertCategory(LedgerCategoryEntity(name = name, type = type, icon = "", isCustom = false))
            }
        }
    }

    fun insert(entry: LedgerEntryEntity) = viewModelScope.launch { dao.insert(entry) }
    fun delete(entry: LedgerEntryEntity) = viewModelScope.launch { dao.delete(entry) }
    fun clearAll() = viewModelScope.launch { dao.clearAll() }
    fun addCategory(cat: LedgerCategoryEntity) = viewModelScope.launch { dao.insertCategory(cat) }

    companion object { val Factory = appFactory(::LedgerViewModel) }
}