package com.demo1_prvt.filler

import com.dolmen.md.demo1_prvt.Product

class ProductFiller : Product.IProduct {
    override fun getUrl(table: Product): String {
        val engines = listOf(
                "https://www.google.com/search?q=",
                "https://www.bing.com/search?q=",
                "https://duckduckgo.com/?q=")
        return "${engines.random()}${table.name}"
    }

    override fun setUrl(table: Product, value: String?) {}
}