package com.demo1_prvt.filler

import com.demo1_prvt.Demo1
import com.dolmen.md.demo1_prvt.Product
import com.dolmen.serv.table.ITableFieldFiller

class ProductFiller : Product.IProduct{
    private val db by lazy { Demo1.start() }
    override fun getInstance(): ITableFieldFiller = ProductFiller()

    override fun getUrl(table: Product): String = "https://www.google.com/search?q=${table.name}"

    override fun setUrl(table: Product, value: String?) {}
}