package com.androdevlinux.satschat.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.androdevlinux.satschat.core.model.ChatMessage

/** A single-conversation screen: a message list plus a composer row. */
@Composable
fun ConversationScreen(
    store: ChatStore,
    title: String = "SatsChat RT",
    modifier: Modifier = Modifier,
) {
    val messages by store.messages.collectAsState()
    var input by remember { mutableStateOf("") }

    Column(modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "$title · demo — not encrypted yet",
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            items(messages) { message -> MessageBubble(message) }
            item { Spacer(Modifier.height(8.dp)) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message") },
                singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { store.send(input); input = "" }, enabled = input.isNotBlank()) {
                Text("Send")
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val background =
        if (message.fromMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromMe) Arrangement.End else Arrangement.Start,
    ) {
        Surface(color = background, shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
