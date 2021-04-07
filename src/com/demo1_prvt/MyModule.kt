package com.demo1_prvt

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
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.random.Random

class MyModule : Demo1_PrvtModuleBase() {

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
            Txt.info("${n}. Name = ${c.name}, Phone = ${c.phone}").msg()
            var m = 0
            iterate<Shipping_Order>("customer=${c.id}") { o ->
                m++
                Txt.info("$n.$m. Order #${o.id} placed ${o.datetime_Order_Placed?.toLocalDate()}").msg()
                var k = 0
                iterate<Shipping_Order_Product>("shipping_order=${o.id}") { item ->
                    k++
//                    val p = select(Product(), item.product)
                    val p = product[item.product]
                    Txt.info("$n.$m.$k. Product = ${p?.name}, qnty = ${item.quantity}").msg()
                }
            }
            if (m == 0) Txt.info("No orders for ${c.name}").msg()
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
        outFile.bufferedWriter().use { out ->
            val start = OffsetDateTime.now()
            out.write("Started at $start\n")
            val customerMap = selectMap(Customer.fId, customerFilter)
            val customer = customerMap.values.sortedBy { it.name }
            val orderFilter = "customer in (${customerMap.keys.take(150).joinToString()})"
            val orderMap = selectMap(Shipping_Order.fId, orderFilter)
            val order = orderMap.values.groupBy { it.customer }
            // max elements to use index search 153? or filter length?
            val orderProductFilter = "shipping_order in (${orderMap.keys.take(150).joinToString(",")})"
            val orderProductMap = selectMap(Shipping_Order_Product.fId, orderProductFilter)
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
        val maxPlacedDaysAgo = 365
        val maxPaidAfter = 30
        val maxShipmentAfter = 45
        val minItems = 3
        val maxItems = 10
        val maxQuantity = 15
        val minutesInDay = 3600
        val customer = selectMap(Customer.fId, "").values.toList()
        val maxCustomer = customer.size
        val product = selectMap(Product.fId, "").values.toList()
        val maxProduct = product.size
        for (i in 1..n) {
            val o = Shipping_Order()
            val placedDaysAgo = Random.nextInt(maxPlacedDaysAgo + 1).toLong()
            val paidDaysAgo = (placedDaysAgo - Random.nextInt(maxPaidAfter + 1)).coerceAtLeast(0)
            val shipmentDaysAgo = placedDaysAgo - Random.nextInt(maxShipmentAfter + 1).toLong()
            o.customer = customer[Random.nextInt(maxCustomer)].id
            o.datetime_Order_Placed =
                    OffsetDateTime.now().minusDays(placedDaysAgo).minusMinutes(Random.nextInt(minutesInDay).toLong())
            o.date_Order_Paid = LocalDate.now().minusDays(paidDaysAgo)
            o.shipment_Date = LocalDate.now().minusDays(shipmentDaysAgo)
            insert(o)
            val k = Random.nextInt(minItems, maxItems + 1)
            var total = BigDecimal.ZERO
            for (j in 1..k) {
                val item = Shipping_Order_Product()
                val p = product[Random.nextInt(maxProduct)]
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
        class ViewIterator(f: Formula, m: MyModule) : View1.Data(f, m) {
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

    companion object {
        init {
            T.registerFieldFiller(City.ICity::class.java, CityFiller::class.java)
        }

        fun start(): MyModule {
            return com.dolmen.serv.Module.start(MyModule::class.java)
        }
    }
}

