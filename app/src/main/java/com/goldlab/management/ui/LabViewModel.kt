package com.goldlab.management.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.goldlab.management.data.PaymentEntity
import com.goldlab.management.data.PurchaseEntity
import com.goldlab.management.data.PurchaseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LabViewModel(private val repo: PurchaseRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    val searchQuery: StateFlow<String> = query.asStateFlow()

    val purchases = query
        .flatMapLatest { if (it.isBlank()) repo.observePurchases() else repo.searchPurchases(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers = repo.observeCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val selectedPurchaseId = MutableStateFlow<Long?>(null)

    val selectedPurchasePayments: StateFlow<List<PaymentEntity>> =
        selectedPurchaseId
            .flatMapLatest { id ->
                if (id == null) flowOf(emptyList())
                else repo.observePaymentsForPurchase(id)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setQuery(q: String) {
        query.value = q
    }

    fun showPayments(purchaseId: Long) {
        selectedPurchaseId.value = purchaseId
    }

    fun closePayments() {
        selectedPurchaseId.value = null
    }

    fun clearMessage() {
        _message.value = null
    }

    fun addCustomer(
        name: String,
        phone: String,
        kind: String,
        address: String,
        notes: String
    ) = viewModelScope.launch {
        runCatching { repo.addCustomer(name, phone, kind, address, notes) }
            .onSuccess { _message.value = "Customer saved" }
            .onFailure { _message.value = it.message ?: "Could not save customer" }
    }

    fun addPurchase(p: PurchaseEntity) = viewModelScope.launch {
        runCatching { repo.addPurchase(p) }
            .onSuccess { _message.value = "Purchase saved successfully" }
            .onFailure { _message.value = it.message ?: "Could not save purchase" }
    }

    fun recordPayment(
        purchaseId: Long,
        amount: String,
        method: String,
        note: String,
        paidAt: Long = System.currentTimeMillis()
    ) = viewModelScope.launch {
        runCatching {
            repo.recordPayment(purchaseId, amount, method, note, paidAt)
        }.onSuccess {
            _message.value = "Payment saved successfully"
        }.onFailure {
            _message.value = it.message ?: "Could not save payment"
        }
    }

    fun deletePurchase(p: PurchaseEntity) = viewModelScope.launch {
        runCatching { repo.deletePurchase(p) }
            .onSuccess { _message.value = "Transaction deleted" }
            .onFailure { _message.value = it.message ?: "Delete failed" }
    }

    fun dashboardTotal(items: List<PurchaseEntity>) =
        items.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(runCatching { BigDecimal(item.totalAmount) }.getOrDefault(BigDecimal.ZERO))
        }
}

class LabViewModelFactory(private val repo: PurchaseRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LabViewModel(repo) as T
}
