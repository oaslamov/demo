package com.demo1_prvt

import com.dolmen.ex.BaseException
import com.dolmen.md.demo1_prvt.*
import com.dolmen.serv.Msg
import com.dolmen.serv.TableBinaryDataProvider
import com.dolmen.serv.Txt
import com.dolmen.serv.anno.Description
import com.dolmen.serv.anno.Parameters
import com.dolmen.serv.table.RowID
import com.dolmen.serv.table.Table
import com.dolmen.ui.Resource
import com.dolmen.ui.screen.*
import com.dolmenmod.mail.Mail
import java.util.*
import kotlin.collections.set


class CustomActions(val m: Demo1) {
    @Description("Shows customers' orders summary")
    @Parameters("customerFilter: String")
    fun action1(customerFilter: String) {
        val product = m.selectMap(Product.fId, "")
        var n0 = 0
        m.iterate<Customer>(customerFilter) { c ->
            n0++
            Msg.create(Txt.info(m.MID("act1_c_header"), n0, c.name))
                    .fullText(m.xtr("act1_c_header_det", n0, c.name, c.phone, c.mobile,
                            listOfNotNull(c.address_Line1, c.address_Line2, c.address_Line3).joinToString()))
                    .msg()
            var n1 = 0
            m.iterate<Shipping_Order>("customer=${c.id}") { o ->
                n1++
                Txt.info(m.MID("act1_o_header"), n0, n1, o.id, o.datetime_Order_Placed?.toLocalDate()).msg()
                var n2 = 0
                m.iterate<Shipping_Order_Product>("shipping_order=${o.id}") { item ->
                    n2++
//                    val p = m.select(Product(), item.product)
                    val p = product[item.product]
                    Txt.info(m.MID("act1_p_header"), n0, n1, n2, p?.name, item.quantity).msg()
                }
            }
            if (n1 == 0) Txt.info(m.MID("act1_no_orders"), c.name).msg()
        }
    }

    @Description("Creates a customer")
    @Parameters("name: String", "phone: String", "mobile: String")
    fun action2(name: String, phone: String, mobile: String) {
        val c = Customer()
        c.name = name.trim()
        c.phone = phone.trim()
        c.mobile = mobile.trim()
        m.insert(c)
        Txt.info("Created: ${c.name}, ph. = ${c.phone}, mob. = ${c.mobile}").msg()
    }

    @Description("Searches, updates and deletes")
    @Parameters("customerId: RowID", "productSubstring: String")
    fun action3(customerId: RowID, productSubstring: String) {
        val c = m.select(Customer(), customerId)
        val cName = c.name
        c.name = cName?.toUpperCase()
        m.update(c)
        Txt.info("Customer #${customerId}. Changed name from '${cName}' to '${c.name}'").msg()
        val p = m.selectFirst<Product>("name like '%${productSubstring}%'")
        if (p != null) {
            val pName = p.name
            p.name = pName?.toLowerCase()
            m.update(p)
            Txt.info("Changed product name from '${pName}' to '${p.name}'").msg()
        }
        val o = m.selectFirst<Shipping_Order>("customer = $customerId")
        if (o != null) {
            Txt.info("Deleting Order #${o.id} placed ${o.datetime_Order_Placed?.toLocalDate()}").msg()
            m.delete(o)
        } else {
            Txt.info("No orders found for customer id = $customerId").msg()
        }
    }

    @Description("Deletes list")
    @Parameters("customerId: RowId")
    fun action4(customerId: RowID) {
        Txt.info("Deleting orders for customer ID = $customerId").msg()
        m.deleteList(Shipping_Order.TABLE_ID, "customer=${customerId}")
    }

    fun sendTestMail(to: String, subject: String, body: String) {
        val mailer = Mail.start(Mail::class.java)
        mailer.send("Test sender<test@example.org>", subject, body, true, mailer.getPrefs(null), to)
    }

    fun uploadNewFile(infoFields: Map<String?, Any?>?, dataTableName: String, filename: String,
                      fileTimeMillis: Long, data: ByteArray?) {
        val dataTableTT = Table.T(dataTableName)
        val tbd = dataTableTT.tableBinaryDataProvider
        if (tbd == null) {
            throw BaseException(Txt.error("Cannnot upload: table \"$0\" is not a data table", dataTableTT))
        } else {
            val infoRec = m.insert(tbd.infoTableType.name, infoFields)
            tbd.update(infoRec.id, data, filename, fileTimeMillis, TableBinaryDataProvider.MODE.UPDATE)
        }
    }

    fun getRichTextPopupScreen(originalScrId: String?, scrId: String, args: Array<out String>?): String? {
        var tableName = ""
        var fieldName = ""
        var rowId = ""
        if (args?.size == 3) {
            tableName = args[0]
            fieldName = args[1]
            rowId = args[2]
        }
        val scr = Screen(Resource.STORE_TYPE.STD)
        with(scr) {
            code = scrId
            grid = Grid().apply {
                base = "screen"
                cols = 1
                rows = 1
            }
            val ds = DataSource()
            ds.code = "ds_o"
            ds.table_name = tableName
            ds.generateFields(this)
            label = ds.fields.find { it.code == fieldName }?.label
            ds.operations = LinkedHashMap()

            ds.operations["select"] = Operation().apply {
                request = Request()
                request.data = ActionData()
                request.data.action = "demo1_prvt.selectList"
                request.data.args = mapOf("tableName" to tableName, "filter" to listOf("id=$rowId"))
            }

            ds.operations["update"] = Operation().apply {
                request = Request()
                request.data = ActionData()
                request.data.action = "demo1_prvt.update"
                request.data.args = mapOf("tableName" to tableName, "fields" to "\${@all_fields}")
            }

            data_sources = arrayListOf(ds)

            val part = Part().apply {
                code = "p_main"
                type = "text"
                data_source = PartDataSource()
                data_source.code = ds.code
                position = Position()
                position.from_col = 1
                position.to_col = 1
                position.from_row = 1
                position.to_row = 1
                data_source = PartDataSource()
                data_source.code = "ds_o"
                data_source.fields = arrayListOf(PartField().apply { code = fieldName })
                data_source.actions = arrayListOf("select", "update")
            }
            parts = arrayListOf(part)
        }
        return scr.toPreparedJson()
    }
}