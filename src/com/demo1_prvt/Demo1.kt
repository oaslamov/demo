package com.demo1_prvt

import com.demo1_prvt.filler.CityFiller
import com.dolmen.md.demo1_prvt.City
import com.dolmen.md.demo1_prvt.Demo1_PrvtModuleBase
import com.dolmen.md.demo1_prvt.View1
import com.dolmen.mod.GuiModule
import com.dolmen.serv.anno.ActionType
import com.dolmen.serv.anno.Description
import com.dolmen.serv.anno.Parameters
import com.dolmen.serv.conn.SelectedData
import com.dolmen.serv.exp.Formula
import com.dolmen.serv.table.ITopTable
import com.dolmen.serv.table.RowID
import com.dolmen.util.Text
import java.time.LocalDate


class Demo1 : Demo1_PrvtModuleBase() {

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

    override fun beforeUpdate(t: ITopTable) {
        Operations(this).triggerBeforeUpdate(t)
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

    override fun x_installed(modulePreviousVersionId: Int) {
        super.x_installed(modulePreviousVersionId)
        Populate(this).loadSampleData()
    }

    companion object {
        init {
            T.registerFieldFiller(City.ICity::class.java, CityFiller::class.java)
        }

        fun start(): Demo1 {
            return com.dolmen.serv.Module.start(Demo1::class.java)
        }
    }
}

