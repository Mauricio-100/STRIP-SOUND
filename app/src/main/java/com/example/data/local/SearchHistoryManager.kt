package com.example.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SearchHistoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("search_prefs", Context.MODE_PRIVATE)

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val raw = prefs.getString("queries", "") ?: ""
        if (raw.isBlank()) {
            _history.value = emptyList()
        } else {
            _history.value = raw.split("|||").filter { it.isNotBlank() }
        }
    }

    fun addSearchQuery(query: String) {
        if (query.isBlank()) return
        val current = _history.value.toMutableList()
        current.remove(query)
        current.add(0, query)
        // Keep last 15 queries
        val updated = current.take(15)
        _history.value = updated
        prefs.edit().putString("queries", updated.joinToString("|||")).apply()
    }

    fun deleteSearchQuery(query: String) {
        val current = _history.value.toMutableList()
        current.remove(query)
        _history.value = current
        prefs.edit().putString("queries", current.joinToString("|||")).apply()
    }

    fun clearHistory() {
        prefs.edit().remove("queries").apply()
        _history.value = emptyList()
    }
}
