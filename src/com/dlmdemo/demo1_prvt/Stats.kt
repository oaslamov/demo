package com.dlmdemo.demo1_prvt

import com.dolmen.md.demo1_prvt.*
import com.dolmen.serv.Txt
import com.dolmen.serv.table.RowID
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.IsoFields

class Stats(val m: Demo1) {
    //val fetchSize = 200

    fun makeStats(start: LocalDate? = null, finish: LocalDate? = null, abLimit: Int, bcLimit: Int) {
        makeAbcStats(start, finish, abLimit, bcLimit)
        Txt.info(m.MID("analysis_done")).msg()
    }

    fun makeAbcStats(start: LocalDate?, finish: LocalDate?, abLimit: Int, bcLimit: Int) {
        data class ProductAcc(var id: RowID, var name: String = "", var quantity: Int = 0, var sum: BigDecimal = ZERO)
        data class CustomerAcc(var id: RowID, var name: String = "", var sum: BigDecimal = ZERO)

        val orderCustomer = mutableMapOf<RowID, RowID?>()
        val ordersAggr = sortedMapOf<String, MutableMap<String, BigDecimal>>()
        val countries = m.selectMap(Country.fId, "")
        val customers = m.selectMap(Customer.fId, "")
        m.iterate<Shipping_Order>("") { o ->
            if (o.customer != null) orderCustomer[o.id] = o.customer
            val d = o.date_Order_Paid
            if (d != null) {
                val period = "${d.year} Q${d.get(IsoFields.QUARTER_OF_YEAR)}"
                val country = countries[customers[o.customer]?.country]?.name ?: "-"
                val s = o.total ?: ZERO
                if (ordersAggr[period] == null) ordersAggr[period] = sortedMapOf()
                val acc = ordersAggr[period]?.get(country) ?: ZERO
                ordersAggr[period]?.set(country, acc + s)
            }
        }

        m.deleteList(Sales_By_Country_Report.TABLE_ID,"")
        ordersAggr.forEach { (period, countriesSums) ->
            countriesSums.forEach { (countryName, sum) ->
                val row = Sales_By_Country_Report()
                row.period = period
                row.country_Name = countryName
                row.sum = sum
                m.insert(row)
            }
        }


        val productsAbc = mutableMapOf<RowID?, ProductAcc?>()
        var productsTotal = ZERO
        val customerAbc = mutableMapOf<RowID?, CustomerAcc?>()
        var customersTotal = ZERO
        m.iterate<Shipping_Order_Product>("")
        { item ->
            val productId = item.product
            if (productId != null) {
                if (productsAbc[productId] == null) productsAbc[productId] = ProductAcc(id = productId)
                val productSum = productsAbc[productId]?.sum ?: ZERO
                productsAbc[productId]?.sum = productSum + (item.sum ?: ZERO)
                val productQnty = productsAbc[productId]?.quantity ?: 0
                productsAbc[productId]?.quantity = productQnty + item.quantity
                productsTotal += item.sum ?: ZERO
            }
            val orderId = item.shipping_Order
            if (orderId != null) {
                val customerId = orderCustomer[orderId]
                if (customerId != null) {
                    if (customerAbc[customerId] == null) customerAbc[customerId] = CustomerAcc(id = customerId)
                    val customerSum = customerAbc[customerId]?.sum ?: ZERO
                    customerAbc[customerId]?.sum = customerSum + (item.sum ?: ZERO)
                    customersTotal += item.sum ?: ZERO
                }
            }

        }

        m.iterate<Product>("")
        { p ->
            if (productsAbc[p.id] != null) productsAbc[p.id]?.name = p.name ?: ""
        }

        m.iterate<Customer>("")
        { c ->
            if (customerAbc[c.id] != null) customerAbc[c.id]?.name = c.name ?: ""
        }

        m.deleteList(Product_Abc.TABLE_ID, "")
        var cuSum = ZERO
        productsAbc.values.sortedByDescending { it?.sum }.forEach { agg ->
            if (agg != null) {
                Product_Abc().apply {
                    product = agg.id
                    name = agg.name
                    quantity = agg.quantity
                    sum = agg.sum
                    avg_Price = agg.sum / agg.quantity.toBigDecimal()
                    cuSum += agg.sum
                    cusum = cuSum
                    val cuPerc = percentage(cuSum, productsTotal)
                    cuperc = cuPerc
                    abc_Class = abcClass(cuPerc, abLimit, bcLimit)
                    m.insert(this)
                }
            }
        }

        m.deleteList(Customer_Abc.TABLE_ID, "")
        cuSum = ZERO
        customerAbc.values.sortedByDescending { it?.sum }.forEach { agg ->
            if (agg != null) {
                Customer_Abc().apply {
                    customer = agg.id
                    name = agg.name
                    sum = agg.sum
                    cuSum += agg.sum
                    cusum = cuSum
                    val cuPerc = percentage(cuSum, customersTotal)
                    cuperc = cuPerc
                    abc_Class = abcClass(cuPerc, abLimit, bcLimit)
                    m.insert(this)
                }
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