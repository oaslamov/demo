package com.dlmdemo.demo1_prvt.filler

import com.dlmdemo.demo1_prvt.Demo1
import com.roofstone.md.demo1_prvt.City
import com.roofstone.md.demo1_prvt.Country
import com.roofstone.md.demo1_prvt.Subcountry
import com.roofstone.serv.table.ITableFieldFiller

class CityFiller : City.ICity {
    private val db by lazy { Demo1.start() }
    override fun getInstance(): ITableFieldFiller = CityFiller()

    override fun getList_Nm(table: City): String {
        val sbcntr = db.select(Subcountry(), table.subcountry)
        val cntr = db.select(Country(), table.country)
        return listOfNotNull(table.name, sbcntr.name, cntr.name).joinToString()
    }

    override fun setList_Nm(table: City, value: String?) {}
}
