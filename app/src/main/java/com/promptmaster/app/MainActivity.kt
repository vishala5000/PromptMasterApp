package com.promptmaster.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

private val Red      = Color(0xFFD32F2F)
private val RedLight = Color(0xFFFF1744)
private val Black    = Color(0xFF000000)
private val White    = Color(0xFFFFFFFF)

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
fun HomeScreen(nav: androidx.navigation.NavController) {
    SquareBackground {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("PROMPT\nMASTER", color = White, fontSize = 42.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, lineHeight = 46.sp)
            Spacer(Modifier.height(8.dp))
            Text("Pick a category", color = White.copy(alpha = 0.75f), fontSize = 14.sp)
            Spacer(Modifier.height(40.dp))
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                NeonButton("👨  MAN") { nav.navigate("man") }
                NeonButton("👩  WOMEN") { nav.navigate("women") }
                NeonButton("💑  COUPLES") { nav.navigate("couples") }
            }
        }
    }
}

@Composable
fun CategoryScreen(title: String, promptText: String, titleText: String) {
    val ctx = LocalContext.current
    var showCustomDialog by remember { mutableStateOf(false) }
    var customWord by remember { mutableStateOf("") }

    fun copy(label: String, text: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text.trim()))
        Toast.makeText(ctx, "$label copied ✅", Toast.LENGTH_SHORT).show()
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

    SquareBackground {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(title, color = White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(40.dp))
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                NeonButton("$title PROMPT") { copy("Prompt", promptText) }
                NeonButton("TITLE PROMPT") { copy("Title", titleText) }
                NeonButton("⚡ CUSTOM PROMPT") { showCustomDialog = true }
            }
        }
    }
}

const val MAN_PROMPT = "CREATE MAN PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, MASTERPIECE, 9:16"
const val WOMEN_PROMPT = "CREATE GIRL PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, BEAUTIFUL, PERFECT MAKEUP, MASTERPIECE, 9:16"
const val COUPLES_PROMPT = "CREATE COUPLES PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, MASTERPIECE, 9:16"
const val TITLE_PROMPT = "CREATE 1 MOST ENGAGING HIGH CTR TITLE UNDER 80 CHARACTERS FOR YOUTUBE SHORTS VIDEO INCLUDING 3 HASHTAGS AND SOME IMOJIES, NEVER REPEAT, COMPLETELY UNIQUE, 2 SEARCHABLE KEYWORD IN THIS ONE TITLE,"
const val CUSTOM_PROMPT_TEMPLATE = "CREATE [word] PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, MASTERPIECE, 9:16"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val nav = rememberNavController()
            NavHost(nav, startDestination = "home") {
                composable("home") { HomeScreen(nav) }
                composable("man") { CategoryScreen("MAN", MAN_PROMPT, TITLE_PROMPT) }
                composable("women") { CategoryScreen("WOMEN", WOMEN_PROMPT, TITLE_PROMPT) }
                composable("couples") { CategoryScreen("COUPLES", COUPLES_PROMPT, TITLE_PROMPT) }
            }
        }
    }
}
