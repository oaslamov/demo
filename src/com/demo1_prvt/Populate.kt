package com.demo1_prvt

import com.dolmen.md.demo1_prvt.*
import com.dolmen.serv.CONST
import com.dolmen.serv.Txt
import com.dolmen.serv.anno.Description
import com.dolmen.serv.anno.Parameters
import com.dolmen.serv.table.RowID
import com.dolmen.util.Text
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.random.Random

class Populate(val m: Demo1) {
    fun loadSampleData() {
        val hasData = m.exists<Country>("") || m.exists<Subcountry>("") || m.exists<City>("")
                || m.exists<Product>("") || m.exists<Customer>("")
        if (hasData) {
            Txt.info(m.xtr("skip_loading_sample_data")).msg()
        } else {
            Txt.info(m.xtr("loading_sample_data")).msg()
            createCities()
            createCustomers()
            createProducts()
            genOrders(1000)
            Stats(m).makeStats(abLimit = 65, bcLimit = 90)
        }
    }

    @Description("Creates products")
    fun createProducts() {
        //val pathIn = "/product.csv"
        val maxPrice = 30
        //val lines = javaClass.getResource(pathIn).readText().lines().filterNot { it.isEmpty() }
        PRODUCT_DATASET.forEach { l ->
            Product().apply {
                name = l.trim()
                price = ((Random.nextInt(maxPrice * 100) + 1) / 100.0).toBigDecimal().setScale(2, RoundingMode.HALF_UP)
                product_Type = Product.PRODUCT_TYPE.GROCERY
                m.insert(this)
            }
        }
        Txt.info("Created ${PRODUCT_DATASET.size} products").msg()
    }

    @Description("Creates customers")
    fun createCustomers() {
        //val pathIn = "/customer.csv"
        val customersPerCountry = 500
        val countries = m.selectMap(
                Country.fId, "name in ('Australia', 'Canada', 'United Kingdom', 'United States') order by name")
                .toList().sortedBy { it.second.name }
        //val lines = javaClass.getResource(pathIn).readText().lines().filterNot { it.isEmpty() }
        CUSTOMER_DATASET.forEachIndexed { i, l ->
            val rec = l.split(",").toTypedArray()
            Customer().apply {
                name = "${rec[1]}, ${rec[0]}"
                phone = rec[6]
                mobile = rec[7]
                address_Line1 = rec[2]
                address_Line2 = "${rec[3]}, ${rec[4]}"
                address_Line3 = rec[5]
                country = countries[(i - 1) / customersPerCountry].first
                m.insert(this)
            }
        }
        Txt.info("Created ${CUSTOMER_DATASET.size} customers").msg()
    }

    @Description("Creates countries, subcountries and cities")
    fun createCities() {
        data class Rec(val city: String, val country: String, val subcountry: String, val geonameid: String)

        //val pathIn = "/world-cities.csv"
        //val recs = javaClass.getResource(pathIn).readText().lines().filterNot { it.isEmpty() }
        val recs = CITY_DATASET
                .map { l ->
                    val rec = l.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                    Rec(city = rec[0].replace("\"", "").take(CONST.MAX_STRING_CHARS),
                            country = rec[1].replace("\"", "").take(CONST.MAX_STRING_CHARS),
                            subcountry = rec[2].replace("\"", "").take(CONST.MAX_STRING_CHARS),
                            geonameid = rec[3].take(CONST.MAX_STRING_CHARS))
                }
                .groupBy { it.country }
                .mapValues { r -> r.value.groupBy { it.subcountry } }
        var n = 0
        recs.forEach { countryMap ->
            val countryId = Country().run { // Create a country and get its Id
                name = countryMap.key
                m.insert(this)
                Txt.info("Loading ${name}").msg()
                id
            }
            countryMap.value.forEach { subcountryMap ->
                val subcountryId = Subcountry().run { // Create a subcountry and get its Id
                    name = subcountryMap.key
                    country_Id = countryId
                    m.insert(this)
                    id
                }
                subcountryMap.value.forEach { rec ->
                    City().apply { // Create a city
                        name = rec.city
                        country_Id = countryId
                        subcountry_Id = subcountryId
                        geonameid = rec.geonameid
                        m.insert(this)
                        n++
                    }
                }
            }
        }
        Txt.info("Created $n cities").msg()
    }

    @Description("Generates random orders")
    @Parameters("n: Int")
    fun genOrders(n: Int): String {
        val rnd = java.util.Random()
        val maxPlacedDaysAgo = 365 * 3
        val maxPaidAfter = 30
        val maxShipmentAfter = 45
        val minItems = 3
        val maxItems = 10
        val maxQuantity = 15
        val minutesInDay = 3600
        val customers = m.selectMap(Customer.fId, "").values.toList().shuffled()
        val maxCustomer = customers.size
        val products = m.selectMap(Product.fId, "").values.toList().shuffled()
        val maxProduct = products.size
        repeat(n) { i ->
            val placedDaysAgo = Random.nextInt(maxPlacedDaysAgo + 1).toLong()
            val paidDaysAgo = (placedDaysAgo - Random.nextInt(maxPaidAfter + 1)).coerceAtLeast(0)
            val shipmentDaysAgo = placedDaysAgo - Random.nextInt(maxShipmentAfter + 1).toLong()
            //val m1 = Random.nextInt(maxCustomer)
            val m1 = ((0.15 * rnd.nextGaussian() + 0.5) * maxCustomer).toInt().coerceIn(0, maxCustomer - 1)
            val o = Shipping_Order().apply { // Create an order and get its Id
                customer = customers[m1].id
                datetime_Order_Placed =
                        OffsetDateTime.now().minusDays(placedDaysAgo).minusMinutes(Random.nextInt(minutesInDay).toLong())
                date_Order_Paid = LocalDate.now().minusDays(paidDaysAgo)
                shipment_Date = LocalDate.now().minusDays(shipmentDaysAgo)
                m.insert(this)
            }
            val k = Random.nextInt(minItems, maxItems + 1)
            var total = BigDecimal.ZERO
            val itemsIDs = mutableListOf<RowID>()
            repeat(k) {
                Shipping_Order_Product().apply { // Create an item
                    var p: Product
                    do { // generate unique item
                        //val m2 = Random.nextInt(maxProduct)
                        // Normal distribution (for a good looking ABC analysis graph)
                        val m2 = ((0.15 * rnd.nextGaussian() + 0.5) * maxProduct).toInt().coerceIn(0, maxProduct - 1)
                        p = products[m2]
                    } while (p.id in itemsIDs)
                    itemsIDs.add(p.id)
                    shipping_Order = o.id
                    product = p.id
                    quantity = Random.nextInt(maxQuantity) + 1
                    val pr = p.price ?: BigDecimal.ZERO
                    val s = quantity.toBigDecimal() * pr
                    price = pr
                    sum = s
                    m.insert(this)
                    total += s
                }
            }
            if (total != BigDecimal.ZERO) {
                o.total = total
                m.update(o)
            }
            if ((i + 1) % 100 == 0) Txt.info("Generated ${i + 1} orders").msg()
        }
        return Text.F("Done")
    }

    @Description("Updates all orders sums")
    fun updateAllOrders() {
        val products = m.selectMap(Product.fId, "")
        var i = 0
        m.iterate<Shipping_Order_Product>("") { item ->
            i++
            with(item) {
                val p = products[product]
                if (p != null) {
                    price = p.price ?: java.math.BigDecimal.ZERO
                    sum = (price?.times(quantity.toBigDecimal()))
                    m.update(item)
                }
                if (i % 100 == 0) com.dolmen.serv.Txt.info("${i}. Updated order item id = ${id}, sum = ${sum}").msg()
            }
        }
    }

}