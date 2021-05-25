package com.demo1_prvt

import com.dolmen.md.demo1_prvt.*
import com.dolmen.serv.Txt
import com.dolmen.serv.anno.Description
import com.dolmen.serv.anno.Parameters
import com.dolmen.serv.table.ITopTable
import com.dolmen.serv.table.RowID
import java.io.File
import java.time.Duration
import java.time.OffsetDateTime

class CustomActions(val m: Demo1) {

    @Description("Shows customers' orders summary")
    @Parameters("customerFilter: String")
    fun action1(customerFilter: String) {
        val product = m.selectMap(Product.fId, "")
        var n0 = 0
        m.iterate<Customer>(customerFilter) { c ->
            n0++
            Txt.info(m.xtr("act1_c_header", n0, c.name, c.phone)).msg()
            var n1 = 0
            m.iterate<Shipping_Order>("customer=${c.id}") { o ->
                n1++
                Txt.info(m.xtr("act1_o_header", n0, n1, o.id, o.datetime_Order_Placed?.toLocalDate())).msg()
                var n2 = 0
                m.iterate<Shipping_Order_Product>("shipping_order=${o.id}") { item ->
                    n2++
//                    val p = m.select(Product(), item.product)
                    val p = product[item.product]
                    Txt.info(m.xtr("act1_p_header", n0, n1, n2, p?.name, item.quantity)).msg()
                }
            }
            if (n1 == 0) Txt.info(m.xtr("act1_no_orders", c.name)).msg()
        }
    }

    @Description("Creates a customer")
    @Parameters("name: String", "phone: String", "mobile: String")
    fun action2(name: String, phone: String, mobile: String) {
        val c = Customer()
        c.name = name.trim()
        c.phone = phone.trim()
        c.mobile = mobile.trim()
        m.insert(c)
        Txt.info("Created: ${c.name}, ph. = ${c.phone}, mob. = ${c.mobile}").msg()
    }

    @Description("Searches, updates and deletes")
    @Parameters("customerId: RowID", "productSubstring: String")
    fun action3(customerId: RowID, productSubstring: String) {
        val c = m.select(Customer(), customerId)
        val cName = c.name
        c.name = cName?.toUpperCase()
        m.update(c)
        Txt.info("Customer #${customerId}. Changed name from '${cName}' to '${c.name}'").msg()
        val p = m.selectFirst<Product>("name like '%${productSubstring}%'")
        if (p != null) {
            val pName = p.name
            p.name = pName?.toLowerCase()
            m.update(p)
            Txt.info("Changed product name from '${pName}' to '${p.name}'").msg()
        }
        val o = m.selectFirst<Shipping_Order>("customer = $customerId")
        if (o != null) {
            Txt.info("Deleting Order #${o.id} placed ${o.datetime_Order_Placed?.toLocalDate()}").msg()
            m.delete(o)
        } else {
            Txt.info("No orders found for customer id = $customerId").msg()
        }
    }

    @Description("Deletes list")
    @Parameters("customerId: RowId")
    fun action4(customerId: RowID) {
        Txt.info("Deleting orders for customer ID = $customerId").msg()
        m.deleteList("demo1_prvt.shipping_order", "customer=${customerId}")
    }

    @Description("Shows customers' orders summary - m.selectMap() version")
    @Parameters("customerFilter: String")
    fun action91(customerFilter: String) {
        val outFile = File("C:/dolmen/tmp/demo1.txt")
        outFile.bufferedWriter().use { out ->
            val start = OffsetDateTime.now()
            out.write("Started at $start\n")
            val customer = m.selectMap(Customer.fId, customerFilter).values.sortedBy { it.name }
            val order = m.selectMap(Shipping_Order.fId, "").values.groupBy { it.customer }
            val orderProduct = m.selectMap(Shipping_Order_Product.fId, "").values.groupBy { it.shipping_Order }
            val product = m.selectMap(Product.fId, "")
            var finish = OffsetDateTime.now()
            out.write("Query finished at $finish\n")
            out.write("Runtime ${Duration.between(start, finish)}\n")
            var n0 = 0
            customer.forEach { c ->
                n0++
                out.write("${n0}. Name = ${c.name}, Phone = ${c.phone}\n")
                var n1 = 0
                order[c.id]?.sortedBy { it.datetime_Order_Placed }?.forEach { o ->
                    n1++
                    out.write("$n0.$n1. Order #${o.id} placed ${o.datetime_Order_Placed?.toLocalDate()}\n")
                    var n2 = 0
                    orderProduct[o.id]?.sortedBy { product[it.product]?.name }?.forEach { item ->
                        n2++
                        val p = product[item.product]
                        out.write("$n0.$n1.$n2. Product = ${p?.name}, qnty = ${item.quantity}\n")
                    }
                }
                if (n1 == 0) out.write("No orders for ${c.name}\n")
            }
            finish = OffsetDateTime.now()
            out.write("Finished at $finish\n")
            out.write("Runtime ${Duration.between(start, finish)}\n")
        }
    }

    @Description("Shows customers' orders summary - m.selectMap() version")
    @Parameters("customerFilter: String")
    fun action92(customerFilter: String) {
        val outFile = File("C:/dolmen/tmp/demo1.txt")
        outFile.bufferedWriter().use { out ->
            val start = OffsetDateTime.now()
            out.write("Started at $start\n")
            //val customer = m.selectMap(Customer.fId, customerFilter).values.sortedBy { it.name }
            val product = m.selectMap(Product.fId, "")
            var finish = OffsetDateTime.now()
            out.write("Query finished at $finish\n")
            out.write("Runtime ${Duration.between(start, finish)}\n")
            var n0 = 0
            //customer.forEach { c ->
            m.iterate<Customer>(customerFilter) { c ->
                n0++
                out.write("${n0}. Name = ${c.name}, Phone = ${c.phone}\n")
                var n1 = 0
                //val order = m.selectMap(Shipping_Order.fId, "customer=${c.id}").values
                //order.sortedBy { it.datetime_Order_Placed }.forEach { o ->
                m.iterate<Shipping_Order>("customer=${c.id}") { o ->
                    n1++
                    out.write("$n0.$n1. Order #${o.id} placed ${o.datetime_Order_Placed?.toLocalDate()}\n")
                    var n2 = 0
                    //val orderProduct = m.selectMap(Shipping_Order_Product.fId, "shipping_order=${o.id}").values
                    //orderProduct.sortedBy { product[it.product]?.name }.forEach { item ->
                    m.iterate<Shipping_Order_Product>("shipping_order=${o.id}") { item ->
                        n2++
                        val p = product[item.product]
                        out.write("$n0.$n1.$n2. Product = ${p?.name}, qnty = ${item.quantity}\n")
                    }
                }
                if (n1 == 0) out.write("No orders for ${c.name}\n")
            }
            finish = OffsetDateTime.now()
            out.write("Finished at $finish\n")
            out.write("Runtime ${Duration.between(start, finish)}\n")
        }
    }

    @Description("Shows customers' orders summary - m.selectMap() version")
    @Parameters("customerFilter: String")
    fun action93(customerFilter: String) {
        val outFile = File("C:/dolmen/tmp/demo1.txt")
        val queryChunkSize = 150
        outFile.bufferedWriter().use { out ->
            val start = OffsetDateTime.now()
            out.write("Started at $start\n")
            val customerMap = m.selectMap(Customer.fId, customerFilter)
            val customer = customerMap.values.sortedBy { it.name }
            val customerChunks = customerMap.keys.chunked(queryChunkSize)
            var orderMap = mapOf<RowID, Shipping_Order>()
            run {//forEach
                val chunk = customerChunks[0]
                val orderFilter = "customer in (${chunk.joinToString(",")})"
                orderMap = orderMap + m.selectMap(Shipping_Order.fId, orderFilter)
            }
            val order = orderMap.values.groupBy { it.customer }
            // max elements to use index search 153? or filter length?
            val orderChunks = orderMap.keys.chunked(queryChunkSize)
            var orderProductMap = mapOf<RowID, Shipping_Order_Product>()
            run {//forEach
                val chunk = orderChunks[0]
                val orderProductFilter = "shipping_order in (${chunk.joinToString(",")})"
                orderProductMap = orderProductMap + m.selectMap(Shipping_Order_Product.fId, orderProductFilter)
            }
            val orderProduct = orderProductMap.values.groupBy { it.shipping_Order }
            val product = m.selectMap(Product.fId, "")
            var finish = OffsetDateTime.now()
            out.write("Query finished at $finish\n")
            out.write("Runtime ${Duration.between(start, finish)}\n")
            var n0 = 0
            customer.forEach { c ->
                n0++
                out.write("${n0}. Name = ${c.name}, Phone = ${c.phone}\n")
                var n1 = 0
                order[c.id]?.sortedBy { it.datetime_Order_Placed }?.forEach { o ->
                    n1++
                    out.write("$n0.$n1. Order #${o.id} placed ${o.datetime_Order_Placed?.toLocalDate()}\n")
                    var n2 = 0
                    orderProduct[o.id]?.sortedBy { product[it.product]?.name }?.forEach { item ->
                        n2++
                        val p = product[item.product]
                        out.write("$n0.$n1.$n2. Product = ${p?.name}, qnty = ${item.quantity}\n")
                    }
                }
                if (n1 == 0) out.write("No orders for ${c.name}\n")
            }
            finish = OffsetDateTime.now()
            out.write("Finished at $finish\n")
            out.write("Runtime ${Duration.between(start, finish)}\n")
        }
    }

    @Description("Performs complex order item search")
    @Parameters("itemFilter", "productFilter", "orderFilter", "customerFilter")
    fun search(itemFilter: String?, productFilter: String?, orderFilter: String?,
               customerFilter: String?): List<ITopTable> {
        val count = 1000
        var item = m.selectMap(Shipping_Order_Product.fId, itemFilter)
        val product = m.selectMap(Product.fId, productFilter)
        var order = m.selectMap(Shipping_Order.fId, orderFilter)
        val customer = m.selectMap(Customer.fId, customerFilter)

        order = order.filterValues { it.customer in customer }
        item = item.filterValues { (it.product in product) and (it.shipping_Order in order) }
        return item.values.take(count).toList()
    }

}