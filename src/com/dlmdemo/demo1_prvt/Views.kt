package com.dlmdemo.demo1_prvt

import com.roofstone.md.demo1_prvt.*
import com.roofstone.serv.conn.SelectedData
import com.roofstone.serv.exp.Formula
import com.roofstone.serv.table.RowID
import com.roofstone.serv.table.RowIDLong
import com.roofstone.ui.rowstyling.Style


class Views(val m: Demo1) {
    fun s_iterateView1(f: Formula): SelectedData<View1> {
        class ViewIterator(f: Formula, m: Demo1) : View1.Data(f, m) {
            val customers: Map<RowID, Customer> = m.selectMap(Customer.fId, "")
            override fun create(s: Shipping_Order): View1 {
                val v = super.create(s)
                if (s.customer != null) {
//                    val c = selectFirst<Customer>("id=${s.customer}")
                    val c = customers[s.customer]
                    if (c != null) {
                        v.c_Name = c.name
                        v.c_Phone = c.phone
                        v.c_Mobile = c.mobile
                        v.c_Address = listOfNotNull(c.address_Line1, c.address_Line2, c.address_Line3).joinToString()
                    }
                }
                return v
            }
        }
        return ViewIterator(f, m)
    }

    fun s_iterateDecor_Test_Card(f: Formula): SelectedData<Decor_Test_Card> {
        class DecorTestCardData(f: Formula?, m: Demo1) : SelectedData<Decor_Test_Card>(f, m) {
            var seqNum: Long = 1
            val colorCodes = listOf(
                "COLOR_DEFAULT", "COLOR_GOOD", "COLOR_ATTENTION", "COLOR_WARNING", "COLOR_ERROR",
                "5", "6", "7", "8", "9",
                "CONTRAST100", "CONTRAST95", "CONTRAST90", "CONTRAST80", "CONTRAST60", "CONTRAST50",
                "CONTRAST40", "CONTRAST20", "CONTRAST00", "19",
                "COLOR_RED", "COLOR_GREEN", "COLOR_BLUE", "COLOR_YELLOW", "COLOR_BLACK", "COLOR_WHITE"
            )
            val fontCodes = listOf(
                "FONT_NORMAL", "FONT_ITALIC", "FONT_BOLD", "FONT_ITALIC+FONT_BOLD",
                "FONT_STRIKETHROUGH", "FONT_ITALIC+FONT_STRIKETHROUGH", "FONT_BOLD+FONT_STRIKETHROUGH",
                "FONT_ITALIC+FONT_BOLD+FONT_STRIKETHROUGH"
            )
            val alignCodes = listOf("ALIGN_DEFAULT", "ALIGN_LEFT", "ALIGN_RIGHT", "ALIGN_CENTER")
            val MAX_COLOR = 26
            val MAX_FONT = 8
            val MAX_ALIGN = 4
            val MAX_ROW = MAX_COLOR * MAX_COLOR * MAX_ALIGN * MAX_FONT
            override fun hasNext(): Boolean {
                if (table != null) return true
                while (seqNum <= MAX_ROW) {
                    table = Decor_Test_Card()
                    val fgColor = ((seqNum - 1) % MAX_COLOR).toInt()
                    val bgColor = (((seqNum - 1) / MAX_COLOR) % MAX_COLOR).toInt()
                    val font = (((seqNum - 1) / (MAX_COLOR * MAX_COLOR)) % MAX_FONT).toInt()
                    val align = (((seqNum - 1) / (MAX_COLOR * MAX_COLOR * MAX_FONT)) % MAX_ALIGN + 1).toInt()
                    table.fg_Color = fgColor
                    table.bg_Color = bgColor
                    table.font = font
                    table.align = align
                    table.style =
                        "${colorCodes[fgColor]}, ${colorCodes[bgColor]}, ${fontCodes[font]}, ${alignCodes[align - 1]}"
                    table.sample_Text = LOREM.take(50)

                    val decorData = Decor_Decor_Test_Card_Formatting.newData()
                    val style = Style().apply {
                        color(fgColor)
                        bgColor(bgColor)
                        font(font)
                        align(align)
                    }
                    decorData.set(Decor_Decor_Test_Card_Formatting.RowDefault, style)
                    table.formatting = decorData

                    table.i_setId(RowIDLong.get(seqNum++))
                    if (isRowFiltered(table)) return true
                }
                return false
            }
        }
        return DecorTestCardData(f, m)
    }
}

