package com.demo1_prvt

import com.dolmen.md.demo1_prvt.*
import com.dolmen.serv.anno.Description
import com.dolmen.serv.anno.Parameters
import com.dolmen.serv.conn.SelectedData
import com.dolmen.serv.exp.Formula
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

        for (i in 1..n) {
            val o = Shipping_Order()
            val c = selectFirst<Customer>("name 'Daleo, Norah'")
            if (c != null) {
                val placedDaysAgo = (random() * placedDaysAgoMax).toLong()
                val paidDaysAgo = (placedDaysAgo - (random() * paidAfterMax)).coerceAtLeast(0.0).toLong()
                val shipmentDaysAgo = placedDaysAgo - (random() * shipmentAfterMax).toLong()

                o.customer = c.id
                o.datetime_Order_Placed =
                        OffsetDateTime.now().minusDays(placedDaysAgo).minusMinutes((random() * 3600).toLong())
                o.date_Order_Paid = LocalDate.now().minusDays(paidDaysAgo)
                o.shipment_Date = LocalDate.now().minusDays(shipmentDaysAgo)
                insert(o)
            }
        }
        return Text.F("Done")
    }

    private fun Shipping_Order_Product.sum(): BigDecimal {
        return (this.price() * BigDecimal(this.quantity)).setScale(2, RoundingMode.HALF_UP)
    }

    private fun Shipping_Order_Product.price(): BigDecimal {
        val p = selectFirst<Product>("id = ${this.product}")
        if (p != null) {
            return BigDecimal(p.price).setScale(2, RoundingMode.HALF_UP)
        } else return BigDecimal.ZERO
    }

    private fun Shipping_Order.total(): BigDecimal {
        var sum: BigDecimal = BigDecimal.ZERO
        iterate<Shipping_Order_Product>("shipping_order = ${this.id}") { item ->
            sum += item.sum()
        }
        return sum
    }

    override fun s_iterateView1(f: Formula): SelectedData<View1> {
        class ViewIterator(f: Formula, m: MyModule) : View1.Data(f, m) {
            override fun create(s: Shipping_Order): View1 {
                val v = super.create(s)
                v.total_Sum = s.total()
                if (s.customer != null) {
                    val c = select(Customer(), s.customer)
                    v.c_Phone = c.phone
                    v.c_Mobile = c.mobile
                    v.c_Address_Line1 = c.address_Line1
                    v.c_Address_Line2 = c.address_Line2
                    v.c_Address_Line3 = c.address_Line3
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
                v.price = s.price()
                v.sum = s.sum()
                return v
            }
        }
        return ViewIterator(f, this)
    }
}
