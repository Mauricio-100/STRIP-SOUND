package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SearchHistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    private val _history = MutableStateFlow<List<String>>(loadHistory())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    private fun loadHistory(): List<String> {
        val historyString = prefs.getString("history", "") ?: ""
        return if (historyString.isEmpty()) emptyList() else historyString.split(",,")
    }

    fun addSearchQuery(query: String) {
        if (query.isBlank()) return
        val current = loadHistory().toMutableList()
        current.remove(query) // Remove if exists to move to top
        current.add(0, query)
        if (current.size > 15) {
            current.removeAt(current.size - 1)
        }
        prefs.edit().putString("history", current.joinToString(",,")).apply()
        _history.value = current
    }

    fun clearHistory() {
        prefs.edit().remove("history").apply()
        _history.value = emptyList()
    }
}
