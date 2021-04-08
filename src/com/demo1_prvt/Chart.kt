package com.demo1_prvt

import com.dolmen.util.JSONManager

data class Legend(val code: String, val name: String, val type: String, val color: String = "")

class Chart() {
    val legends: MutableList<Legend> = mutableListOf<Legend>()
    val data: MutableList<Map<String, String>> = mutableListOf()
    fun getJSON(): String {
        val resJSON = mutableMapOf<String, Any>()
        resJSON["legends"] = legends
        resJSON["data"] = data
        return JSONManager.getJson(resJSON, true)
    }
}