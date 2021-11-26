package com.dlmdemo.demo1_prvt

import com.dolmen.md.demo1_prvt.*
import com.dolmen.serv.CONST.MAX_SCALE
import com.dolmen.serv.anno.Description
import com.dolmen.serv.anno.Parameters
import com.dolmen.util.JSONManager
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.RoundingMode
import java.time.temporal.IsoFields

data class Legend(val code: String, val name: String, val type: String, val color: String? = null)
class Chart {
    val legends: MutableList<Legend> = mutableListOf()
    val data: MutableList<Map<String, String>> = mutableListOf()
    fun getJSON(): String = JSONManager.getJson(mapOf("legends" to legends, "data" to data), true)
}

class ChartManager(val m: Demo1) {
    @Description("Prepares JSON for charts example")
    fun getChartExample(): String {
        val c = Chart()
        c.legends.addAll(listOf(
                Legend("x", "year", "string"),
                Legend("y1", "west", "number"),
                Legend("y2", "east", "number")
        ))
        c.data.addAll(listOf(
                mapOf("x" to "2016", "y1" to "4000", "y2" to "800"),
                mapOf("x" to "2017", "y1" to "5000", "y2" to "700"),
                mapOf("x" to "2018", "y1" to "2500", "y2" to "1300"),
                mapOf("x" to "2019", "y1" to "1200", "y2" to "2000"),
                mapOf("x" to "2020", "y1" to "3365", "y2" to "1000"),
                mapOf("x" to "2021", "y1" to "4345", "y2" to "2000"),
        ))
        return c.getJSON()
    }

    @Description("Prepares JSON for ABC analysis graph")
    fun getChartABC(): String {
        val c = Chart()
        c.legends.add(Legend(code = "x", name = "% items", type = "number"))
        c.legends.add(Legend("y1", m.xtr("label_p_revenue"), "number"))
        c.legends.add(Legend("y2", m.xtr("label_c_revenue"), "number"))
        c.legends.add(Legend("y3", m.xtr("label_p_threshold", "AB"), "number", "#91a3b2"))
        c.legends.add(Legend("y4", m.xtr("label_p_threshold", "BC"), "number", "#b9c2ca"))

        val products = m.selectMap(Product_Abc.fId, "").values.sortedByDescending { it.sum }
        val maxProduct = products.size
        c.data.add(mapOf("x" to "0", "y1" to "0"))
        var class0 = "A"
        var class1: String
        products.forEachIndexed { i, p ->
            val x = (i + 1).toFloat() / maxProduct * 100
            val y = p.cuperc
            c.data.add(mapOf("x" to x.toString(), "y1" to y.toString()))
            class1 = p.abc_Class.toString()
            if ((class0 == "A") and (class1 == "B")) {
                c.data.add(mapOf("x" to "0", "y3" to y.toString()))
                c.data.add(mapOf("x" to x.toString(), "y3" to y.toString()))
                c.data.add(mapOf("x" to x.toString(), "y3" to "0"))
            }
            if ((class0 == "B") and (class1 == "C")) {
                c.data.add(mapOf("x" to "0", "y4" to y.toString()))
                c.data.add(mapOf("x" to x.toString(), "y4" to y.toString()))
                c.data.add(mapOf("x" to x.toString(), "y4" to "0"))
            }
            class0 = class1
        }

        val customers = m.selectMap(Customer_Abc.fId, "").values.sortedByDescending { it.sum }
        val maxCustomer = customers.size
        c.data.add(mapOf("x" to "0", "y2" to "0"))
        customers.forEachIndexed { i, p ->
            val x = (i + 1).toFloat() / maxCustomer * 100
            val y = p.cuperc
            c.data.add(mapOf("x" to x.toString(), "y2" to y.toString()))
        }
        return c.getJSON()
    }

    @Description("Prepares JSON for Order totals chart")
    @Parameters("points: Groups limits")
    fun getChartOrderTotals(points: String): String {
        if (points.isBlank()) return ""
        val limits = points.split(",").map { it.trim().toInt() }.distinct().sorted()
        val limitsSize = limits.size
        if (limitsSize == 0) return ""

        val c = Chart()
        c.legends.add(Legend(code = "x", name = "Order total", type = "string"))
        c.legends.add(Legend("y1", "Count", "number"))
        for (i in 0..limitsSize) {
            var x: String
            var y: String
            when {
                i == 0 -> {
                    x = "<${limits[0]}.00"
                    y = m.count(Shipping_Order::class, "total<${limits[0]}").toString()
                }
                i < limitsSize -> {
                    x = "${limits[i - 1]}.00-${limits[i]}.00"
                    y = m.count(Shipping_Order::class, "total>=${limits[i - 1]} and total<${limits[i]}").toString()
                }
                else -> {
                    x = ">${limits.last()}.00"
                    y = m.count(Shipping_Order::class, "total>=${limits.last()}").toString()
                }
            }
            c.data.add(mapOf("x" to x, "y1" to y))
        }
        return c.getJSON()
    }

    @Description("Prepares JSON for Sales by country chart")
    fun getChartSalesByCountry(): String {
        data class Accum(val count: Int, val sum: BigDecimal)
        data class Group(val period: String, val country: String)

        val customers = m.selectMap(Customer.fId, "")
        val countries = m.selectMap(Country.fId, "")
        val orders = m.selectMap(Shipping_Order.fId, "").values
                .groupingBy { o ->
                    val d = o.date_Order_Paid
                    val c = countries[customers[o.customer]?.country]?.name ?: "unknown"
                    if (d != null)
                        Group("${d.year} Q${d.get(IsoFields.QUARTER_OF_YEAR)}", c)
                    else Group("-1", c)
                }
                .fold(Accum(count = 0, sum = ZERO)) { acc, e ->
                    Accum(acc.count + 1, acc.sum + (e.total ?: ZERO))
                }
                .filterKeys { it.period != "-1" }
                .toSortedMap(compareBy<Group> { it.period }.thenBy { it.country })
        val ct = orders.map { it.key.country }.distinct()
        val c = Chart()
        c.legends.add(Legend(code = "x", name = "Period", type = "string"))
        c.legends.addAll(ct.map { Legend(it, it, "number") })
        c.data.addAll(orders.map { o ->
            mapOf("x" to o.key.period,
                    o.key.country to o.value.sum.toString())
        })
        return c.getJSON()
    }

    @Description("Prepares JSON for Percentage of sales by country chart")
    fun getChartSalesPercentageByCountry(): String {
        data class OrderData(val period: String, val country: String, val sum: BigDecimal)

        val customers = m.selectMap(Customer.fId, "")
        val countries = m.selectMap(Country.fId, "")
        val ct = mutableListOf<String>()
        val ordersAggr = m.selectMap(Shipping_Order.fId, "").values
                .map { o ->
                    val d = o.date_Order_Paid
                    val p = if (d != null) "${d.year} Q${d.get(IsoFields.QUARTER_OF_YEAR)}" else "-1"
                    val c = countries[customers[o.customer]?.country]?.name ?: "unknown"
                    if (c !in ct && d!=null) ct.add(c)
                    val s = o.total ?: ZERO
                    OrderData(period = p, country = c, sum = s)
                }
                .filter { it.period != "-1" }
                .groupBy { it.period }
                .mapValues {
                    it.value.groupingBy { it.country }.fold(ZERO) { acc, e -> acc + e.sum }
                }
                .toSortedMap(compareBy<String> { it })

        val c = Chart()
        c.legends.add(Legend(code = "x", name = "Period", type = "string"))
        c.legends.addAll(ct.sorted().map { Legend(it, it, "number") })

        ordersAggr.forEach { o ->
            val x = o.value.toList().sortedBy { (_, value) -> value }.toMap()
            var sum = BigDecimal("100").setScale(MAX_SCALE) // Distribute 100 percents proportionally
            var q = x.values.sumOf { it } // Sales total for this period
            val roundTo = BigDecimal("0.2").setScale(MAX_SCALE)
            x.forEach { p ->
                val v = p.value
                val w = sum / q
                val result = roundTo * (w * v / roundTo).setScale(0, RoundingMode.HALF_UP)
                c.data.add(mapOf(
                        "x" to o.key,
                        p.key to result.toString()))
                sum -= result
                q -= v
            }
        }
        return c.getJSON()
    }
}