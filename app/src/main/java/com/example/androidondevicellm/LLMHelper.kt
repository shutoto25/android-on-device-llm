package com.example.androidondevicellm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class LLMHelper(private val context: Context) {
    private var llmInference: LlmInference? = null
    private var isInitialized = false

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (isInitialized) {
                return@withContext Result.success(Unit)
            }

//            val modelPath = "/data/local/tmp/llm/gemma-3n-E2B.litertlm"
            val modelPath = "/data/local/tmp/llm/gemma3-1b-it-int4.task"

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateResponse(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized || llmInference == null) {
                return@withContext Result.failure(IllegalStateException("LLM not initialized"))
            }

            val result = llmInference!!.generateResponse(prompt)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun generateResponseAsync(prompt: String): Flow<Result<String>> = channelFlow {
        try {
            if (!isInitialized || llmInference == null) {
                send(Result.failure(IllegalStateException("LLM not initialized")))
                return@channelFlow
            }

            llmInference!!.generateResponseAsync(prompt) { partialResult, done ->
                try {
                    if (partialResult != null) {
                        // 部分的な結果をsend
                        trySend(Result.success(partialResult))
                    }
                    if (done) {
                        // 完了時にchannelを閉じる
                        close()
                    }
                } catch (e: Exception) {
                    trySend(Result.failure(e))
                    close(e)
                }
            }
            
            awaitClose {
                // cleanup if needed
            }
        } catch (e: Exception) {
            send(Result.failure(e))
            close(e)
        }
    }.flowOn(Dispatchers.IO)

    fun close() {
        llmInference?.close()
        llmInference = null
        isInitialized = false
    }
}