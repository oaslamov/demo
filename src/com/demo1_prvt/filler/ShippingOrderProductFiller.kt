package com.demo1_prvt.filler

import com.dolmen.md.demo1_prvt.Shipping_Order_Product
import java.math.BigDecimal

class ShippingOrderProductFiller : Shipping_Order_Product.IShipping_Order_Product {
    override fun getSum(table: Shipping_Order_Product): BigDecimal =
             table.quantity.toBigDecimal() * (table.price?: BigDecimal.ZERO)

    override fun setSum(table: Shipping_Order_Product, value: BigDecimal?) {}
}