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
import com.dolmen.util.Text
import java.io.File
import java.lang.Math.random
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.math.round

class MyModule : Demo1_PrvtModuleBase() {

    @Description("My Action")
    @Parameters("input: String")
    fun myAction(input: String): String {
        return Text.F("\n-------------\nTest OK (input: $0)\n--------------", input)
    }

    @Description("Show customers' orders summary")
    @Parameters("customerFilter: String")
    fun action1(customerFilter: String): String {
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
                    val p = select(Product(), item.product)
                    Txt.info("$n.$m.$k. Product = ${p.name}, qnty = ${item.quantity}").msg()
                }
            }
            if (m == 0) Txt.info("No orders for ${c.name}").msg()
        }
        return Text.F("Done")
    }

    @Description("Go through products")
    @Parameters("productFilter: String")
    fun action2(productFilter: String): String {
        var n = 0
        iterate<Product>(productFilter) { p ->
            n++
            Txt.info("${n}. Product = '${p.name}', type = '${xtrLabel(Product.fProduct_Type.enumed.getByValue(p.product_Type))}'").msg()
        }
        return Text.F("Done")
    }

    @Description("Create a customer")
    @Parameters("name: String", "phone: String", "mobile: String")
    fun action3(name: String, phone: String, mobile: String): String {
        val c = Customer()
        c.name = name
        c.phone = phone
        c.mobile = mobile
        insert(c)
        return Text.F("Done")
    }



    @Description("Generate products")
    @Parameters("pathIn: String", "n: Int")
    fun genProduct(pathIn: String, n: Int): String {
        val fileIn = File(pathIn)
        fileIn.useLines { lines ->
            var i = 1
            for (l in lines) {
                val p = Product()
                p.name = l.trim()
                p.price = round((random() * 3000) + 1) / 100
                p.product_Type = Product.PRODUCT_TYPE.GROCERY
                insert(p)
                if (i == n) break
                i++
            }
        }
        return Text.F("Done")
    }

    @Description("Generate customers")
    @Parameters("pathIn: String", "n: Int")
    fun genCustomer(pathIn: String, n: Int): String {
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


    @Description("Generate orders")
    @Parameters("n: Int")
    fun genOrder(n: Int): String {
        val placedDaysAgoMax = 300
        val paidAfterMax = 30
        val shipmentAfterMax = 45
        val itemsMin = 3
        val itemsMax = 10
        val mc = count(Customer::class, "")
        val step = (mc / n).toInt()
        var next = (random() * step + 1).toInt()
        //Txt.info("mc = ${mc}, step = ${step}, next = ${next}").msg()
        var i = 0
        var k = 0
        iterate<Customer>("") { c ->
            i++
            if ((i == next) and (k < n)) {
                next += step
                k++
                val o = Shipping_Order()
                val placedDaysAgo = (random() * placedDaysAgoMax).toLong()
                val paidDaysAgo = (placedDaysAgo - (random() * paidAfterMax)).coerceAtLeast(0.0).toLong()
                val shipmentDaysAgo = placedDaysAgo - (random() * shipmentAfterMax).toLong()
                o.customer = c.id
                o.datetime_Order_Placed =
                        OffsetDateTime.now().minusDays(placedDaysAgo).minusMinutes((random() * 3600).toLong())
                o.date_Order_Paid = LocalDate.now().minusDays(paidDaysAgo)
                o.shipment_Date = LocalDate.now().minusDays(shipmentDaysAgo)
                insert(o)
                o.genItems(itemsMin, itemsMax, 15)
                //Txt.info(
                //        "Customer ${i}, ${c.name}, placed ${o.datetime_Order_Placed}, paid ${o.date_Order_Paid}, shipment ${o.shipment_Date}").msg()
            }
        }
        return Text.F("Done")
    }

    @Description("Import cities")
    @Parameters("pathIn: String")
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


    private fun Shipping_Order.genItems(min: Int, max: Int, mQ: Int) {
        val mp = count(Product::class, "")
        val n = (random() * (max - min) + min).toInt()
        val step = (mp / n).toInt()
        var next = (random() * step + 1).toInt()
//        Txt.info("mp = ${mp}, n = ${n}, step = ${step}, next = ${next}").msg()
        var i = 0
        var k = 0
        iterate<Product>("") { p ->
            i++
            if ((i == next) and (k < n)) {
                next += step
                k++
                val item = Shipping_Order_Product()
                item.shipping_Order = this.id
                item.product = p.id
                item.quantity = (random() * mQ + 1).toInt()
                insert(item)
//                Txt.info("\tproduct: ${i}, ${p.name}, ${item.quantity}").msg()
            }
        }
    }

    private fun Shipping_Order_Product.getSum(): BigDecimal {
        return (this.getPrice() * BigDecimal(this.quantity)).setScale(2, RoundingMode.HALF_UP)
    }

    private fun Shipping_Order_Product.getPrice(): BigDecimal {
        val p = selectFirst<Product>("id = ${this.product}")
        return if (p == null) BigDecimal.ZERO else BigDecimal(p.price).setScale(2, RoundingMode.HALF_UP)
    }

    private fun Shipping_Order.getTotal(): BigDecimal {
        var sum: BigDecimal = BigDecimal.ZERO
        iterate<Shipping_Order_Product>("shipping_order = ${this.id}") { item ->
            sum += item.getSum()
        }
        return sum
    }


    override fun s_iterateView1(f: Formula): SelectedData<View1> {
        class ViewIterator(f: Formula, m: MyModule) : View1.Data(f, m) {
            override fun create(s: Shipping_Order): View1 {
                val v = super.create(s)
                v.total_Sum = s.getTotal()
                if (s.customer != null) {
                    val c = select(Customer(), s.customer)
                    v.c_Name = c.name
                    v.c_Phone = c.phone
                    v.c_Mobile = c.mobile
                    v.c_Address_Line1 = c.address_Line1
                    v.c_Address_Line2 = c.address_Line2
                    v.c_Address_Line3 = c.address_Line3
//                    if (c.city != null) {
//                        val ct = select(City(), c.city)
//                        if (ct != null) {
//                            v.c_City = "${ct.name}"
//                        }
//                    }
                }
                return v
            }
        }
        return ViewIterator(f, this)
    }

    override fun s_iterateView2(f: Formula): SelectedData<View2> {
        class ViewIterator(f: Formula, m: MyModule) : View2.Data(f, m) {
            override fun create(s: Shipping_Order_Product): View2 {
                val v = super.create(s)
                v.price = s.getPrice()
                v.sum = s.getSum()
                return v
            }
        }
        return ViewIterator(f, this)
    }

    override fun s_iterateView4(f: Formula): SelectedData<View4> {
        class ViewIterator(f: Formula, m: MyModule) : View4.Data(f, m) {
            override fun create(s: City): View4 {
                val v = super.create(s)
                val sbcntr = select(Subcountry(), s.subcountry_Id)
                val cntr = select(Country(), s.country_Id)
                v.list_Name = "${s.name}, ${sbcntr.name}, ${cntr.name}"
                return v
            }
        }
        return ViewIterator(f, this)
    }

    @Description("Refresh All Cities")
    fun refreshAllCities() {
        iterate<City>("") { c ->
            refreshCity(c)
            update(c)
        }
    }

    fun refreshCity(city: City) {
        val sbcntr = select(Subcountry(), city.subcountry_Id)
        val cntr = select(Country(), city.country_Id)
        city.list_Nm = "${city.name}, ${sbcntr.name}, ${cntr.name}"
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
}
