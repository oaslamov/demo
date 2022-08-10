package com.dlmdemo.demo1_prvt

import com.dolmen.md.demo1_prvt.*
import com.dolmen.serv.CONST.MAX_SCALE
import com.dolmen.serv.anno.Description
import com.dolmen.serv.anno.Parameters
import com.dolmen.ui.screen.ChartData
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
    fun getChartExample(isShowy2: Boolean?): String {
        val is2=isShowy2!=null && isShowy2
        val data = ChartData<String, Int>()
        data.setLegendX("year", "string")
        data.setLegendY(0, "west")
        data.setLegendY(1, "south")
        data.setLegendY(2, "north").alternativeAxis(is2)
        data.setLegendY(3, "east").alternativeAxis(is2)


        /*data.add("2016", 4001, 4200, 5200, 7000)
        data.add("2017", 5000, 5200, 6200, 6000)
        data.add("2018", 2500, 2700, 3700, 12000)
        data.add("2019", 1200, 1400, 2400, 19000)
        data.add("2020", 3365, 3565, 4500, 9000)
        data.add("2021", 4345, 4545, 5600, 19000)*/

        data.add("2016", 4001, 4200, 6000, 7000)
        data.add("2017", 5000, 5200, 5000, 6000)
        data.add("2018", 2500, 2700, 11000, 12000)
        data.add("2019", 1200, 1400, 12000, 19000)
        data.add("2020", 3365, 3565, 8000, 9000)
        data.add("2021", 4345, 4545, 12000, 19000)

        return data.toJson()
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
                .fold(ZERO) { acc, e -> acc + (e.total ?: ZERO) }
                .filterKeys { it.period != "-1" }
                .toSortedMap(compareBy<Group> { it.period }.thenBy { it.country })
        val ct = orders.map { it.key.country }.distinct().sorted()
        val c = Chart()
        c.legends.add(Legend(code = "x", name = "Period", type = "string"))
        c.legends.addAll(ct.map { Legend(it, it, "number") })
        c.data.addAll(orders.map { o ->
            mapOf("x" to o.key.period,
                    o.key.country to o.value.toString())
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
                .mapNotNull { o ->
                    val d = o.date_Order_Paid
                    if (d != null) {
                        val p = "${d.year} Q${d.get(IsoFields.QUARTER_OF_YEAR)}"
                        val c = countries[customers[o.customer]?.country]?.name ?: "unknown"
                        if (c !in ct) ct.add(c)
                        val s = o.total ?: ZERO
                        OrderData(period = p, country = c, sum = s)
                    } else null
                }
                .groupBy { it.period }
                .mapValues { it.value.groupingBy { it.country }.fold(ZERO) { acc, e -> acc + e.sum } }
                .toSortedMap(compareBy<String> { it })

        val c = Chart()
        c.legends.add(Legend(code = "x", name = "Period", type = "string"))
        c.legends.addAll(ct.sorted().map { Legend(it, it, "number") })

        ordersAggr.forEach { o ->
            val x = o.value.toList().sortedBy { (_, value) -> value }.toMap()
            var sum = BigDecimal("100").setScale(MAX_SCALE) // Distribute 100 percents proportionally
            var q = x.values.sumOf { it } // Sales total for this period
            val roundTo = BigDecimal("0.1")
            x.forEach { p ->
                val v = p.value
                val w = sum / q
                val result = roundTo * (w * v / roundTo).setScale(0, RoundingMode.HALF_UP)
                c.data.add(mapOf("x" to o.key, p.key to result.toString()))
                sum -= result
                q -= v
            }
        }
        return c.getJSON()
    }
}