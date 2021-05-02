package com.demo1_prvt

import com.dolmen.util.JSONManager

data class Legend(val code: String, val name: String, val type: String, val color: String? = null)
class Chart {
    val legends: MutableList<Legend> = mutableListOf()
    val data: MutableList<Map<String, String>> = mutableListOf()
    fun getJSON(): String = JSONManager.getJson(mapOf("legends" to legends, "data" to data), true)
}