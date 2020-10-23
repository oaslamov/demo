package com.demo1_prvt

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

                v.c_Phone = "ppp"
                v.c_Mobile = "m"
                v.c_Address_Line1 = "aline1"
                v.c_Address_Line2 = "aline2"
                v.c_Address_Line3 = "aline3"

                return v
            }
        }
        return ViewIterator(f, this)
    }
}