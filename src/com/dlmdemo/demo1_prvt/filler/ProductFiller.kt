package com.dlmdemo.demo1_prvt.filler

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
            "https://duckduckgo.com/?q="
        )
        return "${engines.random()}${table.name}"
    }

    override fun getDecor(table: Product): DecorData<Decor_Product_Decor> {
        val decorData = Decor_Product_Decor.newData()
        val p = table.price
        val stylePriceLevel = Style().apply { color(Style.CONTRAST100) }
        if (p != null) {
            stylePriceLevel.bgColor(
                when {
                    p.compareTo(BigDecimal(10)) < 0 -> Style.COLOR_GREEN
                    p.compareTo(BigDecimal(20)) < 0 -> Style.COLOR_YELLOW
                    else -> Style.COLOR_RED
                }
            )
        }
        decorData.set(Decor_Product_Decor.Price_Level, stylePriceLevel)
        val styleDropdownList = stylePriceLevel.clone().apply {
            align(Style.ALIGN_CENTER)
            font(Style.FONT_BOLD + Style.FONT_ITALIC)
        }
        decorData.set(Decor_Product_Decor.Dropdown_List, styleDropdownList)
        return decorData
    }
}