package com.absinthe.anywhere_.utils

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast

object IntentAmConverter {
    private const val TAG = "IntentAmConverter"

    // 入口：自动转am命令 + 复制
    fun convertAndCopy(ctx: Context, intent: Intent) {
        val amCmd = convert(intent)
    }

    // 纯转换逻辑
    fun convert(intent: Intent): String {
        val subCmd = autoDetectSubCommand(intent)
        return buildAmCommand(subCmd, intent)
    }

    // 自动识别 start / broadcast / startservice
    private fun autoDetectSubCommand(intent: Intent): String {
        val comp = intent.component
        if (comp != null) {
            val cls = comp.className
            if (cls.endsWith("Service")) return "startservice"
        }
        if (intent.component == null && intent.action != null) {
            return "broadcast"
        }
        return "start"
    }

    private fun buildAmCommand(cmd: String, intent: Intent): String {
        val sb = StringBuilder("am $cmd ")
        intent.action?.let { sb.append("-a $it ") }
        intent.data?.let { sb.append("-d $it ") }
        intent.type?.let { sb.append("-t $it ") }
        intent.categories?.forEach { sb.append("-c $it ") }
        if (intent.flags != 0) sb.append("-f ${intent.flags} ")
        intent.`package`?.let { sb.append("-p $it ") }
        intent.component?.let {
            sb.append("-n ${it.packageName}/${it.className.replace("$", "\\$")} ")
        }
        appendExtras(sb, intent.extras)

        return sb.toString().trim()
    }

    private fun appendExtras(sb: StringBuilder, extras: Bundle?) {
        extras ?: return
        for (key in extras.keySet()) {
            val value = extras.get(key) ?: continue
            when (value) {
                is String -> sb.append("--e $key \"$value\" ")
                is Int -> sb.append("--ei $key $value ")
                is Long -> sb.append("--el $key $value ")
                is Boolean -> sb.append("--ez $key $value ")
                is Float -> sb.append("--ef $key $value ")
                is Double -> sb.append("--ed $key $value ")
                is Array<*> -> if (value.isArrayOf<String>()) {
                    val arr = value as Array<String>
                    sb.append("--esa $key \"${arr.joinToString(",")}\" ")
                }
                is IntArray -> sb.append("--eia $key \"${value.joinToString(",")}\" ")
                is LongArray -> sb.append("--ela $key \"${value.joinToString(",")}\" ")
            }
        }
    }

    // 复制到剪贴板
    private fun copyToClip(ctx: Context, text: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("am_cmd", text))
    }
}