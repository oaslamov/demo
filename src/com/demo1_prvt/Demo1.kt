package com.demo1_prvt

import com.demo1_prvt.filler.CityFiller
import com.demo1_prvt.filler.ProductFiller
import com.dolmen.md.demo1_prvt.City
import com.dolmen.md.demo1_prvt.Demo1_PrvtModuleBase
import com.dolmen.md.demo1_prvt.Product
import com.dolmen.md.demo1_prvt.View1
import com.dolmen.mod.GuiModule
import com.dolmen.serv.Action
import com.dolmen.serv.anno.ActionType
import com.dolmen.serv.anno.Description
import com.dolmen.serv.anno.Parameters
import com.dolmen.serv.conn.SelectedData
import com.dolmen.serv.exp.Formula
import com.dolmen.serv.table.ITopTable
import com.dolmen.serv.table.RowID
import com.dolmen.ui.Resource
import com.dolmen.ui.screen.*
import com.dolmen.util.Text
import java.time.LocalDate
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.set


class Demo1 : Demo1_PrvtModuleBase() {
    var isLoadingSampleData = false

    @Description("My Action")
    @Parameters("input: String")
    fun myAction(input: String): String {
        return Text.F("\n-------------\nTest OK (input: $0)\n--------------", input)
    }

    @Description("Shows customers' orders summary")
    @Parameters("customerFilter: String")
    fun action1(customerFilter: String) {
        CustomActions(this).action1(customerFilter)
    }

    @Description("Creates a customer")
    @Parameters("name: String", "phone: String", "mobile: String")
    fun action2(name: String, phone: String, mobile: String) {
        CustomActions(this).action2(name, phone, mobile)
    }

    @Description("Searches, updates and deletes")
    @Parameters("customerId: RowID", "productSubstring: String")
    fun action3(customerId: RowID, productSubstring: String) {
        CustomActions(this).action3(customerId, productSubstring)
    }

    @Description("Deletes list")
    @Parameters("customerId: RowId")
    fun action4(customerId: RowID) {
        CustomActions(this).action4(customerId)
    }

    @Description("Shows customers' orders summary - selectMap() version")
    @Parameters("customerFilter: String")
    fun action91(customerFilter: String) {
        CustomActions(this).action91(customerFilter)
    }

    @Description("Shows customers' orders summary - selectMap() version")
    @Parameters("customerFilter: String")
    fun action92(customerFilter: String) {
        CustomActions(this).action92(customerFilter)
    }

    @Description("Shows customers' orders summary - selectMap() version")
    @Parameters("customerFilter: String")
    fun action93(customerFilter: String) {
        CustomActions(this).action93(customerFilter)
    }

    @Description("Updates all orders sums")
    fun updateAllOrders() {
        Populate(this).updateAllOrders()
    }

    @Description("Loads sample data")
    fun loadSampleData() {
        return Populate(this).loadSampleData()
    }

    @Description("Performs complex order item search")
    @Parameters("itemFilter", "productFilter", "orderFilter", "customerFilter")
    fun search(itemFilter: String?, productFilter: String?, orderFilter: String?,
               customerFilter: String?): List<ITopTable> {
        return CustomActions(this).search(itemFilter, productFilter, orderFilter, customerFilter)
    }

    override fun s_iterateView1(f: Formula): SelectedData<View1> {
        return Views(this).s_iterateView1(f)
    }

    @Description("Calculates sales statistics for a given period of time")
    @Parameters("start", "finish", "abLimit: AB threshhold default(65)", "bcLimit: BC threshhold default(90)")
    fun makeStats(start: LocalDate?, finish: LocalDate?, abLimit: Int, bcLimit: Int) {
        return Stats(this).makeStats(start, finish, abLimit, bcLimit)
    }

    @Description("Prepares JSON for charts example")
    fun getChartExample(): String {
        return ChartManager(this).getChartExample()
    }

    @Description("Prepares JSON for ABC analysis graph")
    fun getChartABC(): String {
        return ChartManager(this).getChartABC()
    }

    @Description("Prepares JSON for Order totals chart")
    @Parameters("points: Groups limits")
    fun getChartOrderTotals(points: String): String {
        return ChartManager(this).getChartOrderTotals(points)
    }

    @Description("Prepares JSON for Sales by country chart")
    fun getChartSalesByCountry(): String {
        return ChartManager(this).getChartSalesByCountry()
    }

    @Description("Prepares JSON for Percentage of sales by country chart")
    fun getChartSalesPercentageByCountry(): String {
        return ChartManager(this).getChartSalesPercentageByCountry()
    }

    @Description("Uploads product image to the server")
    @ActionType("file_upload")
    @Parameters("rowID: object id",
            "fileName: file name with extension",
            "fileTime: file modification date in long",
            "fileBytes: bytes array of file data")
    fun uploadProductImage(rowID: RowID?, fileName: String, fileTime: Long, fileBytes: ByteArray?) {
        CustomActions(this).uploadProductImage(rowID, fileName, fileTime, fileBytes)
    }

    @Description("Downloads product image from the server")
    @Parameters("productId: product id")
    fun downloadProductImage(productId: RowID): Action.FileData? {
        return CustomActions(this).downloadProductImage(productId)
    }

    @Description("Deletes product image")
    @Parameters("rowID: product id")
    fun deleteProductImage(rowID: RowID) {
        CustomActions(this).deleteProductImage(rowID)
    }

    override fun beforeUpdate(t: ITopTable) {
        RefreshTable(this).refreshTable(t)
    }

    override fun beforeDelete(t: ITopTable?, isTableFilled: Boolean) {
        RefreshTable(this).refreshTable(t, deleting = true)
    }

    override fun beforeInsert(t: ITopTable) {
        RefreshTable(this).refreshTable(t)
    }

    @Description("Inserts a new record and navigates to the specified screen")
    @Parameters("tableCode: table code", "screenCode: screen code", "mode: screen opening mode",
            "linkID: id code (null = 'id')", "fields: optional map of table field values (default null)")
    @ActionType("insert")
    fun insertAndGo(tableCode: String, screenCode: String, mode: String?, linkID: String?,
                    fields: Map<String, Any>?): ITopTable {
        val table = newTable(tableCode, fields) as ITopTable
        insert(table)
        val args = HashMap<String, Any>()
        if (linkID == null) args["id"] = table.id else args[linkID] = table.id
        GuiModule.goScreen(screenCode, args, mode)
        return table
    }

    @Description("Calls dolmen server (JSON)")
    fun callDolmenJson(): String {
        return Caller(this).callDolmenJson()
    }

    @Description("Calls dolmen server (XML)")
    fun callDolmenXml(): String {
        return Caller(this).callDolmenXml()
    }

    @Description("Sends a test mail message")
    @Parameters("To: send a message to", "Subject: message subject default(Test subject)",
            "Body: message body default(Test message)")
    fun sendTestMail(to: String, subject: String, body: String) {
        CustomActions(this).sendTestMail(to, subject, body)
    }

    override fun x_getDynScreen(originalScrId: String?, scrId: String?, args: Array<out String>?): String? {
        if (scrId == "ref_picker:scr@demo1_prvt") {
            var refField = ""
            var id = ""
            var tableName = ""
            var refTable = ""
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
                val ds = DataSource()
                ds.code = "ds_c"
                ds.table_name = refTable
                ds.fields = ArrayList<Field_c>()
                ds.fields.addAll(
                        listOf(
                                Field_c("name", "Name", "string"),
                                Field_c("phone", "Phone", "string"),
                                Field_c("mobile", "Mobile", "string")
                        ))

                val op = Operation()
                op.request = Request()
                op.request.data = ActionData()
                op.request.data.action = "demo1_prvt.selectList"
                op.request.data.args = mapOf("tableName" to refTable)
                //= ActionData().also{"dd"}
                //op.request.data.action="demo1_prvt.selectList"

                //op.request = Request(ActionData("demo1_prvt.selectList", linkedMapOf("tableName" to refTable), ArrayList()), ArrayList())
                ds.operations = LinkedHashMap()
                ds.operations.put("select", op)
                data_sources = ArrayList<DataSource>()
                data_sources.add(ds)

                parts = ArrayList<Part>()

                val part = Part()
                part.data_source = PartDataSource()
                part.data_source.code = ds.code
                part.generate(ds, Screen.GenerateOptions())
                part.position = Position()
                part.position.from_col = 1
                part.position.to_col = 1
                part.position.from_row = 1
                part.position.to_row = 1
                parts.add(part)
            }
            //scr.generate()
            val json = scr.toPreparedJson()
            return json
        }
        return null
    }

    override fun x_installed(modulePreviousVersionId: Int) {
        Populate(this).loadSampleData()
    }

    companion object {

        init {
            T.registerFieldFiller(City.ICity::class.java, CityFiller::class.java)
            T.registerFieldFiller(Product.IProduct::class.java, ProductFiller::class.java)
        }

        fun start(): Demo1 {
            return com.dolmen.serv.Module.start(Demo1::class.java)
        }
    }
}

