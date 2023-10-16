package com.roofstone.md.demo1_prvt
import com.roofstone.serv.Module
import com.roofstone.serv.Module.start
import com.roofstone.serv.ModuleType
import com.roofstone.serv.conn.SelectedData
import com.roofstone.serv.exp.Formula
import com.roofstone.serv.table.TopTable
import kotlin.reflect.KClass

fun <T : TopTable> Module.iterate(tClass: KClass<T>, filter: String): SelectedData<T> {
    return iterate(tClass.java, filter)
}

fun <T : TopTable> Module.selectFirst(tClass: KClass<T>, filter: String): T? {
    return selectFirst(tClass.java, filter)
}

fun <T : TopTable> Module.exists(tClass: KClass<T>, filter: String): Boolean {
    return exists(tClass.java, filter)
}

fun <T : TopTable> Module.count(tClass: KClass<T>, filter: String): Long {
    return count(tClass.java, filter)
}

fun <M : Module> start(moduleClass: KClass<M>): M {
    return start(moduleClass.java)
}

fun ModuleType.registerEvents(eventClass: KClass<*>) {
    registerEvents(eventClass.java)
}

fun <T: Any> Module.publish(eventClass: KClass<T>): T {
    return publish(eventClass.java)
}

inline fun <reified Tbl: TopTable> Module.iterate(filter: String, noinline body: (rec:Tbl)->Unit) {
    iterate(Tbl::class,filter).iterator().use {iter ->
        iter.forEach { body(it) }
    }
}

inline fun <reified Tbl: TopTable> Module.iterateExit(filter: String, noinline body: (rec:Tbl)->Boolean) {
    iterate(Tbl::class,filter).iterator().use {iter ->
        iter.forEach { if (!body(it)) return }
    }
}

inline fun <reified Tbl: TopTable> Module.exists(filter: String): Boolean {
    return exists(Tbl::class, filter)
}

inline fun <reified Tbl: TopTable> Module.selectFirst(filter: String): Tbl? {
    return selectFirst(Tbl::class, filter)
}

inline fun <reified Tbl: TopTable> Module.iterate(formula: Formula, noinline body: (rec:Tbl)->Unit) {
    iterate(formula).iterator().use { iter ->
        iter.forEach { body(it as Tbl) }
    }
}

inline fun <reified Tbl: TopTable> Module.iterateExit(formula: Formula, noinline body: (rec:Tbl)->Boolean) {
    iterate(formula).iterator().use {iter ->
        iter.forEach { if (!body(it as Tbl)) return }
    }
}
