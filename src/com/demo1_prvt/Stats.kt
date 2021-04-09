package com.demo1_prvt

import com.dolmen.md.demo1_prvt.Product
import com.dolmen.md.demo1_prvt.Product_Abc
import com.dolmen.md.demo1_prvt.Shipping_Order_Product
import java.math.BigDecimal
import java.time.LocalDate

class Stats(val m: MyModule) {
    fun makeStats(start: LocalDate?, finish: LocalDate?, abLimit: Int, bcLimit: Int) {
        val statsTableName = "demo1_prvt.product_abc"
        m.deleteList(statsTableName, "")
        val products = m.selectMap(Product.fId, "")
        val items = m.selectMap(Shipping_Order_Product.fId, "")
                .values
                .groupingBy { it.product }
                .aggregate { _, acc: Pair<Int, BigDecimal>?, item, _ ->
                    Pair(
                            (acc?.first ?: 0) + item.quantity,
                            (acc?.second ?: BigDecimal.ZERO) + (item.sum ?: BigDecimal.ZERO)
                    )
                }
                .toList()
                .sortedByDescending { (_, value) -> value.second }
                .toMap()
        val grandTotal = items.values.sumOf { it.second }
        var cuSum = BigDecimal.ZERO
        items.forEach { (id, aggr) ->
            val stat = Product_Abc()
            stat.product = id
            stat.name = products[id]?.name
            stat.quantity = aggr.first
            stat.sum = aggr.second
            stat.avg_Price = aggr.second / aggr.first.toBigDecimal()
            cuSum += aggr.second
            stat.cusum = cuSum
            val cuPerc = (cuSum.setScale(4) / grandTotal) * 100.toBigDecimal()
            stat.cuperc = cuPerc
            stat.abc_Class = when {
                cuPerc < abLimit.toBigDecimal() -> Product_Abc.ABC_CLASS.A
                cuPerc < bcLimit.toBigDecimal() -> Product_Abc.ABC_CLASS.B
                else -> Product_Abc.ABC_CLASS.C
            }
            m.insert(stat)
        }
    }
}