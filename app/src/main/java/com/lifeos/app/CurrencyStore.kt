package com.lifeos.app

import android.content.Context

data class LifeOsCurrency(val code: String, val symbol: String, val label: String)

val LifeOsCurrencies = listOf(
    LifeOsCurrency("BDT", "Tk", "Bangladeshi Taka"),
    LifeOsCurrency("USD", "$", "US Dollar"),
    LifeOsCurrency("EUR", "€", "Euro"),
    LifeOsCurrency("GBP", "£", "British Pound"),
    LifeOsCurrency("JPY", "¥", "Japanese Yen"),
    LifeOsCurrency("INR", "₹", "Indian Rupee"),
    LifeOsCurrency("KRW", "₩", "South Korean Won")
)

class CurrencyStore(context: Context) {
    private val preferences = context.getSharedPreferences("lifeos_currency", Context.MODE_PRIVATE)

    fun read(): LifeOsCurrency {
        val code = preferences.getString("code", "BDT") ?: "BDT"
        return LifeOsCurrencies.firstOrNull { it.code == code } ?: LifeOsCurrencies.first()
    }

    fun save(currency: LifeOsCurrency) {
        preferences.edit().putString("code", currency.code).apply()
    }
}

fun formatLifeOsAmount(rawAmount: String, currency: LifeOsCurrency): String {
    val value = rawAmount.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
    return "${currency.symbol}${"%.2f".format(java.util.Locale.US, value)}"
}

fun totalLifeOsExpenses(expenses: List<LifeOsExpense>): Double =
    expenses.sumOf { it.amount.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0 }
