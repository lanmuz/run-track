package com.sdevprem.runtrack.shared.diagnostics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 冷启动后延迟弹出，避免与权限弹窗抢焦点；展示 Manifest 中的高德 Key 与当前签名 SHA1，异常不静默。
 */
@Composable
fun AmapDiagnosticsDialog() {
    val context = LocalContext.current
    var report by remember { mutableStateOf<AmapDiagnosticReport?>(null) }
    var readyToShow by remember { mutableStateOf(false) }
    var dismissed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(900)
        report = withContext(Dispatchers.Default) {
            buildAmapDiagnosticReport(context.applicationContext)
        }
        readyToShow = true
    }

    if (readyToShow && !dismissed && report != null) {
        val r = report!!
        AlertDialog(
            onDismissRequest = { dismissed = true },
            title = {
                Text(
                    if (r.hasError) "高德配置异常（请查看详情）"
                    else "高德 Key / SHA1 自检"
                )
            },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    // 关键修改：包裹 SelectionContainer 使文本可复制
                    SelectionContainer {
                        Text(
                            text = reportText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { dismissed = true }) {
                    Text("知道了")
                }
            },
            // 新增 DismissButton：一键复制整个报告（推荐）
            dismissButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(reportText))
                        // 可选：显示 Toast 提示已复制
                        // Toast.makeText(context, "报告已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        dismissed = true
                    }
                ) {
                    Text("复制报告")
                }
            }
        )
    }
}
