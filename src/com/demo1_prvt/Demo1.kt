package com.demo1_prvt

import com.dolmen.call.ActionBase
import com.dolmen.call.Http
import com.dolmen.call.JSONManagerBase
import com.dolmen.md.demo1_prvt.*
import com.dolmen.mod.GuiModule
import com.dolmen.serv.CONST.MAX_STRING_CHARS
import com.dolmen.serv.Txt
import com.dolmen.serv.anno.ActionType
import com.dolmen.serv.anno.Description
import com.dolmen.serv.anno.Parameters
import com.dolmen.serv.conn.SelectedData
import com.dolmen.serv.exp.Formula
import com.dolmen.serv.table.ITopTable
import com.dolmen.serv.table.RowID
import com.dolmen.util.Text
import org.mpru.security.KerberosPrefs
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.IsoFields
import kotlin.random.Random


class Demo1 : Demo1_PrvtModuleBase() {

    @Description("My Action")
    @Parameters("input: String")
    fun myAction(input: String): String {
        return Text.F("\n-------------\nTest OK (input: $0)\n--------------", input)
    }

    @Description("Shows customers' orders summary")
    @Parameters("customerFilter: String")
    fun action1(customerFilter: String) {
        val product = selectMap(Product.fId, "")
        var n = 0
        iterate<Customer>(customerFilter) { c ->
            n++
            Txt.info(xtr("act1_c_header", n, c.name, c.phone)).msg()
            var m = 0
            iterate<Shipping_Order>("customer=${c.id}") { o ->
                m++
                Txt.info(xtr("act1_o_header", n, m, o.id, o.datetime_Order_Placed?.toLocalDate())).msg()
                var k = 0
                iterate<Shipping_Order_Product>("shipping_order=${o.id}") { item ->
                    k++
//                    val p = select(Product(), item.product)
                    val p = product[item.product]
                    Txt.info(xtr("act1_p_header", n, m, k, p?.name, item.quantity)).msg()
                }
            }
            if (m == 0) Txt.info(xtr("act1_no_orders", c.name)).msg()
        }
    }


    @Description("Creates a customer")
    @Parameters("name: String", "phone: String", "mobile: String")
    fun action2(name: String, phone: String, mobile: String) {
        val c = Customer()
        c.name = name
        c.phone = phone
        c.mobile = mobile
        insert(c)
        Txt.info("Created: ${c.name}, ph. = ${c.phone}, mob. = ${c.mobile}").msg()
    }

    @Description("Searches, updates and deletes")
    @Parameters("customerId: RowID", "productSubstring: String")
    fun action3(customerId: RowID, productSubstring: String) {
        val c = select(Customer(), customerId)
        val cName = c.name
        c.name = cName?.toUpperCase()
        update(c)
        Txt.info("Customer #${customerId}. Changed name from '${cName}' to '${c.name}'").msg()
        val p = selectFirst<Product>("name like '%${productSubstring}%'")
        if (p != null) {
            val pName = p.name
            p.name = pName?.toLowerCase()
            update(p)
            Txt.info("Changed product name from '${pName}' to '${p.name}'").msg()
        }
        val o = selectFirst<Shipping_Order>("customer = ${customerId}")
        if (o != null) {
            Txt.info("Deleting Order #${o.id} placed ${o.datetime_Order_Placed?.toLocalDate()}").msg()
            delete(o)
        } else {
            Txt.info("No orders found for customer id = ${customerId}").msg()
        }
    }

    @Description("Deletes list")
    @Parameters("customerId: RowId")
    fun action4(customerId: RowID) {
        Txt.info("Deleting orders for customer ID = ${customerId}").msg()
        deleteList("demo1_prvt.shipping_order", "customer=${customerId}")
    }

    @Description("Shows customers' orders summary - selectMap() version")
    @Parameters("customerFilter: String")
    fun action91(customerFilter: String) {
        val outFile = File("C:/dolmen/tmp/demo1.txt")
        outFile.bufferedWriter().use { out ->
            val start = OffsetDateTime.now()
            out.write("Started at $start\n")
            val customer = selectMap(Customer.fId, customerFilter).values.sortedBy { it.name }
            val order = selectMap(Shipping_Order.fId, "").values.groupBy { it.customer }
            val orderProduct = selectMap(Shipping_Order_Product.fId, "").values.groupBy { it.shipping_Order }
            val product = selectMap(Product.fId, "")
            var finish = OffsetDateTime.now()
            out.write("Query finished at $finish\n")
            out.write("Runtime ${Duration.between(start, finish)}\n")
            var n = 0
            customer.forEach { c ->
                n++
                out.write("${n}. Name = ${c.name}, Phone = ${c.phone}\n")
                var m = 0
                order[c.id]?.sortedBy { it.datetime_Order_Placed }?.forEach { o ->
                    m++
                    out.write("$n.$m. Order #${o.id} placed ${o.datetime_Order_Placed?.toLocalDate()}\n")
                    var k = 0
                    orderProduct[o.id]?.sortedBy { product[it.product]?.name }?.forEach { item ->
                        k++
                        val p = product[item.product]
                        out.write("$n.$m.$k. Product = ${p?.name}, qnty = ${item.quantity}\n")
                    }
                }
                if (m == 0) out.write("No orders for ${c.name}\n")
            }
            finish = OffsetDateTime.now()
            out.write("Finished at $finish\n")
            out.write("Runtime ${Duration.between(start, finish)}\n")
        }
    }

    @Description("Shows customers' orders summary - selectMap() version")
    @Parameters("customerFilter: String")
    fun action92(customerFilter: String) {
        val outFile = File("C:/dolmen/tmp/demo1.txt")
        outFile.bufferedWriter().use { out ->
            val start = OffsetDateTime.now()
            out.write("Started at $start\n")
            //val customer = selectMap(Customer.fId, customerFilter).values.sortedBy { it.name }
            val product = selectMap(Product.fId, "")
            var finish = OffsetDateTime.now()
            out.write("Query finished at $finish\n")
            out.write("Runtime ${Duration.between(start, finish)}\n")
            var n = 0
            //customer.forEach { c ->
            iterate<Customer>(customerFilter) { c ->
                n++
                out.write("${n}. Name = ${c.name}, Phone = ${c.phone}\n")
                var m = 0
                //val order = selectMap(Shipping_Order.fId, "customer=${c.id}").values
                //order.sortedBy { it.datetime_Order_Placed }.forEach { o ->
                iterate<Shipping_Order>("customer=${c.id}") { o ->
                    m++
                    out.write("$n.$m. Order #${o.id} placed ${o.datetime_Order_Placed?.toLocalDate()}\n")
                    var k = 0
                    //val orderProduct = selectMap(Shipping_Order_Product.fId, "shipping_order=${o.id}").values
                    //orderProduct.sortedBy { product[it.product]?.name }.forEach { item ->
                    iterate<Shipping_Order_Product>("shipping_order=${o.id}") { item ->
                        k++
                        val p = product[item.product]
                        out.write("$n.$m.$k. Product = ${p?.name}, qnty = ${item.quantity}\n")
                    }
                }
                if (m == 0) out.write("No orders for ${c.name}\n")
            }
            finish = OffsetDateTime.now()
            out.write("Finished at $finish\n")
            out.write("Runtime ${Duration.between(start, finish)}\n")
        }
    }

    @Description("Shows customers' orders summary - selectMap() version")
    @Parameters("customerFilter: String")
    fun action93(customerFilter: String) {
        val outFile = File("C:/dolmen/tmp/demo1.txt")
        val queryChunkSize = 150
        outFile.bufferedWriter().use { out ->
            val start = OffsetDateTime.now()
            out.write("Started at $start\n")
            val customerMap = selectMap(Customer.fId, customerFilter)
            val customer = customerMap.values.sortedBy { it.name }
            val customerChunks = customerMap.keys.chunked(queryChunkSize)
            var orderMap = mapOf<RowID, Shipping_Order>()
            run {//forEach
                val chunk = customerChunks[0]
                val orderFilter = "customer in (${chunk.joinToString(",")})"
                orderMap = orderMap + selectMap(Shipping_Order.fId, orderFilter)
            }
            val order = orderMap.values.groupBy { it.customer }
            // max elements to use index search 153? or filter length?
            val orderChunks = orderMap.keys.chunked(queryChunkSize)
            var orderProductMap = mapOf<RowID, Shipping_Order_Product>()
            run {//forEach
                val chunk = orderChunks[0]
                val orderProductFilter = "shipping_order in (${chunk.joinToString(",")})"
                orderProductMap = orderProductMap + selectMap(Shipping_Order_Product.fId, orderProductFilter)
            }
            val orderProduct = orderProductMap.values.groupBy { it.shipping_Order }
            val product = selectMap(Product.fId, "")
            var finish = OffsetDateTime.now()
            out.write("Query finished at $finish\n")
            out.write("Runtime ${Duration.between(start, finish)}\n")
            var n = 0
            customer.forEach { c ->
                n++
                out.write("${n}. Name = ${c.name}, Phone = ${c.phone}\n")
                var m = 0
                order[c.id]?.sortedBy { it.datetime_Order_Placed }?.forEach { o ->
                    m++
                    out.write("$n.$m. Order #${o.id} placed ${o.datetime_Order_Placed?.toLocalDate()}\n")
                    var k = 0
                    orderProduct[o.id]?.sortedBy { product[it.product]?.name }?.forEach { item ->
                        k++
                        val p = product[item.product]
                        out.write("$n.$m.$k. Product = ${p?.name}, qnty = ${item.quantity}\n")
                    }
                }
                if (m == 0) out.write("No orders for ${c.name}\n")
            }
            finish = OffsetDateTime.now()
            out.write("Finished at $finish\n")
            out.write("Runtime ${Duration.between(start, finish)}\n")
        }
    }


    @Description("Imports products from file")
    @Parameters("pathIn: example file in {project}/dataset/grocery.csv", "n: Int")
    fun importProducts(pathIn: String, n: Int): String {
        val fileIn = File(pathIn)
        fileIn.useLines { lines ->
            var i = 1
            for (l in lines) {
                val p = Product()
                p.name = l.trim()
                p.price = ((Random.nextInt(3000) + 1) / 100.0).toBigDecimal().setScale(2, RoundingMode.HALF_UP)
                p.product_Type = Product.PRODUCT_TYPE.GROCERY
                insert(p)
                if (i == n) break
                i++
            }
        }
        return Text.F("Done")
    }

    @Description("Imports customers from file")
    @Parameters("pathIn: example file in  {project}/dataset/customer.csv ", "n: Int")
    fun importCustomers(pathIn: String, n: Int): String {
        val fileIn = File(pathIn)
        val countries = listOf(
                selectFirst<Country>("name='Australia'")?.id,
                selectFirst<Country>("name='Canada'")?.id,
                selectFirst<Country>("name='United Kingdom'")?.id,
                selectFirst<Country>("name='United States'")?.id
        )
        fileIn.useLines { lines ->
            var i = 1
            for (l in lines) {
                val rec = l.split(",").toTypedArray()
                val c = Customer()
                c.name = "${rec[1]}, ${rec[0]}"
                c.phone = rec[6]
                c.mobile = rec[7]
                c.address_Line1 = rec[2]
                c.address_Line2 = "${rec[3]}, ${rec[4]}"
                c.address_Line3 = rec[5]
                c.country = countries[(i - 1) / 500]
                insert(c)
                if (i == n) break
                i++
            }
        }
        return Text.F("Done")
    }

    @Description("Generates random orders")
    @Parameters("n: Int")
    fun genOrders(n: Int): String {
        val rnd = java.util.Random()
        val maxPlacedDaysAgo = 365 * 3
        val maxPaidAfter = 30
        val maxShipmentAfter = 45
        val minItems = 3
        val maxItems = 10
        val maxQuantity = 15
        val minutesInDay = 3600
        val customer = selectMap(Customer.fId, "").values.toList().shuffled()
        val maxCustomer = customer.size
        val product = selectMap(Product.fId, "").values.toList()
        val maxProduct = product.size
        for (i in 1..n) {
            val o = Shipping_Order()
            val placedDaysAgo = Random.nextInt(maxPlacedDaysAgo + 1).toLong()
            val paidDaysAgo = (placedDaysAgo - Random.nextInt(maxPaidAfter + 1)).coerceAtLeast(0)
            val shipmentDaysAgo = placedDaysAgo - Random.nextInt(maxShipmentAfter + 1).toLong()
            //val m1 = Random.nextInt(maxCustomer)
            val m1 = ((0.15 * rnd.nextGaussian() + 0.5) * maxCustomer).toInt().coerceIn(0, maxCustomer - 1)
            o.customer = customer[m1].id
            o.datetime_Order_Placed =
                    OffsetDateTime.now().minusDays(placedDaysAgo).minusMinutes(Random.nextInt(minutesInDay).toLong())
            o.date_Order_Paid = LocalDate.now().minusDays(paidDaysAgo)
            o.shipment_Date = LocalDate.now().minusDays(shipmentDaysAgo)
            insert(o)
            val k = Random.nextInt(minItems, maxItems + 1)
            var total = BigDecimal.ZERO
            for (j in 1..k) {
                val item = Shipping_Order_Product()
                //val m2 = Random.nextInt(maxProduct)
                val m2 = ((0.15 * rnd.nextGaussian() + 0.5) * maxProduct).toInt().coerceIn(0, maxProduct - 1)
                val p = product[m2] // Normal distribution (for ABC analysis graph)c
                item.shipping_Order = o.id
                item.product = p.id
                item.quantity = Random.nextInt(maxQuantity) + 1
                val pr = p.price ?: BigDecimal.ZERO
                val s = item.quantity.toBigDecimal() * pr
                item.price = pr
                item.sum = s
                insert(item)
                total += s
            }
            if (total != BigDecimal.ZERO) {
                o.total = total
                update(o)
            }
            if (i % 100 == 0) Txt.info("Generated $i orders").msg()
        }
        return Text.F("Done")
    }

    @Description("Updates all orders sums")
    fun updateAllOrders() {
        val product = selectMap(Product.fId, "")
        var i = 0
        iterate<Shipping_Order>("") { o ->
            i++
            var total = BigDecimal.ZERO
            iterate<Shipping_Order_Product>("shipping_order=${o.id}") { item ->
                val p = product[item.product]
                if (p != null) {
                    val itemPrice = p.price ?: BigDecimal.ZERO
                    item.price = itemPrice
                    item.sum = (itemPrice * item.quantity.toBigDecimal())
                    update(item)
                }
                total += item.sum ?: BigDecimal.ZERO
            }
            o.total = total
            update(o)
            if (i % 100 == 0) Txt.info("${i}. Updated order # ${o.id}, total = ${o.total}").msg()
        }
    }

    @Description("Imports cities from file")
    @Parameters("pathIn: example file in {project}/dataset/world-cities_csv.txt")
    fun importCities(pathIn: String): String {
        val fileIn = File(pathIn)
        var i = 0
        fileIn.useLines { lines ->
            for (l in lines) {
                if (i != 0) {
                    val rec = l.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).toTypedArray()
                    val city = rec[0].replace("\"", "").take(MAX_STRING_CHARS)
                    val country = rec[1].replace("\"", "").take(MAX_STRING_CHARS)
                    val subcountry = rec[2].replace("\"", "").take(MAX_STRING_CHARS)
                    val geonameid = rec[3].take(MAX_STRING_CHARS)
                    //Txt.info("$city | $country | $subcountry | $geonameid").msg()

                    if (subcountry == "İzmir") continue // bug: selectFirst cannot find İzmir

                    var cntr = selectFirst<Country>(
                            "name = \"${country}\"")
                    if (cntr == null) {
                        cntr = Country()
                        cntr.name = country
                        insert(cntr)
                    }
                    var sbcntr = selectFirst<Subcountry>(
                            "country_id = ${cntr.id} and name = \"${subcountry}\"")
                    if (sbcntr == null) {
                        sbcntr = Subcountry()
                        sbcntr.country_Id = cntr.id
                        sbcntr.name = subcountry
                        insert(sbcntr)
                    }
                    var ct = selectFirst<City>(
                            "country_id = ${cntr.id} AND subcountry_id = ${sbcntr.id} AND name = \"${city}\"")
                    if (ct == null) {
                        ct = City()
                        ct.country_Id = cntr.id
                        ct.subcountry_Id = sbcntr.id
                        ct.name = city
                        ct.geonameid = geonameid
                        insert(ct)
                    }
                }
                i++
            }
        }
        Txt.info("Processed ${i} lines from ${pathIn}").msg()
        return Text.F("Done")
    }

    @Description("Performs complex order item search")
    @Parameters("itemFilter", "productFilter", "orderFilter", "customerFilter")
    fun search(itemFilter: String?, productFilter: String?, orderFilter: String?,
               customerFilter: String?): List<ITopTable> {
        val count = 1000
        var item = selectMap(Shipping_Order_Product.fId, itemFilter)
        val product = selectMap(Product.fId, productFilter)
        var order = selectMap(Shipping_Order.fId, orderFilter)
        val customer = selectMap(Customer.fId, customerFilter)

        order = order.filterValues { it.customer in customer }
        item = item.filterValues { (it.product in product) and (it.shipping_Order in order) }
        val resL = item.values.take(count).toList()
        return resL
    }

    override fun s_iterateView1(f: Formula): SelectedData<View1> {
        class ViewIterator(f: Formula, m: Demo1) : View1.Data(f, m) {
            val customers: Map<RowID, Customer> = selectMap(Customer.fId, "")
            override fun create(s: Shipping_Order): View1 {
                val v = super.create(s)
                if (s.customer != null) {
//                    val c = selectFirst<Customer>("id=${s.customer}")
                    val c = customers[s.customer]
                    if (c != null) {
                        v.c_Name = c.name
                        v.c_Phone = c.phone
                        v.c_Mobile = c.mobile
                        v.c_Address = listOfNotNull(c.address_Line1, c.address_Line2, c.address_Line3).joinToString()
                    }
                }
                return v
            }
        }
        return ViewIterator(f, this)
    }

    @Description("Calculates sales statistics for a given period of time")
    @Parameters("start", "finish", "abLimit: AB threshhold default(65)", "bcLimit: BC threshhold default(90)")
    fun makeStats(start: LocalDate?, finish: LocalDate?, abLimit: Int, bcLimit: Int) {
        return Stats(this).makeStats(start, finish, abLimit, bcLimit)
    }

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
        c.legends.add(Legend("y1", xtr("label_p_revenue"), "number"))
        c.legends.add(Legend("y2", xtr("label_c_revenue"), "number"))
        c.legends.add(Legend("y3", xtr("label_p_threshold", "AB"), "number", "#91a3b2"))
        c.legends.add(Legend("y4", xtr("label_p_threshold", "BC"), "number", "#b9c2ca"))

        val products = selectMap(Product_Abc.fId, "").values.sortedByDescending { it.sum }
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

        val customers = selectMap(Customer_Abc.fId, "").values.sortedByDescending { it.sum }
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
                    y = count(Shipping_Order::class, "total<${limits[0]}").toString()
                }
                i < limitsSize -> {
                    x = "${limits[i - 1]}.00-${limits[i]}.00"
                    y = count(Shipping_Order::class, "total>=${limits[i - 1]} and total<${limits[i]}").toString()
                }
                else -> {
                    x = ">${limits.last()}.00"
                    y = count(Shipping_Order::class, "total>=${limits.last()}").toString()
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

        val customers = selectMap(Customer.fId, "")
        val countries = selectMap(Country.fId, "")
        val orders = selectMap(Shipping_Order.fId, "").values
                .groupingBy { o ->
                    val d = o.date_Order_Paid
                    val c = countries[customers[o.customer]?.country]?.name ?: "unknown"
                    if (d != null)
                        Group("${d.year} Q${d.get(IsoFields.QUARTER_OF_YEAR)}", c)
                    else Group("-1", c)
                }
                .fold(Accum(count = 0, sum = BigDecimal.ZERO)) { acc, e ->
                    Accum(acc.count + 1, acc.sum + (e.total ?: BigDecimal.ZERO))
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
        data class Accum(val count: Int, val sum: BigDecimal)
        data class Group(val period: String, val country: String)

        val customers = selectMap(Customer.fId, "")
        val countries = selectMap(Country.fId, "")
        val orders = selectMap(Shipping_Order.fId, "").values
                .groupingBy { o ->
                    val d = o.date_Order_Paid
                    val c = countries[customers[o.customer]?.country]?.name ?: "unknown"
                    if (d != null)
                        Group("${d.year} Q${d.get(IsoFields.QUARTER_OF_YEAR)}", c)
                    else Group("-1", c)
                }
                .fold(Accum(count = 0, sum = BigDecimal.ZERO)) { acc, e ->
                    Accum(acc.count + 1, acc.sum + (e.total ?: BigDecimal.ZERO))
                }
                .filterKeys { it.period != "-1" }
                .toSortedMap(compareBy<Group> { it.period }.thenBy { it.country })
        val periodSums = orders.toList()
                .groupingBy { it.first.period }
                .fold(Accum(count = 0, sum = BigDecimal.ZERO)) { acc, e ->
                    Accum(acc.count + 1, acc.sum + e.second.sum)
                }
        val ct = orders.map { it.key.country }.distinct()
        val c = Chart()
        c.legends.add(Legend(code = "x", name = "Period", type = "string"))
        c.legends.addAll(ct.map { Legend(it, it, "number") })
        c.data.addAll(orders.map { o ->
            mapOf("x" to o.key.period,
                    o.key.country to (BigDecimal(100) * o.value.sum / periodSums[o.key.period]?.sum!!).toString())
        })
        return c.getJSON()
    }


    override fun beforeUpdate(t: ITopTable?) {
        super.beforeUpdate(t)
        if (t is Customer) {
            if (t.city != null) {
                val ct = select(City(), t.city)
                if (ct != null) {
                    t.country = ct.country_Id
                    t.subcountry = ct.subcountry_Id
                    t.city = ct.id
                }
            }
        }
    }

    @Description("Inserts a new record and navigates to the specified screen")
    @Parameters("tableCode: table code", "screenCode: screen code", "mode: screen opening mode",
            "linkID: id code (null = 'id')", "fields: optional map of table field values (default null)")
    @ActionType("insert")
    fun insertAndGo(tableCode: String, screenCode: String, mode: String?, linkID: String?,
                    fields: Map<String, Any>?): ITopTable {
        val table = newTable(tableCode, fields) as ITopTable
        insert(table)
        val args = HashMap<String, Any>()
        if (linkID == null) args["id"] = table.id else args[linkID] = table.id
        GuiModule.goScreen(screenCode, args, mode)
        return table
    }

    @Description("Calls dolmen server (JSON)")
    fun callDolmenJson(): String {
        var res = ""
        val url = "https://dolmensystem.corp.example.com/dolmen"
        val spn = url.replace(Regex(".*//(.*)/.*"), "HTTP/$1")
        val http = Http(url)
        val kerbPrefs = KerberosPrefs()
        //kerbPrefs.setUsername("dora@CORP.EXAMPLE.COM")
        //kerbPrefs.setPassword("Pass123456")
        kerbPrefs.setPrincipal("HTTP/dlm2.corp.example.com@CORP.EXAMPLE.COM")
        kerbPrefs.setKtab("C:/dolmen/Workspace/webserver/webapps/dolmen/dolmen.ktab")
        kerbPrefs.setSpn(spn)
        http.setKerberosClient(kerbPrefs)
        http.setLog(this.l)
        val ac = ActionBase.create("demo1_prvt.selectlist", "demo1_prvt.customer",
                "name like '%val%' order by name").setTag("myTag1")
        var ar = http.action(ac)
        while (ar != null) {
            res += JSONManagerBase.getJson(ar, false) + '\n'
            ar = ar.next
        }
        res += "\nRC == ${http.rc()}"
        return res
    }

    @Description("Calls dolmen server (XML)")
    fun callDolmenXml(): String {
        val url = "https://dolmensystem.corp.example.com/dolmen"
        val http = Http(url)
        val spn = url.replace(Regex(".*//(.*)/.*"), "HTTP/$1")
        val kerbPrefs = KerberosPrefs()
        //kerbPrefs.setUsername("dora@CORP.EXAMPLE.COM")
        //kerbPrefs.setPassword("Pass123456")
        kerbPrefs.setPrincipal("HTTP/dlm2.corp.example.com@CORP.EXAMPLE.COM")
        kerbPrefs.setKtab("C:/dolmen/Workspace/webserver/webapps/dolmen/dolmen.ktab")
        kerbPrefs.setSpn(spn)
        http.setKerberosClient(kerbPrefs)
        http.setLog(this.l)
        var res = http.sendPost("""
            <?xml version="1.0" encoding="UTF-8"?>
            <dolmen version="1">
            <a a="demo1_prvt.action1">
                <arg name="customerFilter">name like '%val%'</arg>
            </a>
            </dolmen>
        """.trimIndent())
        res += "\nRC == ${http.rc()}\n"
        return res
    }


    companion object {
        init {
            T.registerFieldFiller(City.ICity::class.java, CityFiller::class.java)
        }

        fun start(): Demo1 {
            return com.dolmen.serv.Module.start(Demo1::class.java)
        }
    }
}

