package io.agora.agora_videocall_jetpack_compose

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import io.agora.agora_videocall_jetpack_compose.ui.theme.AgoravideocalljetpackcomposeTheme

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgoravideocalljetpackcomposeTheme() {
                Surface(color = MaterialTheme.colors.background){
                    LoginPage()
                }
            }
        }
    }
}

@Composable
fun LoginPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(state = rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Agora Live Video Streaming", fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Jetpack Compose", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(50.dp))
        InputFields()
    }
}

@Composable
private fun InputFields() {
    val context = LocalContext.current
    val channelNameState = remember { mutableStateOf(TextFieldValue()) }
    val userRoleOptions = listOf("Broadcaster", "Audience")
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(userRoleOptions[0]) }
    Column(
        modifier = Modifier.padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        TextField(
            value = channelNameState.value,
            onValueChange = { channelNameState.value = it },
            label = { Text("Channel Name ") },
            placeholder = { Text("test") },
            modifier = Modifier
                .align(
                    alignment = Alignment.CenterHorizontally
                )
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        userRoleOptions.forEach{ text ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .selectable(
                        selected = (text == selectedOption),
                        onClick = { onOptionSelected(text) }
                    )
            )  {
                RadioButton(
                    selected = (
                            text == selectedOption
                            ),
                    modifier = Modifier.padding(
                        horizontal = 25.dp,
                        vertical = 10.dp
                    ),
                    onClick = {
                        onOptionSelected(text)
                    }
                )

                Text(
                    text = text,
                    modifier = Modifier.padding(start = 10.dp, top = 10.dp),
                    fontSize = 18.sp
                )

            }
        }
    }

    Spacer(modifier = Modifier.height(80.dp))

    Button(
        onClick = {
            val intent = Intent(context, VideoActivity::class.java)
            intent.putExtra("ChannelName", channelNameState.value.text)
            intent.putExtra("UserRole", selectedOption)
            ContextCompat.startActivity(context, intent, Bundle())
        },
        contentPadding = PaddingValues(
            horizontal = 20.dp,
            vertical = 10.dp
        )
    ) {
        Icon(Icons.Filled.ArrowForward, contentDescription = "Join", modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text(text = "Join", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}