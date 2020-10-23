package com.demo1_prvt

import com.dolmen.md.demo1_prvt.Demo1_PrvtModuleBase
import com.dolmen.util.Text

class MyModule : Demo1_PrvtModuleBase() {

    fun myAction(input: String): String{
        return Text.F("\n-------------\nTest OK (input: $0)\n--------------", input)
    }
}