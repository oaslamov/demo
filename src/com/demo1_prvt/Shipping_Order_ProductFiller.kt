package com.demo1_prvt


import com.dolmen.md.demo1_prvt.Product
import com.dolmen.md.demo1_prvt.Shipping_Order_Product
import com.dolmen.serv.table.ITableFieldFiller
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.RoundingMode

class Shipping_Order_ProductFiller : Shipping_Order_Product.IShipping_Order_Product {
    override fun getInstance(): ITableFieldFiller = Shipping_Order_ProductFiller()

    private val db get() = MyModule.start()
//    var item: Shipping_Order_Product? = null
//
//    private fun item(itemRec: DocTable): Doc = item ?: run { Doc(docRec, db) }

    override fun getI_Price(table: Shipping_Order_Product?): BigDecimal {
        val p = db.select(Product(), table?.product)
        val itemPrice = p.price.toBigDecimal().setScale(2, RoundingMode.HALF_UP)
        return itemPrice
    }

    override fun setI_Price(table: Shipping_Order_Product?, value: BigDecimal?) {}

    override fun getI_Sum(table: Shipping_Order_Product?): BigDecimal {
        val itemPrice = table?.i_Price ?: ZERO
        val itemQnty = table?.quantity ?: 0
        val itemSum = (itemPrice * itemQnty.toBigDecimal()).setScale(2, RoundingMode.HALF_UP)
        return itemSum
    }

    override fun setI_Sum(table: Shipping_Order_Product?, value: BigDecimal?) {}
}