package com.demo1_prvt

import com.dolmen.ui.Resource
import com.dolmen.ui.screen.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.set

class RefLookup(val m: Demo1) {
    fun getChooseCustomerScreen(originalScrId: String?, scrId: String?, args: Array<out String>?): String? {
        var refField = ""
        var id = ""
        var tableName = ""
        var refTable = ""
        val refTableFields = arrayListOf("id", "name", "phone", "mobile",
                "address_line1", "address_line2", "address_line3")
        if (args?.size == 4) {
            refField = args[0]
            id = args[1]
            tableName = args[2]
            refTable = args[3]
        }
        val scr = Screen(Resource.STORE_TYPE.STD)
        with(scr) {
            code = scrId
            label = "Pick customer"
            grid = Grid()
            grid.base = "screen"
            grid.cols = 1
            grid.rows = 1
            width = "70"

            val ds = DataSource()
            ds.code = "ds_c"
            ds.table_name = refTable
            ds.generateFields(this)
            val op = Operation()
            op.request = Request()
            op.request.data = ActionData()
            op.request.data.action = "demo1_prvt.selectList"
            op.request.data.args = mapOf("tableName" to refTable,
                    "filter" to listOf("\${@user_filter}", "order by name"))
            op.request.data.fields = refTableFields
            ds.operations = LinkedHashMap()
            ds.operations["select"] = op

            val ac = Action_c()
            ac.code = "submit"
            ac.label = "Choose this row"
            ac.type = "ok"
            ac.request = Request()
            ac.request.data = ActionData()
            ac.request.data.action = "demo1_prvt.update"
            ac.request.data.args = mapOf("tableName" to tableName,
                    "fields" to mapOf("id" to id, refField to "\${ds_c.id}"))
            ds.actions = arrayListOf(ac)
            data_sources = arrayListOf(ds)

            parts = ArrayList<Part>()
            val part = Part()
            part.code = "p_main"
            part.type = "table"
            part.data_source = PartDataSource()
            part.data_source.code = ds.code
            part.position = Position()
            part.position.from_col = 1
            part.position.to_col = 1
            part.position.from_row = 1
            part.position.to_row = 1
            part.data_source = PartDataSource()
            part.data_source.code = "ds_c"
            part.data_source.fields = ArrayList(refTableFields.map { PartField().apply { code = it } })
            part.data_source.actions = arrayListOf("submit")
            parts.add(part)
        }
        return scr.toPreparedJson()
    }
}