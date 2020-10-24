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
