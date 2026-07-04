// Copyright 2026 PokeClaw (agents.io). All rights reserved.
// Licensed under the Apache License, Version 2.0.

package io.agents.pokeclaw.ui.chat

data class MonitorTargetSpec(
    val label: String,
    val app: String,
    val tone: String = "Casual",
) {
    val displayLabel: String
        get() = "$label on $app"

    companion object {
        val supportedApps = listOf("WhatsApp", "Telegram", "Messages", "LINE", "WeChat", "QQ", "DingTalk", "Feishu", "WeCom", "Weibo", "Soul", "Momo", "Tantan")
    }
}

object MonitorTargetParser {
    fun fromTaskText(text: String): MonitorTargetSpec? {
        val app = extractApp(text)
        val label = extractLabel(text)
        if (label.isEmpty()) {
            return null
        }
        return MonitorTargetSpec(label = label, app = app)
    }

    private fun extractApp(text: String): String {
        val lower = text.lowercase()
        return when {
            Regex("""\btelegram\b""").containsMatchIn(lower) -> "Telegram"
            Regex("""\bwe\s*chat\b""").containsMatchIn(lower) -> "WeChat"
            Regex("""\bline\b""").containsMatchIn(lower) -> "LINE"
            Regex("""\b(messages|google messages|sms)\b""").containsMatchIn(lower) -> "Messages"
            else -> "WhatsApp"
        }
    }

    private fun extractLabel(text: String): String {
        val lower = text.lowercase()
        var cleaned = lower
        val removeWords = listOf(
            "monitoring", "monitor", "auto-reply", "auto reply", "watching", "watch",
            "on whatsapp", "on telegram", "on messages", "on google messages", "on sms", "on wechat", "on line", "on qq", "on dingtalk", "on feishu", "on wecom", "on weibo",
            "在微信", "在qq", "在钉钉", "在飞书", "在微博", "在soul", "在陌陌", "在探探",
            "messages", "message", "'s", "'s", "for", "from",
            "please", "can you", "start", "enable", "begin", "help me",
        )
        for (word in removeWords) {
            cleaned = cleaned.replace(word, " ")
        }
        cleaned = cleaned.replace(Regex("\\s+"), " ").trim()
        return if (cleaned.isNotEmpty()) cleaned.replaceFirstChar { it.uppercase() } else ""
    }
}
