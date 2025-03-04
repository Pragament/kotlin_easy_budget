package com.example.currencypicker

import androidx.compose.ui.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.currencypicker.ui.theme.CurrencyPickerTheme
import java.nio.file.WatchEvent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CurrencyPickerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CurrencyPickerScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun CurrencyPickerScreen(modifier: Modifier = Modifier) {
    val currencies = listOf(
        "A$ - Australian Dollar",
        "C$ - Canadian Dollar",
        "$ - US Dollar",
        "€ - Euro",
        "£ - British Pound",
        "₹ - Indian Rupee",
        "¥ - Japanese Yen"
    ).sortedBy { it.substringAfter("- ") }

    var expanded by remember { mutableStateOf(false) }
    var selectedCurrency by remember { mutableStateOf(currencies[4]) }

    Column(modifier = Modifier.fillMaxSize()
        .background(color = Color(0xff2196f3)),
        horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth()
                .padding(top = 24.dp),
                horizontalArrangement = Arrangement.Center) {
                Text("Let's get to know each",
                    color = Color.White,
                    fontSize = 32.sp,
                    modifier = Modifier.padding(top = 16.dp),
                    fontWeight = FontWeight.Bold,
                    lineHeight = 48.sp)
            }

            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center) {
                Text("other!",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 48.sp)
            }

        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("Step1: What currency do you use?",
                color = Color.White,
                fontSize = 24.sp,
                lineHeight = 48.sp,
                modifier = Modifier.padding(top = 8.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("(You can change it later in the settings)",
                color = Color.White,
                fontSize = 20.sp,
                lineHeight = 8.sp)
        }
        Spacer(modifier = Modifier.padding(bottom = 16.dp))
        Box {
            Button(
                onClick = { expanded = true },
                modifier = Modifier.width(300.dp),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.buttonColors(Color.White)
            ) {
                Text(text = selectedCurrency,
                    color = Color.Black,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                    maxLines = 1)
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Arrow Down",
                    tint = Color.Black)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(300.dp)
            ) {
                currencies.forEach { currency ->
                    DropdownMenuItem(text = { Text(currency) },
                        onClick = {
                        selectedCurrency = currency
                        expanded = false
                    })
                }
            }
        }
        Spacer(modifier = Modifier.padding(bottom = if (expanded) 400.dp else 32.dp))

        Button(
            onClick = {

            }, 
            colors = ButtonDefaults.buttonColors(Color(0xff06879d))
        ) {
            Text("Confirm Selection")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CurrencyPickerTheme {
        CurrencyPickerScreen()
    }
}