package app.flenimg

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppHolder.context = applicationContext
        setContent { FlenApp() }
    }
}

@Composable
private fun FlenApp() {
    var input by remember { mutableStateOf<Uri?>(null) }
    var target by remember { mutableStateOf("1080p") }
    var preset by remember { mutableStateOf("superhd") }
    var mode by remember { mutableStateOf("auto") }
    var effects by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ready") }
    var expanded by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { input = it }

    MaterialTheme {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Flen.img", style = MaterialTheme.typography.headlineLarge)
            Text("Fast local image & video enhancement", style = MaterialTheme.typography.titleMedium)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Input", style = MaterialTheme.typography.titleLarge)
                    OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }) { Text(if (input == null) "Choose image / video" else "Change file") }
                    Text(input?.toString() ?: "No local file selected")
                    TextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("Or paste a media URL") }, singleLine = true)
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enhancement", style = MaterialTheme.typography.titleLarge)
                    Box {
                        OutlinedButton(onClick = { expanded = true }, Modifier.fillMaxWidth()) { Text("Preset: $preset") }
                        DropdownMenu(expanded, { expanded = false }) {
                            presets.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { preset = item; expanded = false }) }
                        }
                    }
                    TextField(target, { target = it }, Modifier.fillMaxWidth(), label = { Text("Output size") }, singleLine = true)
                    TextField(mode, { mode = it }, Modifier.fillMaxWidth(), label = { Text("Mode: auto / image / video") }, singleLine = true)
                    TextField(effects, { effects = it }, Modifier.fillMaxWidth(), label = { Text("Effects") }, singleLine = true)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { status = EngineBridge.start(input, url, target, preset, mode, effects) }, Modifier.weight(1f)) { Text("Enhance") }
                OutlinedButton(onClick = { status = EngineBridge.doctor() }, Modifier.weight(1f)) { Text("Device check") }
            }
            Text(status)
            Text("Local-first. Hardware acceleration is used when available.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private val presets = listOf(
    "superhd", "hq-hevc", "hdr10", "raw", "y4m", "lossless-x264", "lossless-x265",
    "prores4444", "dnxhr", "av1", "ai-realesrgan-x4plus", "ai-realesrgan-anime-x4",
    "ai-realesrgan-video-x4", "ai-mmagic-realesrgan-x4", "ai-mmagic-swinir-x4",
    "ai-mmagic-basicvsr-x4", "ai-mmagic-realbasicvsr-x4", "ai-img-mmagic-x4", "ai-img-realesrgan-x4"
)

private object EngineBridge {
    private fun quote(value: String) = "'" + value.replace("'", "'\\''") + "'"
    fun start(input: Uri?, url: String, target: String, preset: String, mode: String, effects: String): String {
        val command = buildString {
            append("python -m aitmeral ")
            if (url.isNotBlank()) append("url ").append(quote(url)).append(" ")
            else if (input != null) append("convert ").append(quote(input.toString())).append(" ")
            else return "Choose a file or enter a URL."
            append("-S ").append(quote(target)).append(" ")
            append("-p ").append(quote(preset)).append(" ")
            append("--mode ").append(quote(mode)).append(" ")
            if (effects.isNotBlank()) append("--effects ").append(quote(effects)).append(" ")
        }
        return TermuxRunner.run(command)
    }
    fun doctor() = TermuxRunner.run("python -m aitmeral doctor")
}
private object TermuxRunner {
    fun run(command: String): String = try {
        val intent = Intent("com.termux.app.RUN_COMMAND").apply {
            setPackage("com.termux")
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-lc", command))
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
        }
        AppHolder.context.sendBroadcast(intent)
        "Job submitted. Processing continues in the background."
    } catch (error: Exception) {
        "Local processing backend unavailable: " + (error.message ?: "unknown error")
    }
}
private object AppHolder { lateinit var context: android.content.Context }
