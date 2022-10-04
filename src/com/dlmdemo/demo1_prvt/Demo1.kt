package com.dlmdemo.demo1_prvt

import com.dlmdemo.demo1_prvt.filler.CityFiller
import com.dlmdemo.demo1_prvt.filler.ProductFiller
import com.dolmen.md.demo1_prvt.*
import com.dolmen.serv.Action
import com.dolmen.serv.Action.ListResult
import com.dolmen.serv.GLOB_ID
import com.dolmen.serv.Txt
import com.dolmen.serv.anno.*
import com.dolmen.serv.conn.SelectedData
import com.dolmen.serv.exp.Formula
import com.dolmen.serv.exp.QueryHelper
import com.dolmen.serv.table.*
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

    @Description("Generates random orders")
    @Parameters("n: Number of generated shipping orders")
    fun genOrders(n: Int) {
        isLoadingSampleData = true
        Populate(this).genOrders(n)
        isLoadingSampleData = false
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

    @Description("Runs some queries")
    @Parameters(
        "n: Repeat count default(10)",
        "dbUrl: database connection url default(jdbc:postgresql://127.0.0.1:5432/postgres?maxResultBuffer=10p&defaultRowFetchSize=20&adaptiveFetchMaximum=1000&adaptiveFetch=true)",
        "dbUser: database user default(postgres)",
        "dbPass: database password",
        "dbSchema: database Schema default(dolmen)",
        "fetchSize: default(20)"
    )
    fun testQuery(n: Int, dbUrl: String, dbUser: String, dbPass: String, dbSchema: String, fetchSize: Int) {
        TestPerf(this).testQuery(n, dbUrl, dbUser, dbPass, dbSchema, fetchSize)
    }

    override fun x_getDynScreen(originalScrId: String?, scrId: String?, args: Array<out String>?): String? {
        return when (scrId) {
            "ref_picker:scr@demo1_prvt" -> RefLookup(this).getChooseCustomerScreen(originalScrId, scrId, args)
            "richtext_popup:scr@demo1_prvt" -> CustomActions(this).getRichTextPopupScreen(originalScrId, scrId, args)
            else -> null
        }
    }

    companion object {

        init {
            T.registerFieldFiller(City.ICity::class.java, CityFiller::class.java)
            T.registerFieldFiller(Product.IProduct::class.java, ProductFiller::class.java)
        }

        @DolmenSafeField
        val EF_tableTypes = HashMap<TableType, EF2TabMapper>()

        private class EFRutimeData(val formula: Formula, val mapper: EF2TabMapper) {
        }

        fun start(): Demo1 {
            return com.dolmen.serv.Module.start(Demo1::class.java)
        }
    }

    override fun s_iterateCustomer_Product_Report(f: Formula): SelectedData<Customer_Product_Report> {
        return CustomerProductReportSelectedData(f, this)
    }

    @Parameters(
        "filter: filter (for shipping_order_filter table) default(null) type(filter)",
        "startFrom: start from row number default(0)",
        "count: number of rows to return default(1000)"
    )
    fun filterShippingOrders(filter: String?, startFrom: Int, count: Int): List<ITable> {
        val ef_formula = Formula.parse(filter, Shipping_Order_Filter.T)

        if (EF_tableTypes.size == 0) {
            initEF()
        }
        var mainIt: SelectedData<*>? = null
        var efRuntimeDatas = mutableListOf<EFRutimeData>()
        EF_tableTypes.values.forEach { mapper ->
            val formula = EF_tableTypes[mapper.T]!!.getFormula(ef_formula)
            if (Shipping_Order.T == mapper.T) {
                mainIt = iterate(formula)
            } else if (!formula.isAlwaysTrueExpression) {
                efRuntimeDatas.add(EFRutimeData(formula, mapper))
            }
        }

        var skip = startFrom
        val lst: ListResult<ITable> = ListResult(count)
        try {
            val main_it = mainIt!!.iterator()

            while (main_it.hasNext()) {
                val shippingOrder = main_it.next()
                val shippingOrderId = shippingOrder.id
                var isFail = false
                efRuntimeDatas.forEach l_check_refs@{ efRuntimeData ->
                    val mapper = efRuntimeData.mapper
                    val refField = mapper.ref2EField
                    if (refField == null) {
                        // skip error. Message printed already

                    } else {
                        val idClause = Formula.parse(QueryHelper.c().and(refField, shippingOrderId).toString(), mapper.T)
                        var runtimeFormula = Formula.copy(efRuntimeData.formula, null)
                        runtimeFormula.and(idClause)
                        if (!exists(runtimeFormula)) {
                            isFail = true
                            return@l_check_refs
                        }
                    }
                }
                if (!isFail) {
                    if (skip > 0) {
                        skip--
                    } else {
                        lst.add(shippingOrder)
                        if (lst.size >= count) {
                            lst.setIncompleteData(true)
                            break
                        }
                    }
                }
            }
        } finally {
            mainIt!!.close()
        }
        return lst
    }

    private fun initEF() {
        Shipping_Order_Filter.T.fields().forEach { ef_field ->
            val linkedFieldName = ef_field.getEtcString("linkedField")
            if (linkedFieldName != null) {
                val tabField = GLOB_ID.getIRegisteredGIDByName(linkedFieldName, false)
                if (tabField is Field<*, *>) {
                    val tabType = tabField.tableType
                    var efMapper = EF_tableTypes.get(tabType)
                    if (efMapper == null) {
                        efMapper = EF2TabMapper(tabType)
                        EF_tableTypes[tabType] = efMapper
                    }
                    efMapper.ef_field2tab_field[ef_field] = tabField
                }
            }
        }
    }
}

