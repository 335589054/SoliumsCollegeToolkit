package com.unitoolkit.ui.shopping

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unitoolkit.UniToolkitApp
import com.unitoolkit.core.database.LedgerEntryEntity
import com.unitoolkit.core.database.PostponedItemRow
import com.unitoolkit.core.database.ShoppingActivityEntity
import com.unitoolkit.core.database.ShoppingItemEntity
import com.unitoolkit.core.utils.DateUtils
import com.unitoolkit.core.utils.appFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as UniToolkitApp).database
    private val dao = db.shoppingDao()
    private val ledgerDao = db.ledgerDao()

    val activities: StateFlow<List<ShoppingActivityEntity>> =
        dao.activities().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val postponedRows: StateFlow<List<PostponedItemRow>> =
        dao.postponedRows().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun items(activityId: Long): StateFlow<List<ShoppingItemEntity>> =
        dao.items(activityId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 新建采购活动；同类型的「暂缓采购」项目自动带入 */
    fun createActivity(name: String, type: String) = viewModelScope.launch {
        val newId = dao.insertActivity(ShoppingActivityEntity(name = name, type = type))
        val carry = dao.postponedForType(type).map { it.copy(id = 0, activityId = newId, isPostponed = false, isDone = false) }
        carry.forEach { dao.insertItem(it) }
    }

    fun deleteActivity(activity: ShoppingActivityEntity) = viewModelScope.launch {
        dao.deleteItemsByActivity(activity.id)
        dao.deleteActivity(activity)
    }

    fun addItem(activityId: Long, name: String) = viewModelScope.launch {
        if (name.isNotBlank()) dao.insertItem(ShoppingItemEntity(activityId = activityId, name = name.trim()))
    }

    fun addItems(activityId: Long, names: List<String>) = viewModelScope.launch {
        names.filter { it.isNotBlank() }.forEach { dao.insertItem(ShoppingItemEntity(activityId = activityId, name = it.trim())) }
    }

    fun setDone(item: ShoppingItemEntity, done: Boolean) = viewModelScope.launch {
        dao.updateItem(item.copy(isDone = done))
    }

    fun togglePostponed(item: ShoppingItemEntity) = viewModelScope.launch {
        dao.updateItem(item.copy(isPostponed = !item.isPostponed))
    }

    /** 采购完成：未购项目移入「暂缓采购区」，可选输入总金额同步到账单 */
    fun completeActivity(activity: ShoppingActivityEntity, amount: Double?) = viewModelScope.launch {
        dao.items(activity.id).first().filter { !it.isDone }.forEach { dao.updateItem(it.copy(isPostponed = true)) }
        if (amount != null && amount > 0.0) {
            ledgerDao.insert(
                LedgerEntryEntity(
                    type = "expense",
                    amount = amount,
                    category = "购物",
                    note = activity.name,
                    date = DateUtils.nowDateTime(),
                    paymentMethod = "",
                )
            )
        }
    }

    fun deleteItem(item: ShoppingItemEntity) = viewModelScope.launch { dao.deleteItem(item) }

    companion object { val Factory = appFactory(::ShoppingViewModel) }
}