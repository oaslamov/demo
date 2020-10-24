package com.demo1_prvt

import com.dolmen.md.demo1_prvt.Demo1_PrvtModuleBase
import com.dolmen.md.demo1_prvt.*

import com.dolmen.serv.conn.SelectedData
import com.dolmen.serv.exp.Formula
import com.dolmen.util.Text
import java.math.BigDecimal
import java.math.RoundingMode

class MyModule : Demo1_PrvtModuleBase() {

    fun myAction(input: String): String {
        return Text.F("\n-------------\nTest OK (input: $0)\n--------------", input)
    }


    override fun s_iterateView1(f: Formula): SelectedData<View1> {
        class ViewIterator(f: Formula, m: MyModule) : View1.Data(f, m) {
            override fun create(s: Shipping_Order): View1 {
                val v = super.create(s)
                if (s.customer != null) {
                    val c = select(Customer(), s.customer)
                    var sum = 0.00

                    iterate<Shipping_Order_Product>("shipping_order = ${s.id}") { item ->
                        val p = selectFirst<Product>("id = ${item.product}")
                        if (p != null) {
                            sum += item.quantity * p.price
                        }
                    }

                    v.c_Phone = c.phone
                    v.c_Mobile = c.mobile
                    v.c_Address_Line1 = c.address_Line1
                    v.c_Address_Line2 = c.address_Line2
                    v.c_Address_Line3 = c.address_Line3
                    v.total_Sum = BigDecimal(sum).setScale(2, RoundingMode.HALF_UP)
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
                val p = selectFirst<Product>("id = ${s.product}")
                if (p != null) {
                    v.price = BigDecimal(p.price).setScale(2, RoundingMode.HALF_UP)
                    v.sum = BigDecimal(s.quantity) * v.price
                }
                return v
            }
        }
        return ViewIterator(f, this)
    }
}