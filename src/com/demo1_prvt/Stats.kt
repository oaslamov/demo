package com.demo1_prvt

import com.dolmen.md.demo1_prvt.*
import com.dolmen.serv.table.RowID
import java.math.BigDecimal
import java.time.LocalDate

class Stats(val m: MyModule) {
    val itemQuery: Map<RowID, Shipping_Order_Product> = m.selectMap(Shipping_Order_Product.fId, "")
    fun makeStats(start: LocalDate?, finish: LocalDate?, abLimit: Int, bcLimit: Int) {
        makeProductStats(start, finish, abLimit, bcLimit, itemQuery)
        makeCustomerStats(start, finish, abLimit, bcLimit, itemQuery)
    }

    private data class Accum(val qnty: Int, val sum: BigDecimal)

    fun makeProductStats(start: LocalDate?, finish: LocalDate?, abLimit: Int, bcLimit: Int,
                         itemQuery: Map<RowID, Shipping_Order_Product>) {
        val statsTableName = "demo1_prvt.product_abc"
        m.deleteList(statsTableName, "")
        val products = m.selectMap(Product.fId, "")
        val items = itemQuery
                .values
                .groupingBy { it.product }
                .aggregate { _, acc: Accum?, item, _ ->
                    Accum(
                            (acc?.qnty ?: 0) + item.quantity,
                            (acc?.sum ?: BigDecimal.ZERO) + (item.sum ?: BigDecimal.ZERO)
                    )
                }
                .toList()
                .sortedByDescending { (_, value) -> value.sum }
                .toMap()
        val grandTotal = items.values.sumOf { it.sum }
        var cuSum = BigDecimal.ZERO
        items.forEach { (id, aggr) ->
            val stat = Product_Abc()
            stat.product = id
            stat.name = products[id]?.name
            stat.quantity = aggr.qnty
            stat.sum = aggr.sum
            stat.avg_Price = aggr.sum/ aggr.qnty.toBigDecimal()
            cuSum += aggr.sum
            stat.cusum = cuSum
            val cuPerc = (cuSum.setScale(4) / grandTotal) * 100.toBigDecimal()
            stat.cuperc = cuPerc
            stat.abc_Class = abcClass(cuPerc, abLimit, bcLimit)
            m.insert(stat)
        }
    }

    fun makeCustomerStats(start: LocalDate?, finish: LocalDate?, abLimit: Int, bcLimit: Int,
                          itemQuery: Map<RowID, Shipping_Order_Product>) {
        val statsTableName = "demo1_prvt.customer_abc"
        m.deleteList(statsTableName, "")
        val customers = m.selectMap(Customer.fId, "")
        val orders = m.selectMap(Shipping_Order.fId, "")
        val items = itemQuery
                .values
                .groupingBy { orders[it.shipping_Order]?.customer }
                .aggregate { _, acc: Accum?, item, _ ->
                    Accum(
                            (acc?.qnty ?: 0) + item.quantity,
                            (acc?.sum ?: BigDecimal.ZERO) + (item.sum ?: BigDecimal.ZERO)
                    )
                }
                .toList()
                .sortedByDescending { (_, value) -> value.sum }
                .toMap()
        val grandTotal = items.values.sumOf { it.sum }
        var cuSum = BigDecimal.ZERO
        items.forEach { (id, aggr) ->
            val stat = Customer_Abc()
            stat.customer = id
            stat.name = customers[id]?.name
            stat.sum = aggr.sum
            cuSum += aggr.sum
            stat.cusum = cuSum
            val cuPerc = (cuSum.setScale(4) / grandTotal) * 100.toBigDecimal()
            stat.cuperc = cuPerc
            stat.abc_Class = abcClass(cuPerc, abLimit, bcLimit)
            m.insert(stat)
        }
    }

    private fun abcClass(cuPerc: BigDecimal, abLimit: Int, bcLimit: Int): String {
        return when {
            cuPerc < abLimit.toBigDecimal() -> "A"
            cuPerc < bcLimit.toBigDecimal() -> "B"
            else -> "C"
        }
    }
}