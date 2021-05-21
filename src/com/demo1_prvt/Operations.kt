package com.demo1_prvt

import com.dolmen.md.demo1_prvt.*
import com.dolmen.serv.table.ITopTable
import java.math.BigDecimal

class Operations(val m: Demo1) {
    fun triggerBeforeUpdate(t: ITopTable?) {
        when (t) {
            is Customer -> t.city?.let {
                m.selectFirst<City>("id=${t.city}")?.let { ct ->
                    t.country = ct.country_Id
                    t.subcountry = ct.subcountry_Id
                    t.city = ct.id
                }
            }
            is Shipping_Order_Product -> t.apply {
                if (price == BigDecimal.ZERO || price == null) {
                    price = m.selectFirst<Product>("id=${product}")?.price
                }
                val oldSum = m.selectFirst<Shipping_Order_Product>("id=$id")?.sum?: BigDecimal.ZERO
                val newSum = (price ?: BigDecimal.ZERO) * quantity.toBigDecimal()
                sum = newSum
                m.selectFirst<Shipping_Order>("id=${shipping_Order}")?.let {o->
                    o.total = m.selectMap(Shipping_Order_Product.fId, "shipping_order=${o.id}").values
                            .sumOf { it.sum ?: BigDecimal.ZERO } - oldSum + newSum
                    m.update(o)
                }
            }
        }
    }
}
