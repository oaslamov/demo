package com.dlmdemo.demo1_prvt

import com.dolmen.md.demo1_prvt.Shipping_Order_Product
import com.dolmen.md.demo1_prvt.iterate
import com.dolmen.serv.Txt
import com.dolmen.serv.exp.Formula
import java.sql.Connection
import java.sql.DriverManager
import kotlin.system.measureTimeMillis

class TestPerf(val m: Demo1) {
    fun testQuery(n: Int, dbUrl: String, dbUser: String, dbPass: String, dbSchema: String, fetchSize: Int) {

        val t0 = runJdbc(n, dbUrl, dbUser, dbPass, dbSchema, fetchSize)
        val t1 = runSelectMap(n, fetchSize)
        val t2 = runIterate(n, fetchSize)
        val tMin = minOf(t0, t1, t2).coerceAtLeast(1)

        val report = "$0 (executed $n times): $1ms, $2%"

        Txt.info(report, "jdbc", t0, t0 * 100 / tMin).msg()
        Txt.info(report, "selectMap", t1, t1 * 100 / tMin).msg()
        Txt.info(report, "iterate", t2, t2 * 100 / tMin).msg()
    }

    private fun runIterate(n: Int, fetchSize: Int): Long {
        val f = Formula.parse("", Shipping_Order_Product.T)
        f.expectedRows = fetchSize
        return measureTimeMillis {
            repeat(n) {
                m.iterate<Shipping_Order_Product>(f) {
                    val noop = null //breakpoint
                }
            }
        }
    }

    private fun runJdbc(n: Int, dbUrl: String, dbUser: String, dbPass: String, dbSchema: String, fetchSize: Int): Long {
        val dbTable = "shipping_order_product__demo1_prvt"
        val q = "SELECT t.id, t.ver, t.sver, t.data FROM $dbSchema.$dbTable t order by t.id;"
        var connection: Connection? = null
        try {
            connection = DriverManager.getConnection(dbUrl, dbUser, dbPass)
        } catch (ex: Exception) {
            Txt.error("Connection error: ${ex.message}").msg()
        }
        if (connection == null) return -1
        return measureTimeMillis {
            try {
                connection.autoCommit = false
                val query = connection.prepareStatement(q)
                query.fetchSize = fetchSize
                repeat(n) {
                    val result = query.executeQuery()
                    while (result.next()) {
                        with(result) {
                            val id = getInt("id")
                            val ver = getInt("ver")
                            val sver = getInt("sver")
                            val data = getString("data")
                            val noop = null //breakpoint
                        }
                    }
                }
            } catch (ex: Exception) {
                Txt.error("Query error: ${ex.message}").msg()
            } finally {
                connection.close()
            }
        }
    }

    fun runSelectMap(n: Int, fetchSize: Int): Long {
        val f = Formula.parse("", Shipping_Order_Product.T)
        f.expectedRows = fetchSize
        return measureTimeMillis {
            repeat(n) {
                m.selectMap(Shipping_Order_Product.fId, f)
            }
        }
    }
}