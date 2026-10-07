package app.flenimg

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
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
import java.io.File

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
                    OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }) {
                        Text(if (input == null) "Choose image / video" else "Change file")
                    }
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
                Button(onClick = {
                    status = EngineBridge.start(input, url, target, preset, mode, effects)
                }, Modifier.weight(1f)) { Text("Enhance") }
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
    private const val OUTPUT_DIR = "/storage/emulated/0/Download/Flen.img/outputs"

    private fun quote(value: String) = "'" + value.replace("'", "'\\''") + "'"

    fun start(input: Uri?, url: String, target: String, preset: String, mode: String, effects: String): String {
        return try {
            val source = if (url.isNotBlank()) {
                "url " + quote(url)
            } else if (input != null) {
                quote(prepareInput(input))
            } else {
                return "Choose a file or enter a URL."
            }

            val command = buildString {
                append("mkdir -p ").append(quote(OUTPUT_DIR)).append(" && ")
                append("python -m aitmeral ")
                append(if (url.isNotBlank()) "url " else "convert ")
                append(source.substringAfter(if (url.isNotBlank()) "url " else "convert "))
                append(" -o ").append(quote(OUTPUT_DIR))
                append(" -S ").append(quote(target))
                append(" -p ").append(quote(preset))
                append(" --mode ").append(quote(mode))
                if (effects.isNotBlank()) append(" --effects ").append(quote(effects))
            }
            TermuxRunner.run(command)
        } catch (e: Exception) {
            "Could not prepare input: " + (e.message ?: "unknown error")
        }
    }

    fun doctor() = TermuxRunner.run("python -m aitmeral doctor")

    private fun prepareInput(uri: Uri): String {
        val resolver = AppHolder.context.contentResolver
        val original = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else "input.bin"
        } ?: "input.bin"
        val safe = original.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val name = System.currentTimeMillis().toString() + "_" + safe
        val relative = "Download/Flen.img/input/"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, resolver.getType(uri) ?: "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, relative)
        }
        val out = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Unable to create shared input file")
        resolver.openInputStream(uri).use { source ->
            resolver.openOutputStream(out).use { sink ->
                requireNotNull(source)
                requireNotNull(sink)
                source.copyTo(sink, 1024 * 1024)
            }
        }
        return "/storage/emulated/0/$relative$name"
    }
}

private object TermuxRunner {
    fun run(command: String): String = try {
        val intent = Intent("com.termux.RUN_COMMAND").apply {
            setClassName("com.termux", "com.termux.app.RunCommandService")
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-lc", command))
            putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
        }
        AppHolder.context.startService(intent)
        "Job submitted. Output: Download/Flen.img/outputs"
    } catch (error: Exception) {
        "Local processing backend unavailable: " + (error.message ?: "unknown error")
    }
}

private object AppHolder { lateinit var context: android.content.Context }
