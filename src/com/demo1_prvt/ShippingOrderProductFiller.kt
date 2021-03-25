package com.demo1_prvt


import com.dolmen.md.demo1_prvt.Product
import com.dolmen.md.demo1_prvt.Shipping_Order_Product
import com.dolmen.serv.table.ITableFieldFiller
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.RoundingMode

class ShippingOrderProductFiller : Shipping_Order_Product.IShipping_Order_Product {
    override fun getInstance() = ShippingOrderProductFiller()

    private val db by lazy { MyModule.start() }

    private var p: Product? = null
    private fun getP(table: Shipping_Order_Product?): Product? {
        if (p == null) p = db.select(Product(), table?.product)
        return p
    }

    override fun getI_Price(table: Shipping_Order_Product?): BigDecimal {
        if (table == null) return ZERO
        val price = getP(table)?.price ?: 0.0
        return price.toBigDecimal().setScale(2, RoundingMode.HALF_UP)
    }

    override fun setI_Price(table: Shipping_Order_Product?, value: BigDecimal?) {}

    override fun getI_Sum(table: Shipping_Order_Product?): BigDecimal {
        if (table == null) return ZERO
        val itemPrice = table.i_Price ?: ZERO
        val itemQnty = table.quantity
        return (itemPrice * itemQnty.toBigDecimal()).setScale(2, RoundingMode.HALF_UP)
    }

    override fun setI_Sum(table: Shipping_Order_Product?, value: BigDecimal?) {}
}