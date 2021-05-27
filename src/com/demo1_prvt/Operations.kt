package com.demo1_prvt

import com.dolmen.md.demo1_prvt.*
import com.dolmen.serv.table.ITopTable
import java.math.BigDecimal

class Operations(val m: Demo1) {
    fun triggerBeforeUpdate(t: ITopTable?) {
        when (t) {
            is Customer -> refreshCustomer(t)
            is Shipping_Order_Product -> refreshShippingOrderProduct(t)
        }
    }

    fun triggerBeforeInsert(t: ITopTable?) {
        if (m.isLoadingSampleData) return
        when (t) {
            is Customer -> refreshCustomer(t)
            is Shipping_Order_Product -> refreshShippingOrderProduct(t)
        }
    }

    fun triggerAfterInsert(t: ITopTable?) {
        if (m.isLoadingSampleData) return
    }

    fun triggerBeforeDelete(t: ITopTable?) {
        when (t) {
            is Customer -> refreshCustomer(t)
            is Shipping_Order_Product -> refreshShippingOrderProduct(t)
        }
    }

    fun refreshCustomer(t: Customer) {
        t.city?.let {
            m.selectFirst<City>("id=${t.city}")?.let { ct ->
                t.country = ct.country_Id
                t.subcountry = ct.subcountry_Id
                t.city = ct.id
            }
        }
    }

    fun refreshShippingOrderProduct(t: Shipping_Order_Product) {
        t.apply {
            val oldItem = m.selectFirst<Shipping_Order_Product>("id=$id")
            val shouldRefreshPrice = (price?.compareTo(BigDecimal.ZERO) ?: 0) <= 0  // unknown or bad price
                    || oldItem?.product != product  // product is changed
            if (shouldRefreshPrice) price = m.selectFirst<Product>("id=${product}")?.price // get price for product
            val oldSum = oldItem?.sum ?: BigDecimal.ZERO
            val newSum = (price ?: BigDecimal.ZERO) * quantity.toBigDecimal()
            if (oldSum != newSum) {  // update item sum and order total
                sum = newSum
                val orderId = if (shipping_Order == null)
                    oldItem?.shipping_Order // when deleting an item
                else shipping_Order // when creating or updating an item
                m.selectFirst<Shipping_Order>("id=$orderId")?.let { o ->
                    o.total = m.selectMap(Shipping_Order_Product.fId, "shipping_order=${o.id}").values
                            .sumOf { it.sum ?: BigDecimal.ZERO } - oldSum + newSum
                    m.update(o)
                }
            }
        }
    }
}
