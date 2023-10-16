package com.dlmdemo.demo1_prvt

import com.roofstone.md.demo1_prvt.Shipping_Order
import com.roofstone.serv.ENV
import com.roofstone.serv.exp.Formula
import com.roofstone.serv.exp.FormulaBuilder
import com.roofstone.serv.table.Field
import com.roofstone.serv.table.FieldRef
import com.roofstone.serv.table.TableType

class EF2TabMapper(val T: TableType) {

    val ref2EField: FieldRef<*, *, *>?
    init{
        if(T!= Shipping_Order.T){
            ref2EField=T.getRefField(Shipping_Order.T)
            if(ref2EField==null) {
                ENV.log().error("Table $0 does not have ref to $1", T, Shipping_Order.T)
            }
        }else{
            ref2EField=null
        }
    }

    val ef_field2tab_field=HashMap<Field<*, *>,Field<*,*>>()

    fun getFormula(filter: Formula): Formula {
        val fb: FormulaBuilder = FormulaBuilder(filter, T)
        ef_field2tab_field.forEach{ a, b ->
            fb.addReplaceRule(a, b)
        }
        fb.removeOtherTables()
        return fb.filter
    }
}