package com.demo1_prvt

import com.dolmen.md.demo1_prvt.*
import com.dolmen.serv.Txt
import com.dolmen.serv.table.RowID
import java.math.BigDecimal
import java.time.LocalDate

class Stats(val m: Demo1) {
    val itemQuery: Map<RowID, Shipping_Order_Product> = m.selectMap(Shipping_Order_Product.fId, "")
    fun makeStats(start: LocalDate? = null, finish: LocalDate? = null, abLimit: Int, bcLimit: Int) {
        makeProductStats(start, finish, abLimit, bcLimit, itemQuery)
        makeCustomerStats(start, finish, abLimit, bcLimit, itemQuery)
        Txt.info("Performed data analysis").msg()
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
            Product_Abc().apply {
                product = id
                name = products[id]?.name
                quantity = aggr.qnty
                sum = aggr.sum
                avg_Price = aggr.sum / aggr.qnty.toBigDecimal()
                cuSum += aggr.sum
                cusum = cuSum
                val cuPerc = (cuSum.setScale(4) / grandTotal) * BigDecimal(100)
                cuperc = cuPerc
                abc_Class = abcClass(cuPerc, abLimit, bcLimit)
                m.insert(this)
            }
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
            Customer_Abc().apply {
                customer = id
                name = customers[id]?.name
                sum = aggr.sum
                cuSum += aggr.sum
                cusum = cuSum
                val cuPerc = (cuSum.setScale(4) / grandTotal) * BigDecimal(100)
                cuperc = cuPerc
                abc_Class = abcClass(cuPerc, abLimit, bcLimit)
                m.insert(this)
            }
        }
    }

    private fun abcClass(cuPerc: BigDecimal, abLimit: Int, bcLimit: Int): String =
            when {
                cuPerc < abLimit.toBigDecimal() -> "A"
                cuPerc < bcLimit.toBigDecimal() -> "B"
                else -> "C"
            }
}