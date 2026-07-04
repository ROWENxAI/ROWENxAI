// Copyright 2026 PokeClaw (agents.io). All rights reserved.
// Licensed under the Apache License, Version 2.0.

package io.agents.pokeclaw.agent

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import io.agents.pokeclaw.utils.XLog

/**
 * TaskShortcuts — instant Android intent execution without going through the LLM agent loop.
 *
 * Call [tryExecute] before starting the agent pipeline. If the user's task matches a known
 * shortcut, it is executed immediately (< 200ms) and a result message is returned.
 * Returns null when no shortcut matches, meaning the caller should proceed with the normal
 * agent pipeline.
 */
object TaskShortcuts {

    private const val TAG = "TaskShortcuts"

    // ── Known app package names ──────────────────────────────────────────────

    private val KNOWN_PACKAGES = mapOf(
        "youtube"    to "com.google.android.youtube",
        "whatsapp"   to "com.whatsapp",
        "instagram"  to "com.instagram.android",
        "chrome"     to "com.android.chrome",
        "gmail"      to "com.google.android.gm",
        "maps"       to "com.google.android.apps.maps",
        "google map" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "spotify"    to "com.spotify.music",
        "netflix"    to "com.netflix.mediaclient",
        "tiktok"     to "com.zhiliaoapp.musically",
        "twitter"    to "com.twitter.android",
        "x"          to "com.twitter.android",
        "facebook"   to "com.facebook.katana",
        "telegram"   to "org.telegram.messenger",
        "snapchat"   to "com.snapchat.android",
        "discord"    to "com.discord",
        "linkedin"   to "com.linkedin.android",
        "uber"       to "com.ubercab",
        "lyft"       to "com.lyft.android",
        "amazon"     to "com.amazon.mShop.android.shopping",
        "ebay"       to "com.ebay.mobile",
        "zoom"       to "us.zoom.videomeetings",
        "slack"      to "com.Slack",
        "calculator" to "com.google.android.calculator",
        "clock"      to "com.google.android.deskclock",
        "calendar"   to "com.google.android.calendar",
        "photos"     to "com.google.android.apps.photos",
        "google photo" to "com.google.android.apps.photos",
        "google photos" to "com.google.android.apps.photos",
        "files"      to "com.google.android.documentsui",
        "phone"      to "com.google.android.dialer",
        "messages"   to "com.google.android.apps.messaging",
        "contacts"   to "com.google.android.contacts",
        "music"      to "com.google.android.music",
        "play store" to "com.android.vending",
        "playstore"  to "com.android.vending",

        // ── 国内主流应用 ──
        // 社交 / 通讯
        "微信"        to "com.tencent.mm",
        "wechat"      to "com.tencent.mm",
        "qq"          to "com.tencent.mobileqq",
        "微博"        to "com.sina.weibo",
        "weibo"       to "com.sina.weibo",
        "小红书"      to "com.xingin.xhs",
        "xiaohongshu" to "com.xingin.xhs",
        "rednote"     to "com.xingin.xhs",
        "钉钉"        to "com.alibaba.android.rimet",
        "dingtalk"    to "com.alibaba.android.rimet",
        "飞书"        to "com.ss.android.lark",
        "feishu"      to "com.ss.android.lark",
        "lark"        to "com.ss.android.lark",
        "企业微信"    to "com.tencent.wework",
        "wecom"       to "com.tencent.wework",

        // 短视频
        "抖音"        to "com.ss.android.ugc.aweme",
        "douyin"      to "com.ss.android.ugc.aweme",
        "快手"        to "com.smile.gifmaker",
        "kuaishou"    to "com.smile.gifmaker",

        // 视频
        "b站"         to "tv.danmaku.bili",
        "bilibili"    to "tv.danmaku.bili",
        "哔哩哔哩"    to "tv.danmaku.bili",
        "爱奇艺"      to "com.qiyi.video",
        "iqiyi"       to "com.qiyi.video",
        "优酷"        to "com.youku.phone",
        "youku"       to "com.youku.phone",
        "腾讯视频"    to "com.tencent.qqlive",

        // 音乐
        "网易云音乐"  to "com.netease.cloudmusic",
        "netease music" to "com.netease.cloudmusic",
        "qq音乐"      to "com.tencent.qqmusic",
        "qq music"    to "com.tencent.qqmusic",

        // 购物
        "淘宝"        to "com.taobao.taobao",
        "taobao"      to "com.taobao.taobao",
        "京东"        to "com.jingdong.app.mall",
        "jd"          to "com.jingdong.app.mall",
        "拼多多"      to "com.xunmeng.pinduoduo",
        "pinduoduo"   to "com.xunmeng.pinduoduo",
        "美团"        to "com.sankuai.meituan",
        "meituan"     to "com.sankuai.meituan",
        "饿了么"      to "me.ele",
        "eleme"       to "me.ele",

        // 支付 / 金融
        "支付宝"      to "com.eg.android.AlipayGphone",
        "alipay"      to "com.eg.android.AlipayGphone",

        // 出行
        "滴滴"        to "com.sdu.didi.psnger",
        "didi"        to "com.sdu.didi.psnger",
        "高德地图"    to "com.autonavi.minimap",
        "高德"        to "com.autonavi.minimap",
        "amap"        to "com.autonavi.minimap",
        "百度地图"    to "com.baidu.BaiduMap",

        // 搜索 / 浏览器
        "百度"        to "com.baidu.searchbox",
        "baidu"       to "com.baidu.searchbox",
        "夸克"        to "com.quark.browser",
        "quark"       to "com.quark.browser",
        "uc浏览器"    to "com.UCMobile",
        "uc"          to "com.UCMobile",

        // 办公 / 邮箱
        "wps"         to "cn.wps.moffice_eng",
        "网易邮箱"    to "com.netease.mail",
        "163邮箱"     to "com.netease.mail",
        "qq邮箱"      to "com.tencent.androidqqmail",
        "outlook"     to "com.microsoft.office.outlook",

        // ── 社交 / 交友 ──
        "soul"        to "cn.soulapp.android",
        "陌陌"        to "com.immomo.momo",
        "momo"        to "com.immomo.momo",
        "探探"        to "com.p1.mobile.putong",
        "tantan"      to "com.p1.mobile.putong",
        "连信"        to "com.lianxin.main",
        "lianxin"     to "com.lianxin.main",
        "青藤之恋"    to "com.qingteng.love",
        "uki"         to "com.wemomo.uki",
    )

    // ── Torch state ──────────────────────────────────────────────────────────

    private var torchEnabled = false

    // ── Public entry point ───────────────────────────────────────────────────

    /**
     * Try to execute [task] as an instant shortcut.
     *
     * @return A user-visible result string if the shortcut was matched and executed (or
     *         attempted), or null if no shortcut matched.
     */
    fun tryExecute(context: Context, task: String): String? {
        val t = task.trim().lowercase()

        // --- Open camera ---
        if (t.contains("open camera") || t == "camera") {
            return openCamera(context)
        }

        // --- Open settings ---
        if (t.contains("open settings") || t == "settings" || t.contains("go to settings")) {
            return openSettings(context)
        }

        // --- Flashlight / torch ---
        if (t.contains("flashlight") || t.contains("torch")) {
            return when {
                t.contains(" on") || t.startsWith("turn on") || t.startsWith("enable") || t.contains("on flashlight") || t.contains("on torch") ->
                    setTorch(context, true)
                t.contains(" off") || t.startsWith("turn off") || t.startsWith("disable") || t.contains("off flashlight") || t.contains("off torch") ->
                    setTorch(context, false)
                // Toggle if state is ambiguous
                else -> setTorch(context, !torchEnabled)
            }
        }

        // --- Take screenshot ---
        if (t.contains("take screenshot") || t.contains("screenshot") || t == "screenshot") {
            return takeScreenshot(context)
        }

        // --- Go home ---
        if (t == "go home" || t == "home" || t.contains("press home") || t.contains("go to home")) {
            return pressHome(context)
        }

        // --- Go back ---
        if (t == "go back" || t == "back" || t.contains("press back")) {
            return pressBack(context)
        }

        // --- Open [known app by keyword] ---
        // Check known packages map first (exact keyword match inside the task string)
        for ((keyword, pkg) in KNOWN_PACKAGES) {
            if (t.contains(keyword)) {
                return launchPackage(context, pkg, keyword)
            }
        }

        // --- Open [app name] — fuzzy search installed packages ---
        if (t.startsWith("open ")) {
            val appName = t.removePrefix("open ").trim()
            if (appName.isNotEmpty()) {
                return launchAppByName(context, appName)
            }
        }
        if (t.startsWith("launch ")) {
            val appName = t.removePrefix("launch ").trim()
            if (appName.isNotEmpty()) {
                return launchAppByName(context, appName)
            }
        }
        if (t.startsWith("start ")) {
            val appName = t.removePrefix("start ").trim()
            if (appName.isNotEmpty()) {
                return launchAppByName(context, appName)
            }
        }

        return null // no shortcut matched
    }

    // ── Shortcut implementations ─────────────────────────────────────────────

    private fun openCamera(context: Context): String {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            XLog.i(TAG, "Shortcut: opened camera")
            "Camera opened."
        } catch (e: Exception) {
            XLog.w(TAG, "Shortcut: camera failed — ${e.message}")
            "Could not open camera: ${e.message}"
        }
    }

    private fun openSettings(context: Context): String {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            XLog.i(TAG, "Shortcut: opened settings")
            "Settings opened."
        } catch (e: Exception) {
            XLog.w(TAG, "Shortcut: settings failed — ${e.message}")
            "Could not open settings: ${e.message}"
        }
    }

    private fun setTorch(context: Context, on: Boolean): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            // Find a camera that has a flash unit
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId == null) {
                XLog.w(TAG, "Shortcut: no flash unit found")
                return "This device has no flashlight."
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, on)
                torchEnabled = on
                val state = if (on) "on" else "off"
                XLog.i(TAG, "Shortcut: flashlight $state")
                "Flashlight turned $state."
            } else {
                "Flashlight control requires Android 6.0 or above."
            }
        } catch (e: Exception) {
            XLog.w(TAG, "Shortcut: torch failed — ${e.message}")
            "Could not control flashlight: ${e.message}"
        }
    }

    private fun takeScreenshot(context: Context): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Request screenshot via accessibility service if available
                val svc = io.agents.pokeclaw.service.ClawAccessibilityService.getInstance()
                if (svc != null) {
                    svc.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                    XLog.i(TAG, "Shortcut: screenshot via accessibility service")
                    "Screenshot taken."
                } else {
                    XLog.w(TAG, "Shortcut: accessibility service not running for screenshot")
                    "Screenshot requires Accessibility permission. Please enable PokeClaw in Accessibility settings."
                }
            } else {
                "Screenshot shortcut requires Android 9.0 or above."
            }
        } catch (e: Exception) {
            XLog.w(TAG, "Shortcut: screenshot failed — ${e.message}")
            "Could not take screenshot: ${e.message}"
        }
    }

    private fun pressHome(context: Context): String {
        return try {
            val svc = io.agents.pokeclaw.service.ClawAccessibilityService.getInstance()
            if (svc != null) {
                svc.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
                XLog.i(TAG, "Shortcut: pressed Home")
                "Went home."
            } else {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                XLog.i(TAG, "Shortcut: pressed Home via intent")
                "Went home."
            }
        } catch (e: Exception) {
            XLog.w(TAG, "Shortcut: home failed — ${e.message}")
            "Could not go home: ${e.message}"
        }
    }

    private fun pressBack(context: Context): String {
        return try {
            val svc = io.agents.pokeclaw.service.ClawAccessibilityService.getInstance()
            if (svc != null) {
                svc.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
                XLog.i(TAG, "Shortcut: pressed Back")
                "Went back."
            } else {
                "Back shortcut requires Accessibility permission. Please enable PokeClaw in Accessibility settings."
            }
        } catch (e: Exception) {
            XLog.w(TAG, "Shortcut: back failed — ${e.message}")
            "Could not go back: ${e.message}"
        }
    }

    private fun launchPackage(context: Context, packageName: String, label: String): String {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                ?: return "App \"$label\" is not installed."
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            XLog.i(TAG, "Shortcut: launched $label ($packageName)")
            "Opened $label."
        } catch (e: Exception) {
            XLog.w(TAG, "Shortcut: launch $packageName failed — ${e.message}")
            "Could not open $label: ${e.message}"
        }
    }

    private fun launchAppByName(context: Context, appName: String): String? {
        return try {
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            // Score each app: 2 points for exact match, 1 for contains
            val candidate = installedApps
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .mapNotNull { info ->
                    val label = pm.getApplicationLabel(info).toString().lowercase()
                    val score = when {
                        label == appName -> 2
                        label.contains(appName) || appName.contains(label) -> 1
                        else -> 0
                    }
                    if (score > 0) Pair(score, info) else null
                }
                .maxByOrNull { it.first }
                ?.second

            if (candidate == null) {
                XLog.i(TAG, "Shortcut: no app found for \"$appName\"")
                return null // no match; caller proceeds with agent pipeline
            }

            val displayLabel = pm.getApplicationLabel(candidate).toString()
            val intent = pm.getLaunchIntentForPackage(candidate.packageName)!!
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            XLog.i(TAG, "Shortcut: launched \"$displayLabel\" (${candidate.packageName}) for query \"$appName\"")
            "Opened $displayLabel."
        } catch (e: Exception) {
            XLog.w(TAG, "Shortcut: launchAppByName \"$appName\" failed — ${e.message}")
            "Could not open \"$appName\": ${e.message}"
        }
    }
}
