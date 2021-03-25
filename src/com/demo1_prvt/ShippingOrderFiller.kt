package com.demo1_prvt

import com.dolmen.md.demo1_prvt.*
import java.math.BigDecimal
import java.math.BigDecimal.ZERO

class ShippingOrderFiller : Shipping_Order.IShipping_Order {
    private val db get() = MyModule.start()

    override fun getOrder_Total(table: Shipping_Order?): BigDecimal {
        if (table == null) return ZERO
        var sum = ZERO
        db.iterate<Shipping_Order_Product>("shipping_order = ${table.id}") { item ->
            sum += item.i_Sum ?: ZERO
        }
        return sum
    }

    override fun setOrder_Total(table: Shipping_Order?, value: BigDecimal?) {}
}