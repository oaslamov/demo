package com.dlmdemo.demo1_prvt

import com.dolmen.md.demo1_prvt.*
import com.dolmen.serv.CONST
import com.dolmen.serv.Txt
import com.dolmen.serv.anno.Description
import com.dolmen.serv.anno.Parameters
import com.dolmen.serv.table.RowID
import java.math.BigDecimal.ZERO
import java.math.RoundingMode
import java.time.OffsetDateTime
import kotlin.random.Random

class Populate(val m: Demo1) {
    val rnd = java.util.Random()

    fun loadSampleData() {
        val hasData = m.exists<Country>("") || m.exists<Subcountry>("") || m.exists<City>("")
                || m.exists<Product>("") || m.exists<Customer>("")
        if (hasData) {
            Txt.info(m.MID("skip_loading_sample_data")).msg()
        } else {
            m.isLoadingSampleData = true
            Txt.info(m.MID("loading_sample_data")).msg()
            createCities()
            createCustomers()
            createProducts()
            genOrders(1000)
            Stats(m).makeStats(abLimit = 65, bcLimit = 90)
            m.isLoadingSampleData = false
        }
    }

    @Description("Creates products")
    fun createProducts() {
        val maxShippingFrom = 4
        val maxPrice = 30
        //val pathIn = "/product.csv"
        //val lines = javaClass.getResource(pathIn).readText().lines().filterNot { it.isEmpty() }
        val countries = m.selectMap(Country.fId, "")
        PRODUCT_DATASET.forEach { l ->
            Product().apply {
                name = l.trim()
                price = ((Random.nextInt(maxPrice * 100) + 1) / 100.0).toBigDecimal().setScale(2, RoundingMode.HALF_UP)
                product_Type = Product.PRODUCT_TYPE.GROCERY
                m.insert(this)
                countries.keys.shuffled().take(Random.nextInt(maxShippingFrom) + 1)
                    .forEach { countryId ->
                        val rel = Product_Shipping_From()
                        rel.product = this.id
                        rel.shipping_From = countryId
                        m.insert(rel)
                    }
            }
        }
        Txt.info("Created ${PRODUCT_DATASET.size} products").msg()
    }

    @Description("Creates customers")
    fun createCustomers() {
        //val pathIn = "/customer.csv"
        val customersPerCountry = 500
        val countries = m.selectMap(
            Country.fId, "name in ('Australia', 'Canada', 'United Kingdom', 'United States') order by name"
        )
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
                val countryPair = countries[(i - 1) / customersPerCountry]
                country = countryPair.first
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
        COUNTRY_DATASET.forEach { countryName -> // Create countries without cities
            Country().apply {
                name = countryName
                m.insert(this)
            }
        }
        val recs = CITY_DATASET
            .map { l ->
                val rec = l.split(""",(?=(?:[^"]*"[^"]*")*[^"]*$)""".toRegex())
                Rec(
                    city = rec[0].replace("\"", "").take(CONST.MAX_STRING_CHARS),
                    country = rec[1].replace("\"", "").take(CONST.MAX_STRING_CHARS),
                    subcountry = rec[2].replace("\"", "").take(CONST.MAX_STRING_CHARS),
                    geonameid = rec[3].take(CONST.MAX_STRING_CHARS)
                )
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
                    country = countryId
                    m.insert(this)
                    id
                }
                subcountryMap.value.forEach { rec ->
                    City().apply { // Create a city
                        name = rec.city
                        country = countryId
                        subcountry = subcountryId
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
    fun genOrders(n: Int) {
        val maxPeriod = 3L * 365L * 24L * 60L // Three years in minutes
        val maxPaidAfter = 30L
        val maxShipmentAfter = 45L
        val minItems = 3
        val maxItems = 7
        val maxQuantity = 20
        val customers = m.selectMap(Customer.fId, "").values.toList().shuffled()
        val products = m.selectMap(Product.fId, "").values.toList().shuffled()
        val maxCustomer = customers.size
        repeat(n) { i ->
            //val k = Random.nextInt(maxCustomer)      // Use this for uniform distribution
            // Or this for normal distribution (for a good looking ABC analysis graph)
            val k = ((0.15 * rnd.nextGaussian() + 0.5) * maxCustomer).toInt().coerceIn(0, maxCustomer - 1)
            Shipping_Order().apply {
                customer = customers[k].id
                datetime_Order_Placed = OffsetDateTime.now().minusMinutes(Random.nextLong(maxPeriod))
                date_Order_Paid = datetime_Order_Placed?.toLocalDate()?.plusDays(Random.nextLong(maxPaidAfter))
                    ?.coerceAtMost(OffsetDateTime.now().toLocalDate())
                shipment_Date = datetime_Order_Placed?.toLocalDate()?.plusDays(Random.nextLong(maxShipmentAfter))
                comment = makeOrderComment(customers[k])
                m.insert(this)
                genItems(n = Random.nextInt(minItems, maxItems + 1), products, maxQuantity)
            }
            //if ((i + 1) % 100 == 0) Txt.info("Generated ${i + 1} orders").msg()
        }
        Txt.info("Generated $n orders").msg()
    }

    private fun Shipping_Order.genItems(n: Int, products: List<Product>, maxQuantity: Int) {
        val maxProduct = products.size
        val itemsIDs = mutableListOf<RowID>()
        var prevItemOrder = 0.0
        val itemOrderStep = 2000.0
        repeat(n) {
            Shipping_Order_Product().apply { // Create an item
                var p: Product
                do { // generate unique item
                    //val k = Random.nextInt(maxProduct)       // Use this for uniform distribution
                    // Or this for normal distribution (for a good looking ABC analysis graph)
                    val k = ((0.15 * rnd.nextGaussian() + 0.5) * maxProduct).toInt().coerceIn(0, maxProduct - 1)
                    p = products[k]
                } while (p.id in itemsIDs)
                itemsIDs.add(p.id)
                shipping_Order = this@genItems.id
                product = p.id
                quantity = Random.nextInt(maxQuantity) + 1
                price = p.price
                sum = price?.times(quantity.toBigDecimal())
                item_Order = prevItemOrder + itemOrderStep
                prevItemOrder = item_Order
                m.insert(this)
                total = (total ?: ZERO) + (sum ?: ZERO)
            }
        }
        m.update(this)
    }

    private fun makeOrderComment(customer: Customer): String {
        fun mapLink(s: String?): String =
            """<a href="https://maps.google.com/maps?q=$s" target="_blank" rel="noopener noreferrer">$s</a>"""

        fun searchLink(s: String?): String =
            """<a href="https://www.google.com/search?q=$s" target="_blank" rel="noopener noreferrer">$s</a>"""

        with(customer) {
            return """
                <p>
                <ul>
                <li>Find: ${searchLink(name)}</li>
                <li>Open map: ${mapLink(address_Line2)}</li>
                </ul>
                </p>
            """.trimIndent().replace("\n", "")
        }
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
                    price = p.price ?: ZERO
                    sum = (price?.times(quantity.toBigDecimal()))
                    m.update(item)
                }
                if (i % 100 == 0) com.dolmen.serv.Txt.info("${i}. Updated order item id = ${id}, sum = ${sum}").msg()
            }
        }
    }

    fun mailingLabelHtml(
        name: String?, addressLine1: String?, addressLine2: String?,
        addressLine3: String?, country: String?
    ): String {
        return """
            <p>
            <strong>${name?.toUpperCase()}</strong></br>
            $addressLine1</br>
            $addressLine2</br>
            $addressLine3</br>
            <em>${country?.toUpperCase()}</em>
            </p>
        """.trimIndent()
    }

}


