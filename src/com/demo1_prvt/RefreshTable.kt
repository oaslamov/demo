package com.demo1_prvt

import com.dolmen.md.demo1_prvt.*
import com.dolmen.serv.table.ITopTable
import com.dolmen.serv.table.RowID
import java.math.BigDecimal.ZERO

class RefreshTable(val m: Demo1) {

    fun refreshTable(t: ITopTable?, deleting: Boolean = false) {
        if (m.isLoadingSampleData) return
        when (t) {
            is Customer -> refreshCustomer(t, deleting)
            is Shipping_Order_Product -> refreshShippingOrderProduct(t, deleting)
        }
    }

    fun refreshCustomer(t: Customer, deleting: Boolean = false) {
        if (deleting) return
        if (t.city != null) {
            val city = m.selectFirst<City>("id=${t.city}")
            if (city != null) {
                t.country = city.country
                t.subcountry = city.subcountry
                t.city = city.id
            }
        }
    }

    fun refreshShippingOrderProduct(t: Shipping_Order_Product, deleting: Boolean = false) {
        val orderId: RowID?
        if (!deleting) {
            t.price = m.selectFirst<Product>("id=${t.product}")?.price
            t.sum = (t.price ?: ZERO) * t.quantity.toBigDecimal()
            orderId = t.shipping_Order
        } else orderId = m.selectFirst<Shipping_Order_Product>("id=${t.id}")?.shipping_Order
        val o = m.selectFirst<Shipping_Order>("id=$orderId")
        if (o != null) {
            var total = t.sum ?: ZERO   // start with the sum of the current item
            m.iterate<Shipping_Order_Product>("shipping_order=$orderId") { item ->
                if (item.id != t.id) total += item.sum ?: ZERO  // and add sums of all other items of this order
            }
            o.total = total
            m.update(o)
        }
    }
}
