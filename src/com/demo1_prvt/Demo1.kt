package com.demo1_prvt

import com.demo1_prvt.filler.CityFiller
import com.dolmen.md.demo1_prvt.*
import com.dolmen.mod.GuiModule
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
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime


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
        val o = selectFirst<Shipping_Order>("customer = $customerId")
        if (o != null) {
            Txt.info("Deleting Order #${o.id} placed ${o.datetime_Order_Placed?.toLocalDate()}").msg()
            delete(o)
        } else {
            Txt.info("No orders found for customer id = $customerId").msg()
        }
    }

    @Description("Deletes list")
    @Parameters("customerId: RowId")
    fun action4(customerId: RowID) {
        Txt.info("Deleting orders for customer ID = $customerId").msg()
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

    @Description("Updates all orders sums")
    fun updateAllOrders() {
        Populate(this).updateAllOrders()
    }

    @Description("Loads sample data")
    fun loadSampleData() {
        return Populate(this).loadSampleData()
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
        return item.values.take(count).toList()
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
        return ChartManager(this).getChartExample()
    }

    @Description("Prepares JSON for ABC analysis graph")
    fun getChartABC(): String {
        return ChartManager(this).getChartABC()
    }

    @Description("Prepares JSON for Order totals chart")
    @Parameters("points: Groups limits")
    fun getChartOrderTotals(points: String): String {
        return ChartManager(this).getChartOrderTotals(points)
    }

    @Description("Prepares JSON for Sales by country chart")
    fun getChartSalesByCountry(): String {
        return ChartManager(this).getChartSalesByCountry()
    }

    @Description("Prepares JSON for Percentage of sales by country chart")
    fun getChartSalesPercentageByCountry(): String {
        return ChartManager(this).getChartSalesPercentageByCountry()
    }

    override fun beforeUpdate(t: ITopTable) {
        Operations(this).triggerBeforeUpdate(t)
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
        return Caller(this).callDolmenJson()
    }

    @Description("Calls dolmen server (XML)")
    fun callDolmenXml(): String {
        return Caller(this).callDolmenXml()
    }

    override fun x_installed(modulePreviousVersionId: Int) {
        super.x_installed(modulePreviousVersionId)
        Populate(this).loadSampleData()
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

