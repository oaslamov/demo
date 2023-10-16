package com.dlmdemo.demo1_prvt

import com.roofstone.md.demo1_prvt.*
import com.roofstone.serv.CONST
import com.roofstone.serv.Txt
import com.roofstone.serv.table.RowID
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.RoundingMode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.IsoFields
import java.util.*

class Stats(val m: Demo1) {
    data class CustomerData(val id: RowID, val name: String, val countryName: String)

    fun makeStats(start: LocalDate? = null, finish: LocalDate? = null, abLimit: Int, bcLimit: Int) {
        data class ProductAcc(var id: RowID, var name: String = "", var quantity: Int = 0, var sum: BigDecimal = ZERO)
        data class CustomerAcc(var id: RowID, var name: String = "", var sum: BigDecimal = ZERO)

        val orderCustomer = mutableMapOf<RowID, RowID?>()
        val ordersAgg = sortedMapOf<String, SortedMap<String, BigDecimal>>()
        val periodsAgg = mutableMapOf<String, BigDecimal>()
        val customers = readCustomersFromDb()
        m.iterate<Shipping_Order>("") { o ->
            if (o.customer != null) orderCustomer[o.id] = o.customer
            val d = o.date_Order_Paid
            if (d != null) {
                val period = "${d.year} Q${d.get(IsoFields.QUARTER_OF_YEAR)}"
                val country = customers[o.customer]?.countryName ?: "-"
                val s = o.total ?: ZERO
                if (ordersAgg[period] == null) ordersAgg[period] = sortedMapOf()
                val acc = ordersAgg[period]?.get(country) ?: ZERO
                ordersAgg[period]?.set(country, acc + s)
                if (periodsAgg[period] == null) periodsAgg[period] = ZERO
                val periodAcc = periodsAgg[period] ?: ZERO
                periodsAgg[period] = periodAcc + s
            }
        }

        m.deleteList(Sales_By_Country_Report.TABLE_ID, "")
        val roundTo = BigDecimal("0.1")
        ordersAgg.forEach { (period, countriesSums) ->
            var sum = BigDecimal("100").setScale(CONST.MAX_SCALE) // Distribute 100 percents proportionally
            var q = periodsAgg[period] ?: ZERO // Sales total for this period
            countriesSums.forEach { (countryName, countrySum) ->
                var percentage = ZERO
                if (q.signum() > 0) {
                    val w = sum / q
                    percentage = roundTo * (w * countrySum / roundTo).setScale(0, RoundingMode.HALF_UP)
                    sum -= percentage
                    q -= countrySum
                }
                val row = Sales_By_Country_Report()
                row.period = period
                row.country_Name = countryName
                row.sum = countrySum
                row.percentage = percentage
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
                    customerAbc[customerId]?.name = customers[customerId]?.name ?: "-"
                    customersTotal += item.sum ?: ZERO
                }
            }

        }

        m.iterate<Product>("") { p ->
            if (productsAbc[p.id] != null) productsAbc[p.id]?.name = p.name ?: ""
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

        var report = m.selectFirst(Report.fReport_Type, Report.REPORT_TYPE.DATA_ANALYSIS)
        val isNewReport = report == null
        if (isNewReport) report = Report()
        report?.report_Type = Report.REPORT_TYPE.DATA_ANALYSIS
        report?.report_Date = OffsetDateTime.now()
        if (isNewReport) m.insert(report) else m.update(report)

        Txt.info(m.MID("analysis_done")).msg()
    }

    private fun readCustomersFromDb(): MutableMap<RowID, CustomerData> {
        val countries = m.selectMap(Country.fId, "")
        val customers = mutableMapOf<RowID, CustomerData>()
        m.iterate<Customer>("") { row ->
            val countryName = countries[row.country]?.name ?: "-"
            customers[row.id] = CustomerData(row.id, row.name ?: "-", countryName)
        }
        return customers
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