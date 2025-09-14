package com.example.androidondevicellm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.androidondevicellm.ui.theme.AndroidondevicellmTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var llmHelper: LLMHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        llmHelper = LLMHelper(this)
        enableEdgeToEdge()
        setContent {
            AndroidondevicellmTheme {
                ChatScreen(llmHelper = llmHelper, lifecycleScope = lifecycleScope)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        llmHelper.close()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    llmHelper: LLMHelper,
    lifecycleScope: kotlinx.coroutines.CoroutineScope
) {
    var inputText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var isLoading by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }
    var initError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        lifecycleScope.launch {
            llmHelper.initialize().fold(
                onSuccess = { isInitialized = true },
                onFailure = { 
                    initError = it.message ?: "初期化に失敗しました"
                    isInitialized = false
                }
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("MediaPipe LLM Chat") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (initError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = "エラー: $initError",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    ChatMessageItem(message = message)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    placeholder = { Text("メッセージを入力...") },
                    enabled = isInitialized && !isLoading
                )

                Button(
                    onClick = {
                        if (inputText.isNotBlank() && isInitialized && !isLoading) {
                            val userMessage = ChatMessage(inputText, true)
                            messages = messages + userMessage
                            isLoading = true

                            lifecycleScope.launch {
                                llmHelper.generateResponse(inputText).fold(
                                    onSuccess = { response ->
                                        val aiMessage = ChatMessage(response, false)
                                        messages = messages + aiMessage
                                    },
                                    onFailure = { error ->
                                        val errorMessage = ChatMessage(
                                            "エラー: ${error.message ?: "応答の生成に失敗しました"}",
                                            false
                                        )
                                        messages = messages + errorMessage
                                    }
                                )
                                isLoading = false
                            }
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && isInitialized && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("送信")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (message.isUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)