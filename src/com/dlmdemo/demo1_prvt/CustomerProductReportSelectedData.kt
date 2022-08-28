package com.dlmdemo.demo1_prvt

import com.dolmen.ex.BaseException
import com.dolmen.md.demo1_prvt.Customer_Product_Report
import com.dolmen.md.demo1_prvt.Demo1_PrvtModuleBase
import com.dolmen.md.demo1_prvt.Shipping_Order
import com.dolmen.md.demo1_prvt.Shipping_Order_Product
import com.dolmen.serv.Txt
import com.dolmen.serv.conn.SelectedData
import com.dolmen.serv.conn.TableIt
import com.dolmen.serv.exp.FieldLimit
import com.dolmen.serv.exp.Formula
import com.dolmen.serv.exp.FormulaBuilder
import com.dolmen.serv.exp.QueryHelper
import com.dolmen.serv.table.RowID
import java.math.BigDecimal
import java.time.OffsetDateTime

class CustomerProductReportSelectedData: SelectedData<Customer_Product_Report> {

    /*
     * Формируем отчет: количество и сумма по каждому товару для каждого клиента за период.
     * Строка отчета выглядит так:
     * 1) Клиент
     * 2) Товар
     * 3) Количество и сумма из всех заказов за период по данному товару и клиенту
     *
     * Это таблицы:
     * 1) Customer - клиент
     * 2) Shipping_Order - заказ на дату
     * 3) Shipping_Order_Product - товар, количество, сумма
     *
     * Возможный порядок работы:
     * Нам нужна сумма по товару и клиенту. Ее можно считать разными способами:
     * 1) выбрать клиента и товар
     * 2) для клиента выбрать следующий заказ
     * 3) из заказа выбрать все строки по нужному товару, суммируя их
     * 4) вернуть строку отчета для этого клиента, этого товара
     * 5) вернуться на начало и выбрать следующую комбинацию
     *
     * Такой способ крайне неэффективен с точки зрения работы базы данных:
     * 1) все заказы придется читать по много раз для каждого товара
     * 2) все позиции одного заказа скорее всего располагаются на диске рядом, в одном или соседних блоках. Однако, мы читаем по одному товару, поэтому одни и те же блоки
     * могут читаться по много раз
     *
     * Оптимизируем.
     * 1) мы знаем, что товарных позиций у нас 10 тыс. штук, а значит мы можем сохранить их в памяти. Даже если клиент за период покупал много позиций.
     * Оценка памяти: объект с id товара, числовое поле и поле типа BigDecimal - это порядка 8+8+65 байт, плюс накладные 20 байт, плюс накладные
     * на массив порядка 8 байт умножить на 10000 = порядка 100 килобайт памяти.
     * Значит все товары одного заказа мы эффективно прочитаем за один раз (это один или соседние блоки в БД).
     * 2) у нас интервал по датам и индекс на дату в таблице Shipping_Order. Значит эффективнее будет начинать выборку с нее, а не с клиента. Тогда мы прочитаем
     * каждый заказ только один раз, еще и ограничивая количество строк по индексу.
     * Однако, клиент мог делать несколько заказов за период, и нам нужно их найти. Хранить в памяти всех клиентов и все товары для них нельзя: памяти не хватит.
     * Кроме того, если у нас запросят только одного клиента и потом перестанут просить строки, окажется, что мы зря обрабатывали всех (это неэффективно).
     *
     * Чтобы обеспечить:
     * 2.1) чтение таблицы заказов один раз
     * 2.2) не читать все данные заранее и не хранить суммы по всем клиентам в памяти, мы схитрим (Алле привет).
     * Если сортировать выборку из Shipping_Order по полю Customer, то все заказы для одного клиента мы получим подряд.
     * То есть мы можем читать заказы пока не изменится поле customer, что будет означать, что заказов для этого клиента за период больше нет.
     * А зная, что по полю Shipping_Order.customer есть индекс, сортировка будет на стороне сервера БД (благодаря полю в индексной таблице, а не самому индексу, но это детали).
     *
     *
     * Реальный порядок работы:
     * Нам понадобится хранить в памяти:
     * 1) данные для строки отчета по каждому товару для текущего клиента (не забываем, почему в данном случае это можно - см. выше)
     * 2) последний считанный из базы данных Shipping_Order, по которому мы поняли, что клиент сменился - его надо будет обработать для следующего клиента, поэтому сохраняем
     *
     * Читать будем так:
     * 1) возвращаем строки отчета по товарам текущего клиента из памяти, пока они есть
     * 2) начинаем обработку для нового клиента. Обрабатываем запомненный ранее Shipping_Order (первый по этому клиенту)
     * 3) читаем и обрабатываем Shipping_Order, пока поле customer в них соответствует текущему клиенту. Как только оно сменится - запоминаем последний Shipping_Order
     * 4) все данные для текущего клиента готовы, переходим в начало
     *
     * Обработка каждого Shipping_Order:
     * 1) прочитать товарные позиции по нему из Shipping_Order_Product. Если в фильтре отчета указан только конкретный товар, то лучше сразу это учесть
     * 2) просуммировать данные по товару в памяти (HashMap по id товара) - оценка расхода памяти была выше
     */


    private val productIdFilter: RowID? // фильтр на конкретный товар, если он запрошен для отчета

    private var shippingOrdersIterator: TableIt<Shipping_Order>?=null // итератор для выборки Shipping_Order из БД один раз по фильтру дат с сортировкой пл customer

    private var notProcessedShippingOrder: Shipping_Order?=null // запомненный Shipping_Order, который надо будет обработать в первую очередь при смене клиента


    // Хранение и выборка строк сумм по товарам для текущего клиента
    private var currentCustomerId: RowID?=null // текущий клиент. Храним его для формирования строк Customer_Product_Report из ProductSum
    private var productId_to_productSum: HashMap<RowID, ProductSum>?=null // HashMap для хранения данных по товару для текущего клиента
    // Класс для хранения данных по товару. Можно сразу хранить в кэше строку Customer_Product_Report, но она значительно больше по размеру и дольше в обработке
    private class ProductSum(){
        var count: Int=0
        var sum=BigDecimal.ZERO
    }
    private var рroductSumIterator: Iterator<Map.Entry<RowID, ProductSum>>?=null // Итератор для выборки всех подготовленных сумм по товарам для текущего клиента

    // Запоминаем дату чтобы заполнить строку Customer_Product_Report
    val orderDate: OffsetDateTime

    constructor(f: Formula, demo: Demo1): super(f, demo){

        var dateFrom: OffsetDateTime?=null // мы знаем тип, потому что поле таблицы типа datetime
        var dateTo: OffsetDateTime?=null
        // Сделаем для примера, что в фильтре обязательно указать период, чтобы ограничить количество данных
        // Проверяем, что обе даты указаны
        var dateLimit=f.getLimit(Customer_Product_Report.fOrder_Date)
        if(dateLimit!=null){
            dateFrom=dateLimit.min as OffsetDateTime?
            dateTo = dateLimit.max as OffsetDateTime?
        }
        if(dateFrom==null || dateTo==null){
            // Формируем ошибку, в нее подставятся имена полей на языке клиента, если есть переводы на другой язык
            throw(BaseException(Txt.error(demo.EID(52050),
                Demo1_PrvtModuleBase.T.xtrLabel(null, Customer_Product_Report.fOrder_Date),
                Demo1_PrvtModuleBase.T.xtrLabel(null, Customer_Product_Report.T)
            )))
        }
        this.orderDate=dateFrom

        // Делаем фильтр для shipping_order из фильтра по Customer_Product_Report
        val shippingOrderBuilder=FormulaBuilder(f, Shipping_Order.T)

        // Условие по Customer, если есть (если его нет, то фильтр будет пустой, это нас устраивает)
        shippingOrderBuilder.addReplaceRule(Customer_Product_Report.fCustomer, Shipping_Order.fCustomer)

        // Условия на все другие поля таблицы Customer_Product_Report из фильтра для Shipping_Order надо убрать
        shippingOrderBuilder.removeOtherTables()

        // По другим полям совпадающих условий (которые можно напрямую использовать заменой поля в фильтре) нет
        // Но есть условие на интервал дат. Сформируем часть фильтра для него

        val dateClause=QueryHelper.c()
            .and(Shipping_Order.fShipment_Date, ">=", dateFrom)
            .and(Shipping_Order.fShipment_Date, "<=", dateTo)
            .orderBy(Shipping_Order.fCustomer, false) // сортировка, чтобы все документы по одному клиенту шли подряд
            .toString()

        // Объединяем условия в общий фильтр. Получим фильтр с условие на интервал дат и условие на клиента (если оно указано), а также
        // сортировку по клиенту
        val shippingOrderFilter=Formula.parse(dateClause, Shipping_Order.T).and(shippingOrderBuilder.filter)

        // Подготавливаем итератор для строк Shipping_Order
        shippingOrdersIterator = demo.iterate(shippingOrderFilter).iterator() as TableIt<Shipping_Order>

        // Для дальнейшей оптимизации посмотрим, есть ли условие на товар. Тогда мы сможем эффективнее выбирать из таблицы Shipping_Order_Product
        productIdFilter=FieldLimit.getEqual(f, Customer_Product_Report.fProduct) as RowID?
    }

    // Здесь подготавливаем следующую строку отчета
    override fun hasNext(): Boolean {

        if(table!=null) return true // если строка уже подготовлена, но ее у нас еще не забрали - то новую не выбираем

        // если есть готовые строки по клиенту в памяти - возвращаем
        PRODUCT_LOOP@ while(true) {
            if (рroductSumIterator != null) {
                while (рroductSumIterator!!.hasNext()) {
                    val productEnty = рroductSumIterator!!.next()
                    table=Customer_Product_Report()
                    table.customer=currentCustomerId
                    table.order_Date=orderDate
                    table.product=productEnty.key
                    val productData=productEnty.value
                    table.count=productData.count
                    table.value=productData.sum

                    /*
                     Поле ID мы здесь не заполняем, потому что по таблице Customer_Product_Report у нас указаны ключевые поля
                     (см. "key_seq":X в дескрипторе таблицы) - см. описание в документации (все ключевые поля
                     автоматически входят в поле ID и сами формируют его)
                     */

                    if(isRowFiltered(table)){
                        // этот вызов проверит, что table соответствует условию
                        // Если да, то table запомнится и будет выдан при вызове next() у нашего итератора
                        // Если нет, то table в нашем итераторе станет null
                        return true
                    }
                }
                рroductSumIterator=null
                currentCustomerId=null
            }

            // Строк по текущему клиенту больше нет, считаем для следующего клиента
            if(shippingOrdersIterator==null){
                // Все заказы уже обработаны, более строк не будет
                return false
            }

            // Если есть необработанный заказ по следующему клиенту - обрабатываем его
            if (notProcessedShippingOrder != null) {
                processShippingOrder(notProcessedShippingOrder!!)
                notProcessedShippingOrder = null
            }

            // Выбираем строки Shipping_Order и считаем суммы по ним, пока не сменится поле customer (помним, у нас по нему сортировка)
            // Суммируем строки по товару заказа в памяти (вызов processShippingOrder())

            while (shippingOrdersIterator!!.hasNext()) {

                val shippingOrder = shippingOrdersIterator!!.next()
                val newCustomer =  shippingOrder.customer
                if (currentCustomerId == null || currentCustomerId!!.equals(newCustomer)) {
                    processShippingOrder(shippingOrder)
                } else {
                    // Клиент изменился. Запоминаем Shipping_Order - обработаем его на следующем вызове как первый для нового клиента
                    notProcessedShippingOrder = shippingOrder
                    // Данные в памяти для текущего клиента готовы, переход вверх на их возврат построчно
                    currentClintReady()
                    continue@PRODUCT_LOOP
                }
            }
            // Заказов больше нет
            shippingOrdersIterator!!.close()
            shippingOrdersIterator=null // Это сработает как флаг, что больше заказов нет, когда у нас снова вызовут hasNext() - см. выше
            currentClintReady()
            continue@PRODUCT_LOOP // Данные в памяти для текущего клиента готовы, переход вверх на их возврат построчно
        }
        return false
    }

    private fun processShippingOrder(shippingOrder: Shipping_Order){
        if(currentCustomerId==null){
            currentCustomerId=shippingOrder.customer
        }
        if(productId_to_productSum==null){
            productId_to_productSum=HashMap(1000)
            /* Заметка.
                Считаем что у нас порядка тысячи товаров за период. Это оптимизация для перестройки HashMap - см. описание по HashMap
                По-умолчанию размер HashMap мал, и при большом кол-ве строк она будет перехешироваться целиком много раз - это может быть долго.
                Здесь 1000 в большинстве случаев с запасом, но это всего несколько килобайт памяти, что с лихвой окупит даже одну перестройку Map
             */
        }

        val queryHelper=QueryHelper.c().and(Shipping_Order_Product.fShipping_Order, shippingOrder.id)
        if(productIdFilter!=null){
            queryHelper.and(Shipping_Order_Product.fProduct, productIdFilter) // оптимизируем, если запрошен отчет по конкретному товару
        }

        // читаем все позиции по заказу. Использование .use гарантирует, что в случае ошибки iterator будет автоматически закрыт (вызвано close()) для освобождения рекурсов запроса к БД
        module.iterate(Shipping_Order_Product::class.java, queryHelper.toString()).use { shippingOrderProductData ->
            shippingOrderProductData.forEach { shippingOrderProduct ->
                // суммируем позицию заказа в HashMap
                val productId=shippingOrderProduct.product
                if(productId!=null) {
                    var productSum = productId_to_productSum!![productId]
                    if (productSum == null) {
                        productSum = ProductSum()
                        productId_to_productSum!![productId] = productSum
                    }
                    productSum.count += shippingOrderProduct.quantity
                    val sum = shippingOrderProduct.sum
                    if (sum != null) {
                        productSum.sum += sum
                    }
                }
            }
        }
    }

    private fun currentClintReady(){
        if(productId_to_productSum!=null) {
            рroductSumIterator = productId_to_productSum!!.entries.iterator()
            productId_to_productSum = null // Это будет флаг для processShippingOrder() что начинается новый клиент и нужна новая HashMap
        }
    }


    override fun close() {
        shippingOrdersIterator?.close()
    }

}