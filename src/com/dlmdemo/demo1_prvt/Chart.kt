package com.dlmdemo.demo1_prvt

import com.dolmen.md.demo1_prvt.*
import com.dolmen.serv.CONST.MAX_SCALE
import com.dolmen.serv.aggregate.Count
import com.dolmen.serv.anno.Description
import com.dolmen.serv.anno.Parameters
import com.dolmen.serv.exp.FieldLimit
import com.dolmen.serv.exp.Formula
import com.dolmen.serv.exp.QueryHelper
import com.dolmen.ui.screen.ChartData
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.RoundingMode
import java.time.temporal.IsoFields

class ChartManager(val m: Demo1) {
    //val fetchSize = 200
    fun getChartExample2(filter: String): ChartData<*, *> {
        val f = Formula.parse(filter, com.dolmen.md.std.Parameters.T)
        val isShowy2 = true == FieldLimit.getEqual(f, com.dolmen.md.std.Parameters.fCheck)
        return getChartExample(isShowy2)
    }

    @Description("Prepares JSON for charts example")
    fun getChartExample(isShowy2: Boolean?): ChartData<*, *> {
        val is2 = isShowy2 != null && isShowy2
        val data = ChartData<String, Int>()
        data.setLegendX("year", "string")
        data.setLegendY(0, m.xtr("label_west"))
        data.setLegendY(1, m.xtr("label_east")).alternativeAxis(is2)

        data.add("2016", 4001, 7000)
        data.add("2017", 5000, 6000)
        data.add("2018", 2500, 12000)
        data.add("2019", 1200, 19000)
        data.add("2020", 3365, 9000)
        data.add("2021", 4345, 19000)

        return data
    }

    @Description("Prepares JSON for ABC analysis graph")
    fun getChartABC(): ChartData<*, *> {
        val data = ChartData<Int, BigDecimal>()
        data.setLegendX("% items", "number")
        data.setLegendY(0, m.xtr("label_p_revenue"), "number")
        data.setLegendY(1, m.xtr("label_c_revenue"), "number")

        val productFilter = Formula.parse(QueryHelper.c().orderBy(Product_Abc.fSum, true).toString(), Product_Abc.T)
        //productFilter.expectedRows = fetchSize
        val products = m.selectMap(Product_Abc.fId, productFilter).values.toList()
        val maxProduct = products.size

        val customerFilter = Formula.parse(QueryHelper.c().orderBy(Customer_Abc.fSum, true).toString(), Customer_Abc.T)
        //customerFilter.expectedRows = fetchSize
        val customers = m.selectMap(Customer_Abc.fId, customerFilter).values.toList()
        val maxCustomer = customers.size

        data.add(0, ZERO, ZERO)
        for (x in 5..100 step 5) {
            val y1 = if (maxProduct > 0) {
                val iProduct = (x * maxProduct / 100 - 1).coerceIn(0, maxProduct - 1)
                products[iProduct].cuperc
            } else ZERO
            val y2 = if (maxCustomer > 0) {
                val iCustomer = (x * maxCustomer / 100 - 1).coerceIn(0, maxCustomer - 1)
                customers[iCustomer].cuperc
            } else ZERO
            data.add(x, y1, y2)
        }

        return data
    }

    @Description("Prepares JSON for Order totals chart")
    @Parameters("points: Groups limits")
    fun getChartOrderTotals(points: String): ChartData<*, *> {
        fun countOrders(filter: QueryHelper): Long =
            (m.aggregates(Formula.parse(filter.toString(), Shipping_Order.T), Count())[0].result as Number).toLong()

        val data = ChartData<String, Long>()
        if (points.isBlank()) return data
        val limits = points.split(",").map { it.trim().toInt() }.distinct().sorted()
        val limitsSize = limits.size
        if (limitsSize == 0) return data

        data.setLegendX(m.xtr("label_order_total"), "string")
        data.setLegendY(0, m.xtr("label_count"), "number")

        for (i in 0..limitsSize) {
            var x: String
            var y: Long
            when {
                i == 0 -> {
                    x = "≤${limits.first()}.00"
                    y = countOrders(QueryHelper.c().and(Shipping_Order.fTotal, "<=", limits.first()))
                }
                i < limitsSize -> {
                    x = "${limits[i - 1]}.01-${limits[i]}.00"
                    y = countOrders(
                        QueryHelper.c().and(Shipping_Order.fTotal, ">", limits[i - 1])
                            .and(Shipping_Order.fTotal, "<=", limits[i])
                    )
                }
                else -> {
                    x = ">${limits.last()}.00"
                    y = countOrders(QueryHelper.c().and(Shipping_Order.fTotal, ">", limits.last()))
                }
            }
            data.add(x, y)
        }
        return data
    }

    @Description("Prepares JSON for Sales by country chart")
    fun getChartSalesByCountry(filter: String): ChartData<*, *> {
        val customerFilter = Formula.parse("", Customer.T)
        //customerFilter.expectedRows = fetchSize
        val customers = m.selectMap(Customer.fId, customerFilter)

        val countryFilter = Formula.parse("", Country.T)
        //countryFilter.expectedRows = fetchSize
        val countries = m.selectMap(Country.fId, countryFilter)

        val ct = mutableListOf<String>()

        val shippingOrderFilter = Formula.parse(filter, Shipping_Order.T)
        //shippingOrderFilter.expectedRows = fetchSize

        val ordersAggr = sortedMapOf<String, MutableMap<String, BigDecimal>>()

        m.iterate<Shipping_Order>(shippingOrderFilter) { o ->
            val d = o.date_Order_Paid
            if (d != null) {
                val p = "${d.year} Q${d.get(IsoFields.QUARTER_OF_YEAR)}"
                val c = countries[customers[o.customer]?.country]?.name ?: m.xtr("label_unknown_country")
                if (c !in ct) ct.add(c)
                val s = o.total ?: ZERO
                if (ordersAggr[p] == null) ordersAggr[p] = sortedMapOf()
                val acc = ordersAggr[p]?.get(c) ?: ZERO
                ordersAggr[p]?.set(c, acc + s)
            }
        }

        val data = ChartData<String, BigDecimal>()
        data.setLegendX(m.xtr("label_period"), "string")
        ct.sort()
        ct.forEachIndexed { i, c ->
            data.setLegendY(i, c, "number")
        }
        ordersAggr.forEach { (p, c) ->
            var y = arrayOf<BigDecimal>()
            ct.forEach { countryName ->
                y += c[countryName] ?: ZERO
            }
            data.add(p, *y)
        }

        return data
    }

    @Description("Prepares JSON for Percentage of sales by country chart")
    fun getChartSalesPercentageByCountry(): ChartData<*, *> {
        val customerFilter = Formula.parse("", Customer.T)
        //customerFilter.expectedRows = fetchSize
        val customers = m.selectMap(Customer.fId, customerFilter)

        val countryFilter = Formula.parse("", Country.T)
        //countryFilter.expectedRows = fetchSize
        val countries = m.selectMap(Country.fId, countryFilter)

        val ct = mutableListOf<String>()

        val shippingOrderFilter = Formula.parse("", Shipping_Order.T)
        //shippingOrderFilter.expectedRows = fetchSize

        val ordersAggr = sortedMapOf<String, MutableMap<String, BigDecimal>>()

        m.iterate<Shipping_Order>(shippingOrderFilter) { o ->
            val d = o.date_Order_Paid
            if (d != null) {
                val p = "${d.year} Q${d.get(IsoFields.QUARTER_OF_YEAR)}"
                val c = countries[customers[o.customer]?.country]?.name ?: m.xtr("label_unknown_country")
                if (c !in ct) ct.add(c)
                val s = o.total ?: ZERO
                if (ordersAggr[p] == null) ordersAggr[p] = sortedMapOf()
                val acc = ordersAggr[p]?.get(c) ?: ZERO
                ordersAggr[p]?.set(c, acc + s)
            }
        }

        val data = ChartData<String, BigDecimal>()
        data.setLegendX(m.xtr("label_period"), "string")
        ct.sort()
        ct.forEachIndexed { i, c ->
            data.setLegendY(i, c, "number")
        }

        ordersAggr.forEach { o ->
            val x = o.value
            var sum = BigDecimal("100").setScale(MAX_SCALE) // Distribute 100 percents proportionally
            var q = x.values.sumOf { it } // Sales total for this period
            val roundTo = BigDecimal("0.1")
            var y = arrayOf<BigDecimal>()
            ct.forEach { c ->
                if (q.signum() > 0) {
                    val v = x[c] ?: ZERO
                    val w = sum / q
                    val result = roundTo * (w * v / roundTo).setScale(0, RoundingMode.HALF_UP)
                    y += result
                    sum -= result
                    q -= v
                } else y += ZERO
            }
            data.add(o.key, *y)
        }

        return data
    }

}