package com.demo1_prvt.filler

import com.dolmen.md.demo1_prvt.Decor_Product_Decor
import com.dolmen.md.demo1_prvt.Product
import com.dolmen.ui.rowstyling.DecorData
import com.dolmen.ui.rowstyling.Style
import java.math.BigDecimal

class ProductFiller : Product.IProduct {
    override fun getUrl(table: Product): String {
        val engines = listOf(
                "https://www.google.com/search?q=",
                "https://www.bing.com/search?q=",
                "https://duckduckgo.com/?q=")
        return "${engines.random()}${table.name}"
    }

    override fun setUrl(table: Product, value: String?) {}

    override fun getDecor(table: Product): DecorData<Decor_Product_Decor> {
        val decorData = Decor_Product_Decor.newData()
        val styleGreen = Style().apply { color(Style.COLOR_GREEN) }
        val styleYellow = Style().apply { color(Style.COLOR_YELLOW) }
        val styleRed = Style().apply { color(Style.COLOR_RED) }
        val p = table.price
        if (p != null) {
            when {
                p.compareTo(BigDecimal(10)) < 0 -> decorData.set(Decor_Product_Decor.Price_Level, styleGreen)
                p.compareTo(BigDecimal(20)) < 0 -> decorData.set(Decor_Product_Decor.Price_Level, styleYellow)
                else -> decorData.set(Decor_Product_Decor.Price_Level, styleRed)
            }
        }
        return decorData
    }
}