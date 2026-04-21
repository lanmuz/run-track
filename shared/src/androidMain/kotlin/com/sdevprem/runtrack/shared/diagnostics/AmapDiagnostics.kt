package com.sdevprem.runtrack.shared.diagnostics

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * 高德控制台绑定的 Debug 证书 SHA1（用户提供的值），用于运行时对比。
 * 若你更换签名，请同步更新此处或改为从配置读取。
 */
private const val EXPECTED_DEBUG_SHA1 =
    "85:27:54:98:89:11:17:94:71:61:2A:BE:E4:9D:6E:07:83:F9:EA:6F"

/** 你在工单中声明应生效的 Key（与 Manifest 合并结果对比，便于确认是否“真用上”）。 */
private const val USER_SUPPLIED_EXPECTED_AMAP_KEY =
    "f7ed794f43e9f49b9ff09f50b6d2a0b4"

private const val META_AMAP_KEY = "com.amap.api.v2.apikey"

private fun normalizeSha1(s: String): String =
    s.uppercase().replace(":", "").replace(" ", "")

private fun formatSha1ColonUpper(bytes: ByteArray): String =
    bytes.joinToString(":") { String.format("%02X", it) }

/**
 * 从已合并的 Manifest 读取高德 Key（与运行时 SDK 使用的一致）。
 */
fun readAmapKeyFromMergedManifest(context: Context): String? =
    try {
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        appInfo.metaData?.getString(META_AMAP_KEY)
    } catch (e: Exception) {
        null
    }

/**
 * 当前安装包签名对应的 SHA1（与高德控制台「SHA1」填写格式一致，带冒号大写）。
 */
fun readCurrentSigningSha1Colon(context: Context): Result<String> =
    runCatching {
        val pm = context.packageManager
        val pkg = context.packageName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val pi = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
            val signers = pi.signingInfo?.apkContentsSigners
            require(!signers.isNullOrEmpty()) { "signingInfo.apkContentsSigners 为空" }
            val md = MessageDigest.getInstance("SHA1")
            md.update(signers[0].toByteArray())
            formatSha1ColonUpper(md.digest())
        } else {
            @Suppress("DEPRECATION")
            val pi = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            val sigs = pi.signatures
            require(!sigs.isNullOrEmpty()) { "GET_SIGNATURES 为空" }
            val md = MessageDigest.getInstance("SHA1")
            md.update(sigs[0].toByteArray())
            formatSha1ColonUpper(md.digest())
        }
    }

data class AmapDiagnosticReport(
    val lines: List<String>,
    val hasError: Boolean,
)

/**
 * 生成自检报告：Key 是否写入 Manifest、是否与占位符区分、SHA1 是否与预期一致。
 * 任何读取异常都会体现在 [hasError] 与文案中，不静默吞掉。
 */
fun buildAmapDiagnosticReport(context: Context): AmapDiagnosticReport {
    val lines = mutableListOf<String>()
    var hasError = false

    val key = readAmapKeyFromMergedManifest(context)
    when {
        key.isNullOrBlank() -> {
            hasError = true
            lines += "【错误】Manifest 中未读取到 meta-data「$META_AMAP_KEY」或值为空。"
            lines += "请检查 app 模块是否通过 Gradle 正确注入 MAPS_API_KEY（local.properties / CI 环境变量）。"
        }

        key.contains("\${") || key == "\${MAPS_API_KEY}" -> {
            hasError = true
            lines += "【错误】Key 仍为占位符未替换：「$key」"
            lines += "说明构建时 MAPS_API_KEY 未注入，高德不会下发瓦片（常见为白底只显示 logo/比例尺）。"
        }

        key == "CI_PLACEHOLDER" -> {
            hasError = true
            lines += "【错误】当前 Key 为 CI 占位符：CI_PLACEHOLDER"
            lines += "请在 CI 的 local.properties 或 secrets 中写入真实高德 Key 后重编。"
        }

        else -> {
            lines += "【Key】已从 Manifest 读取（与 SDK 使用一致）："
            lines += key
            lines += ""
            if (key == USER_SUPPLIED_EXPECTED_AMAP_KEY) {
                lines += "【Key 对比】与工单预期字符串一致 ✓"
            } else {
                hasError = true
                lines += "【错误】Key 与工单预期字符串不一致 ✗"
                lines += "工单预期：$USER_SUPPLIED_EXPECTED_AMAP_KEY"
                lines += "请检查 local.properties / CI 注入的 MAPS_API_KEY 是否指向同一把 Key，并重新安装当前 APK。"
            }
        }
    }

    lines += ""
    lines += "【预期 SHA1】（高德控制台绑定 Debug 证书）"
    lines += EXPECTED_DEBUG_SHA1

    readCurrentSigningSha1Colon(context).fold(
        onFailure = { e ->
            hasError = true
            lines += ""
            lines += "【错误】读取安装包签名 SHA1 失败：${e.message ?: "未知异常"}"
            lines += (e.stackTraceToString().take(1200))
        },
        onSuccess = { actual ->
            lines += ""
            lines += "【当前运行 SHA1】"
            lines += actual
            val match =
                normalizeSha1(actual) == normalizeSha1(EXPECTED_DEBUG_SHA1)
            lines += ""
            if (match) {
                lines += "【SHA1】与预期一致 ✓"
            } else {
                hasError = true
                lines += "【错误】SHA1 与预期不一致 ✗"
                lines += "若高德控制台只绑定了上述「预期 SHA1」，当前包将无法拉取街道瓦片。"
                lines += "请在控制台增加本机 SHA1，或使用与控制台一致的签名重新打包。"
            }
        }
    )

    if (!hasError && key != null && key.isNotBlank()) {
        lines += ""
        lines += "【说明】若仍白底有 logo："
        lines += "1) 确认高德控制台 Key 类型为 Android、包名 ${context.packageName}；"
        lines += "2) 确认已调用隐私合规（本应用已在 Application 中 setMapPrivacy）；"
        lines += "3) 网络是否可访问高德瓦片服务。"
    }

    return AmapDiagnosticReport(lines = lines, hasError = hasError)
}
