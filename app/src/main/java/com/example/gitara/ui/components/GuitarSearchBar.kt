package com.example.gitara.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuitarSearchBar(
    searchText: String,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = searchText,
        onValueChange = onSearchChange,
        placeholder = { Text("Поиск песен...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = if (searchText.isNotEmpty()) {
            {
                IconButton(onClick = { onSearchChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Очистить")
                }
            }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    )
}
