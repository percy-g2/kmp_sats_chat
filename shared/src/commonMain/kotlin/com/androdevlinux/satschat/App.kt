package com.androdevlinux.satschat

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.androdevlinux.satschat.feature.chat.ChatStore
import com.androdevlinux.satschat.feature.chat.ConversationScreen

@Composable
@Preview
fun App() {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        val store = remember { ChatStore(scope) }
        ConversationScreen(store, modifier = Modifier.safeContentPadding().imePadding())
    }
}
