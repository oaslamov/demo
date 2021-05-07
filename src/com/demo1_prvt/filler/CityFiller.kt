package com.demo1_prvt.filler

import com.demo1_prvt.Demo1
import com.dolmen.md.demo1_prvt.City
import com.dolmen.md.demo1_prvt.Country
import com.dolmen.md.demo1_prvt.Subcountry
import com.dolmen.serv.table.ITableFieldFiller

class CityFiller : City.ICity {
    private val db by lazy { Demo1.start() }
    override fun getInstance(): ITableFieldFiller = CityFiller()

    override fun getList_Nm(table: City): String {
        val sbcntr = db.select(Subcountry(), table.subcountry_Id)
        val cntr = db.select(Country(), table.country_Id)
        return listOfNotNull(table.name, sbcntr.name, cntr.name).joinToString()
    }

    override fun setList_Nm(table: City, value: String?) {}
}
