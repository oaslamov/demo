package com.dlmdemo.demo1_prvt

import com.dolmen.md.demo1_prvt.*
import com.dolmen.serv.Txt
import com.dolmen.serv.table.RowID
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.RoundingMode
import java.time.LocalDate

class Stats(val m: Demo1) {
    val itemQuery: Map<RowID, Shipping_Order_Product> = m.selectMap(Shipping_Order_Product.fId, "")
    fun makeStats(start: LocalDate? = null, finish: LocalDate? = null, abLimit: Int, bcLimit: Int) {
        makeProductStats(start, finish, abLimit, bcLimit, itemQuery)
        makeCustomerStats(start, finish, abLimit, bcLimit, itemQuery)
        Txt.info(m.MID("analysis_done")).msg()
    }


    fun makeProductStats(
        start: LocalDate?, finish: LocalDate?, abLimit: Int, bcLimit: Int,
        itemQuery: Map<RowID, Shipping_Order_Product>
    ) {
        data class Accum(val qnty: Int, val sum: BigDecimal)
        m.deleteList(Product_Abc.TABLE_ID, "")
        val products = m.selectMap(Product.fId, "")
        val items = itemQuery
            .values
            .groupingBy { it.product }
            .fold(Accum(qnty = 0, sum = ZERO)) { acc, e -> Accum(acc.qnty + e.quantity, acc.sum + (e.sum ?: ZERO)) }
            .toList()
            .sortedByDescending { (_, value) -> value.sum }
            .toMap()
        val grandTotal = items.values.sumOf { it.sum }
        var cuSum = ZERO
        items.forEach { (id, aggr) ->
            Product_Abc().apply {
                product = id
                name = products[id]?.name
                quantity = aggr.qnty
                sum = aggr.sum
                avg_Price = aggr.sum / aggr.qnty.toBigDecimal()
                cuSum += aggr.sum
                cusum = cuSum
                val cuPerc = percentage(cuSum, grandTotal)
                cuperc = cuPerc
                abc_Class = abcClass(cuPerc, abLimit, bcLimit)
                m.insert(this)
            }
        }
    }

    fun makeCustomerStats(
        start: LocalDate?, finish: LocalDate?, abLimit: Int, bcLimit: Int,
        itemQuery: Map<RowID, Shipping_Order_Product>
    ) {
        m.deleteList(Customer_Abc.TABLE_ID, "")
        val customers = m.selectMap(Customer.fId, "")
        val orders = m.selectMap(Shipping_Order.fId, "")
        val items = itemQuery
            .values
            .groupingBy { orders[it.shipping_Order]?.customer }
            .fold(ZERO) { acc, e -> acc + (e.sum ?: ZERO) }
            .toList()
            .sortedByDescending { (_, value) -> value }
            .toMap()
        val grandTotal = items.values.sumOf { it }
        var cuSum = ZERO
        items.forEach { (id, aggr) ->
            Customer_Abc().apply {
                customer = id
                name = customers[id]?.name
                sum = aggr
                cuSum += aggr
                cusum = cuSum
                val cuPerc = percentage(cuSum, grandTotal)
                cuperc = cuPerc
                abc_Class = abcClass(cuPerc, abLimit, bcLimit)
                m.insert(this)
            }
        }
    }

    private fun percentage(
        sum: BigDecimal, total: BigDecimal,
        scale: Int = 1, roundingMode: RoundingMode = RoundingMode.HALF_UP
    ) =
        (sum * BigDecimal(100) / total).setScale(scale, roundingMode)

    private fun abcClass(percentage: BigDecimal, abLimit: Int, bcLimit: Int): String =
        when {
            percentage < abLimit.toBigDecimal() -> "A"
            percentage < bcLimit.toBigDecimal() -> "B"
            else -> "C"
        }
}