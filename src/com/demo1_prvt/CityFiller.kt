package com.demo1_prvt

import com.dolmen.md.demo1_prvt.City
import com.dolmen.md.demo1_prvt.Country
import com.dolmen.md.demo1_prvt.Subcountry

class CityFiller : City.ICity {
    private val db by lazy { MyModule.start() }

    override fun getList_Nm(table: City): String {
        val sbcntr = db.select(Subcountry(), table.subcountry_Id)
        val cntr = db.select(Country(), table.country_Id)
        return "${table.name}, ${sbcntr.name}, ${cntr.name}"
    }

    override fun setList_Nm(table: City, value: String?) {}
}