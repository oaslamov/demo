package com.demo1_prvt

import com.dolmen.md.demo1_prvt.Customer
import com.dolmen.md.demo1_prvt.Demo1_PrvtModuleBase
import com.dolmen.md.demo1_prvt.Shipping_Order
import com.dolmen.md.demo1_prvt.View1
import com.dolmen.serv.conn.SelectedData
import com.dolmen.serv.exp.Formula
import com.dolmen.util.Text

class MyModule : Demo1_PrvtModuleBase() {

    fun myAction(input: String): String{
        return Text.F("\n-------------\nTest OK (input: $0)\n--------------", input)
    }

    override fun s_iterateView1(f: Formula): SelectedData<View1> {
        class ViewIterator(f: Formula, m: MyModule): View1.Data(f, m) {
            override fun create(s: Shipping_Order): View1 {
                val v = super.create(s)
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
}