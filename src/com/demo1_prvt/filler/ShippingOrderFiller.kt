package com.demo1_prvt.filler

import com.demo1_prvt.Demo1
import com.dolmen.md.demo1_prvt.Shipping_Order
import com.dolmen.md.demo1_prvt.Shipping_Order_Product
import com.dolmen.serv.table.ITableFieldFiller
import com.dolmen.util.JSONManager
import java.math.BigDecimal


class ShippingOrderFiller : Shipping_Order.IShipping_Order {
    private val db by lazy { Demo1.start() }
    override fun getInstance(): ITableFieldFiller = ShippingOrderFiller()

    override fun getTotal(table: Shipping_Order): BigDecimal {
        val aggr = db.selectAggregates(Shipping_Order_Product.TABLE_ID, "shipping_order=${table.id}", "SUM(sum)")
        val s = JSONManager.parse(aggr, List::class.java) as List<Map<String, *>>
        val r = if (s.isNullOrEmpty()) BigDecimal.ZERO else s[0]["val"].toString().toBigDecimal()
        return r
    }

    override fun setTotal(table: Shipping_Order, value: BigDecimal?) {}
}