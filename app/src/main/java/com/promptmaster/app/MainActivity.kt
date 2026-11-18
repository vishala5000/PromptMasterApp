package com.promptmaster.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private val Red      = Color(0xFFD32F2F)
private val RedLight = Color(0xFFFF1744)
private val Black    = Color(0xFF000000)
private val White    = Color(0xFFFFFFFF)

// ---------- DATA STORAGE (FREE, OFFLINE) ----------
class PromptStorage(context: Context) {
    private val file = File(context.filesDir, "prompts.json")
    
    data class PromptItem(
        val text: String,
        val category: String,
        val timestamp: Long,
        val isFavorite: Boolean = false
    )
    
    fun loadPrompts(): List<PromptItem> {
        return try {
            if (!file.exists()) return emptyList()
            val json = file.readText()
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val obj = arr.getJSONObject(it)
                PromptItem(
                    text = obj.getString("text"),
                    category = obj.getString("category"),
                    timestamp = obj.getLong("timestamp"),
                    isFavorite = obj.optBoolean("favorite", false)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun savePrompt(item: PromptItem) {
        val current = loadPrompts().toMutableList()
        current.add(0, item)
        if (current.size > 100) current.removeAt(current.lastIndex)
        val arr = JSONArray()
        current.forEach {
            arr.put(JSONObject().apply {
                put("text", it.text)
                put("category", it.category)
                put("timestamp", it.timestamp)
                put("favorite", it.isFavorite)
            })
        }
        file.writeText(arr.toString())
    }
    
    fun toggleFavorite(index: Int) {
        val current = loadPrompts().toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(isFavorite = !current[index].isFavorite)
            val arr = JSONArray()
            current.forEach {
                arr.put(JSONObject().apply {
                    put("text", it.text)
                    put("category", it.category)
                    put("timestamp", it.timestamp)
                    put("favorite", it.isFavorite)
                })
            }
            file.writeText(arr.toString())
        }
    }
    
    fun deletePrompt(index: Int) {
        val current = loadPrompts().toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            val arr = JSONArray()
            current.forEach {
                arr.put(JSONObject().apply {
                    put("text", it.text)
                    put("category", it.category)
                    put("timestamp", it.timestamp)
                    put("favorite", it.isFavorite)
                })
            }
            file.writeText(arr.toString())
        }
    }
}

// ---------- UI COMPONENTS ----------
@Composable
fun SquareBackground(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Black)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 80f
            val stroke = Stroke(width = 1.2f)
            var y = 0f
            while (y < size.height + step) {
                var x = 0f
                while (x < size.width + step) {
                    drawRect(color = Red.copy(alpha = 0.25f), topLeft = Offset(x, y), size = Size(step, step), style = stroke)
                    x += step
                }
                y += step
            }
        }
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(colors = listOf(Red.copy(alpha = 0.25f), Color.Transparent), radius = 900f)
        ))
        content()
    }
}

@Composable
fun NeonButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(brush = Brush.linearGradient(listOf(RedLight, Red)), shape = RoundedCornerShape(18.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun SmallButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .background(brush = Brush.linearGradient(listOf(RedLight, Red)), shape = RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// ---------- SCREENS ----------
@Composable
fun HomeScreen(nav: androidx.navigation.NavController, storage: PromptStorage) {
    SquareBackground {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("PROMPT\nMASTER", color = White, fontSize = 42.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, lineHeight = 46.sp)
            Spacer(Modifier.height(8.dp))
            Text("Advanced • Free • Offline", color = White.copy(alpha = 0.75f), fontSize = 14.sp)
            Spacer(Modifier.height(32.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                NeonButton("👨  MAN") { nav.navigate("man") }
                NeonButton("👩  WOMEN") { nav.navigate("women") }
                NeonButton("💑  COUPLES") { nav.navigate("couples") }
                NeonButton("👶  KIDS") { nav.navigate("kids") }
                NeonButton("🐕  PETS") { nav.navigate("pets") }
                NeonButton("🦸  FANTASY") { nav.navigate("fantasy") }
                NeonButton("🚀  SCI-FI") { nav.navigate("scifi") }
                Spacer(Modifier.height(8.dp))
                NeonButton("📜  HISTORY") { nav.navigate("history") }
                NeonButton("⭐  FAVORITES") { nav.navigate("favorites") }
            }
        }
    }
}

@Composable
fun CategoryScreen(
    title: String, 
    promptText: String, 
    titleText: String,
    storage: PromptStorage
) {
    val ctx = LocalContext.current
    var showCustomDialog by remember { mutableStateOf(false) }
    var customWord by remember { mutableStateOf("") }
    var showVariationsDialog by remember { mutableStateOf(false) }

    fun copy(label: String, text: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text.trim()))
        Toast.makeText(ctx, "$label copied ✅", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            storage.savePrompt(PromptStorage.PromptItem(text.trim(), title, System.currentTimeMillis()))
        }
    }
    
    fun share(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        ctx.startActivity(Intent.createChooser(intent, "Share Prompt"))
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Enter Custom Word", color = White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = customWord,
                    onValueChange = { customWord = it },
                    label = { Text("e.g., CYBORG, DOG, WARRIOR", color = White.copy(0.7f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        cursorColor = RedLight,
                        focusedBorderColor = Red,
                        unfocusedBorderColor = Red.copy(0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (customWord.isNotBlank()) {
                        val finalPrompt = CUSTOM_PROMPT_TEMPLATE.replace("[word]", customWord.trim().uppercase())
                        copy("Custom Prompt", finalPrompt)
                        showCustomDialog = false
                        customWord = ""
                    } else {
                        Toast.makeText(ctx, "Please enter a word", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Generate & Copy", color = RedLight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showCustomDialog = false
                    customWord = ""
                }) {
                    Text("Cancel", color = White.copy(0.7f))
                }
            },
            containerColor = Black,
            shape = RoundedCornerShape(16.dp)
        )
    }
    
    if (showVariationsDialog) {
        val variations = generateVariations(promptText)
        AlertDialog(
            onDismissRequest = { showVariationsDialog = false },
            title = { Text("5 Prompt Variations", color = White, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(variations) { variation ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { 
                                copy("Variation", variation)
                                showVariationsDialog = false
                            },
                            colors = CardDefaults.cardColors(containerColor = Red.copy(0.2f))
                        ) {
                            Text(
                                text = variation,
                                color = White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVariationsDialog = false }) {
                    Text("Close", color = RedLight)
                }
            },
            containerColor = Black,
            shape = RoundedCornerShape(16.dp)
        )
    }

    SquareBackground {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(title, color = White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(32.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                NeonButton("$title PROMPT") { copy("Prompt", promptText) }
                NeonButton("TITLE PROMPT") { copy("Title", titleText) }
                NeonButton("⚡ CUSTOM PROMPT") { showCustomDialog = true }
                NeonButton("🎲 5 VARIATIONS") { showVariationsDialog = true }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SmallButton("📤 SHARE", onClick = { share(promptText) })
                    SmallButton("📋 COPY", onClick = { copy("Prompt", promptText) })
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(storage: PromptStorage, nav: androidx.navigation.NavController) {
    var prompts by remember { mutableStateOf(storage.loadPrompts()) }
    val ctx = LocalContext.current
    
    SquareBackground {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📜 History", color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = White)
                }
            }
            Spacer(Modifier.height(16.dp))
            if (prompts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No history yet", color = White.copy(0.5f), fontSize = 16.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(prompts.size) { index ->
                        val prompt = prompts[index]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Red.copy(0.15f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(prompt.category, color = RedLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row {
                                        IconButton(onClick = {
                                            storage.toggleFavorite(index)
                                            prompts = storage.loadPrompts()
                                        }) {
                                            Icon(
                                                if (prompt.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                                "Favorite",
                                                tint = if (prompt.isFavorite) RedLight else White.copy(0.5f)
                                            )
                                        }
                                        IconButton(onClick = {
                                            storage.deletePrompt(index)
                                            prompts = storage.loadPrompts()
                                        }) {
                                            Icon(Icons.Default.Delete, "Delete", tint = White.copy(0.5f))
                                        }
                                    }
                                }
                                Text(prompt.text, color = White, fontSize = 13.sp, maxLines = 3)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SmallButton("COPY") {
                                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("Prompt", prompt.text))
                                        Toast.makeText(ctx, "Copied ✅", Toast.LENGTH_SHORT).show()
                                    }
                                    SmallButton("SHARE") {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, prompt.text)
                                        }
                                        ctx.startActivity(Intent.createChooser(intent, "Share"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(storage: PromptStorage, nav: androidx.navigation.NavController) {
    var prompts by remember { mutableStateOf(storage.loadPrompts().filter { it.isFavorite }) }
    val ctx = LocalContext.current
    
    SquareBackground {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⭐ Favorites", color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = White)
                }
            }
            Spacer(Modifier.height(16.dp))
            if (prompts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No favorites yet", color = White.copy(0.5f), fontSize = 16.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(prompts.size) { index ->
                        val prompt = prompts[index]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Red.copy(0.15f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(prompt.category, color = RedLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(prompt.text, color = White, fontSize = 13.sp, maxLines = 3)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SmallButton("COPY") {
                                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("Prompt", prompt.text))
                                        Toast.makeText(ctx, "Copied ✅", Toast.LENGTH_SHORT).show()
                                    }
                                    SmallButton("SHARE") {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, prompt.text)
                                        }
                                        ctx.startActivity(Intent.createChooser(intent, "Share"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- PROMPT TEMPLATES ----------
const val MAN_PROMPT = "CREATE MAN PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, MASTERPIECE, 9:16"
const val WOMEN_PROMPT = "CREATE GIRL PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, BEAUTIFUL, PERFECT MAKEUP, MASTERPIECE, 9:16"
const val COUPLES_PROMPT = "CREATE COUPLES PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, MASTERPIECE, 9:16"
const val KIDS_PROMPT = "CREATE KIDS PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, CUTE, INNOCENT, MASTERPIECE, 9:16"
const val PETS_PROMPT = "CREATE PETS PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, ADORABLE, MASTERPIECE, 9:16"
const val FANTASY_PROMPT = "CREATE FANTASY PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, MAGICAL, EPIC, MASTERPIECE, 9:16"
const val SCIFI_PROMPT = "CREATE SCI-FI PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, FUTURISTIC, MASTERPIECE, 9:16"
const val TITLE_PROMPT = "CREATE 1 MOST ENGAGING HIGH CTR TITLE UNDER 80 CHARACTERS FOR YOUTUBE SHORTS VIDEO INCLUDING 3 HASHTAGS AND SOME IMOJIES, NEVER REPEAT, COMPLETELY UNIQUE, 2 SEARCHABLE KEYWORD IN THIS ONE TITLE,"
const val CUSTOM_PROMPT_TEMPLATE = "CREATE [word] PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, MASTERPIECE, 9:16"

fun generateVariations(basePrompt: String): List<String> {
    val styles = listOf("CINEMATIC", "ARTISTIC", "DRAMATIC", "MINIMALIST", "VIBRANT")
    val effects = listOf("GOLDEN HOUR", "NEON LIGHTS", "MOODY", "BRIGHT", "ETHEREAL")
    return styles.zip(effects).map { (style, effect) ->
        "$basePrompt, STYLE: $style, EFFECT: $effect"
    }
}

// ---------- MAIN ACTIVITY ----------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storage = PromptStorage(this)
        setContent {
            val nav = rememberNavController()
            NavHost(nav, startDestination = "home") {
                composable("home") { HomeScreen(nav, storage) }
                composable("man") { CategoryScreen("MAN", MAN_PROMPT, TITLE_PROMPT, storage) }
                composable("women") { CategoryScreen("WOMEN", WOMEN_PROMPT, TITLE_PROMPT, storage) }
                composable("couples") { CategoryScreen("COUPLES", COUPLES_PROMPT, TITLE_PROMPT, storage) }
                composable("kids") { CategoryScreen("KIDS", KIDS_PROMPT, TITLE_PROMPT, storage) }
                composable("pets") { CategoryScreen("PETS", PETS_PROMPT, TITLE_PROMPT, storage) }
                composable("fantasy") { CategoryScreen("FANTASY", FANTASY_PROMPT, TITLE_PROMPT, storage) }
                composable("scifi") { CategoryScreen("SCI-FI", SCIFI_PROMPT, TITLE_PROMPT, storage) }
                composable("history") { HistoryScreen(storage, nav) }
                composable("favorites") { FavoritesScreen(storage, nav) }
            }
        }
    }
}
