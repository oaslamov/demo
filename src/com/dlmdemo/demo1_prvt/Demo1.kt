package com.dlmdemo.demo1_prvt

import com.dlmdemo.demo1_prvt.filler.CityFiller
import com.dlmdemo.demo1_prvt.filler.ProductFiller
import com.dolmen.md.demo1_prvt.*
import com.dolmen.serv.Action
import com.dolmen.serv.Txt
import com.dolmen.serv.anno.ActionType
import com.dolmen.serv.anno.Description
import com.dolmen.serv.anno.Parameters
import com.dolmen.serv.anno.Priv
import com.dolmen.serv.conn.SelectedData
import com.dolmen.serv.exp.Formula
import com.dolmen.serv.table.ITopTable
import com.dolmen.serv.table.RowID
import com.dolmen.ui.screen.ChartData
import com.dolmen.util.Text
import java.time.LocalDate


open class Demo1 : Demo1_PrvtModuleBase() {
    var isLoadingSampleData = false

    @Description("My Action")
    @Parameters("input: String")
    fun myAction(input: String): String {
        Txt.info("Testing: $input").msg()
        return Text.F("\n-------------\nTest OK (input: $0)\n--------------", input)
    }

    @Description("Shows customers' orders summary")
    @Parameters("customerFilter: String")
    open fun action1(customerFilter: String) {
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

    @Description("Updates all orders sums")
    fun updateAllOrders() {
        Populate(this).updateAllOrders()
    }

    @Description("Loads sample data")
    fun loadSampleData() {
        return Populate(this).loadSampleData()
    }

    override fun s_iterateView1(f: Formula): SelectedData<View1> {
        return Views(this).s_iterateView1(f)
    }

    override fun s_iterateDecor_Test_Card(f: Formula): SelectedData<Decor_Test_Card> {
        return Views(this).s_iterateDecor_Test_Card(f)
    }

    @Description("Calculates sales statistics for a given period of time")
    @Parameters("start", "finish", "abLimit: AB threshhold default(65)", "bcLimit: BC threshhold default(90)")
    fun makeStats(start: LocalDate?, finish: LocalDate?, abLimit: Int, bcLimit: Int) {
        return Stats(this).makeStats(start, finish, abLimit, bcLimit)
    }

    @Description("Prepares JSON for charts example")
    @Parameters("isShowY2: shows second Y axis if true default(false)")
    fun getChartExample(isShowY2: Boolean?): Action.IJSONResult<ChartData<*, *>> {
        return ChartManager(this).getChartExample(isShowY2)
    }

    @Description("Prepares JSON for charts example 2")
    @Parameters("filter: filter type(filter)")
    fun getChartExample2(filter: String): Action.IJSONResult<ChartData<*, *>> {
        return ChartManager(this).getChartExample2(filter)
    }

    @Description("Prepares JSON for ABC analysis graph")
    fun getChartABC(): ChartData<*, *> {
        return ChartManager(this).getChartABC()
    }

    @Description("Prepares JSON for Order totals chart")
    @Parameters("points: Groups limits")
    fun getChartOrderTotals(points: String): ChartData<*, *> {
        return ChartManager(this).getChartOrderTotals(points)
    }

    @Description("Prepares JSON for Sales by country chart")
    fun getChartSalesByCountry(): ChartData<*, *> {
        return ChartManager(this).getChartSalesByCountry()
    }

    @Description("Prepares JSON for Percentage of sales by country chart")
    fun getChartSalesPercentageByCountry(): ChartData<*, *> {
        return ChartManager(this).getChartSalesPercentageByCountry()
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

    @Description("Calls dolmen server (JSON)")
    fun callDolmenJson(): String {
        return Caller(this).callDolmenJson()
    }

    @Description("Calls dolmen server (XML)")
    fun callDolmenXml(): String {
        return Caller(this).callDolmenXml()
    }

    @Description("Sends a test mail message")
    @Parameters(
        "To: send a message to", "Subject: message subject default(Test subject)",
        "Body: message body default(Test message)"
    )
    fun sendTestMail(to: String, subject: String, body: String) {
        CustomActions(this).sendTestMail(to, subject, body)
    }

    @Description("Creates a new record in the Info table and uploads a file to the Data table")
    @Parameters(
        "infoFields: : optional map of info table field values default(null)",
        "dataTableName: table having a field with usage=\"filedata\"",
        "filename: file name",
        "fileTimeMillis: file time",
        "data: data"
    )
    @ActionType("file_upload")
    @Priv(value = "update", tableParameterName = "dataTableName")
    fun uploadNewFile(
        infoFields: Map<String?, Any?>?, dataTableName: String, filename: String,
        fileTimeMillis: Long, data: ByteArray?
    ): RowID? {
        return CustomActions(this).uploadNewFile(infoFields, dataTableName, filename, fileTimeMillis, data)
    }

    @Description("Tests alt, ctrl keys")
    @Parameters("isAltPressed", "isCtrlPressed")
    fun testAltCtrl(isAltPressed: Boolean, isCtrlPressed: Boolean) {
        Txt.info("With Alt == $isAltPressed, with Ctrl == $isCtrlPressed").msg()
    }

    override fun x_getDynScreen(originalScrId: String?, scrId: String?, args: Array<out String>?): String? {
        return when (scrId) {
            "ref_picker:scr@demo1_prvt" -> RefLookup(this).getChooseCustomerScreen(originalScrId, scrId, args)
            "richtext_popup:scr@demo1_prvt" -> CustomActions(this).getRichTextPopupScreen(originalScrId, scrId, args)
            else -> null
        }
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

