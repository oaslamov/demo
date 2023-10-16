package com.dlmdemo.demo1_prvt

import com.roofstone.call.ActionBase
import com.roofstone.call.Http
import com.roofstone.call.JSONManagerBase
import com.roofstone.serv.anno.Description
import org.mpru.security.KerberosPrefs

class Caller(val m: Demo1) {
    @Description("Calls server (JSON)")
    fun callServerJson(): String {
        var res = ""
        val url = "https://system.corp.example.com/mainsrv"
        val spn = url.toSpn()
        val http = Http(url)
        val kerbPrefs = KerberosPrefs()
        //kerbPrefs.setUsername("dora@CORP.EXAMPLE.COM")        // Kerberos Username and Password
        //kerbPrefs.setPassword("Pass123456")                   // Kerberos Username and Password
        kerbPrefs.setPrincipal("HTTP/dlm2.corp.example.com@CORP.EXAMPLE.COM")         // Kerberos Principal and Keytab
        kerbPrefs.setKtab("C:/roofstone/Workspace/webserver/webapps/mainsrv/mainsrv.ktab") // Kerberos Principal and Keytab
        kerbPrefs.setSpn(spn)
        http.setKerberosClient(kerbPrefs)
        http.setLog(m.l())
        val ac = ActionBase.create(
            "demo1_prvt.selectlist", "demo1_prvt.customer",
            "name like '%val%' order by name"
        ).setTag("myTag1")
        var ar = http.action(ac)
        while (ar != null) {
            res += JSONManagerBase.getJson(ar, false) + '\n'
            ar = ar.next
        }
        res += "\nRC == ${http.rc()}"
        return res
    }

    @Description("Calls server (XML)")
    fun callServerXml(): String {
        val url = "https://system.corp.example.com/mainsrv"
        val http = Http(url)
        val spn = url.toSpn()
        val kerbPrefs = KerberosPrefs()
        //kerbPrefs.setUsername("dora@CORP.EXAMPLE.COM")    // Kerberos Username and Password
        //kerbPrefs.setPassword("Pass123456")               // Kerberos Username and Password
        kerbPrefs.setPrincipal("HTTP/dlm2.corp.example.com@CORP.EXAMPLE.COM")         // Kerberos Principal and Keytab
        kerbPrefs.setKtab("C:/roofstone/Workspace/webserver/webapps/mainsrv/mainsrv.ktab") // Kerberos Principal and Keytab
        kerbPrefs.setSpn(spn)
        http.setKerberosClient(kerbPrefs)
        http.setLog(m.l())
        var res = http.sendPost(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <roofstone version="1">
            <a a="demo1_prvt.action1">
                <arg name="customerFilter">name like '%val%'</arg>
            </a>
            </roofstone>
        """.trimIndent()
        )
        res += "\nRC == ${http.rc()}\n"
        return res
    }

    private fun String.toSpn(): String = this.replace(Regex(".*//(.*)/.*"), "HTTP/$1")
}