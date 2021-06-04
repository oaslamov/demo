package com.demo1_prvt

import com.dolmen.md.demo1_prvt.*
import com.dolmen.serv.table.ITopTable
import java.math.BigDecimal.ZERO

class Operations(val m: Demo1) {

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
                t.country = city.country_Id
                t.subcountry = city.subcountry_Id
                t.city = city.id
            }
        }
    }

    fun refreshShippingOrderProduct(t: Shipping_Order_Product, deleting: Boolean = false) {
        if (!deleting) {
            t.price = m.selectFirst<Product>("id=${t.product}")?.price    // Always recalculate the whole row???
            t.sum = (t.price ?: ZERO) * t.quantity.toBigDecimal()
        }
        // t.shipping_Order is null when deleting an item
        // oldItem is null when creating an item
        // both are not null when editing an item
        val orderId = t.shipping_Order ?: m.selectFirst<Shipping_Order_Product>("id=${t.id}")?.shipping_Order
        val o = m.selectFirst<Shipping_Order>("id=$orderId")
        if (o != null) {
            var total = t.sum ?: ZERO                           // start with the current item sum
            var itemFilter = "shipping_order=$orderId"          // updating or deleting an item
            if (t.id != null) itemFilter += " and id!=${t.id}"   // creating an item
            m.iterate<Shipping_Order_Product>(itemFilter) { item ->
                total += item.sum ?: ZERO
            }
            o.total = total
            m.update(o)
        }
    }
}
