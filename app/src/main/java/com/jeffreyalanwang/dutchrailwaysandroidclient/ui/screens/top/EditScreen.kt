package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top

import android.annotation.SuppressLint
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
private fun EditScreenPreview() {
    EditScreen {  }
}

@Composable
fun EditScreen(onNavigate: (Any) -> Unit) {
    Column {
        Section("Edit places") {
            item("Edit station") {}
            item("Edit area") {}
        }

        Section("Edit trains") {
            item("New train") {}
            item("Copy train") {}
            item("Delete train") {}
        }
    }
}

@Composable
private fun Section(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable SectionScope.() -> Unit
) {
    val scrollState = rememberScrollState()

    Column {
        Text(title)
        Row(
            Modifier
                .horizontalScroll(scrollState)
                .height(150.dp)
                .padding(all = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionScope.content()
        }
    }
}

object SectionScope {
    @SuppressLint("ComposableNaming")
    @Composable
    fun item(
        title: String,
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
        content: @Composable BoxScope.() -> Unit,
    ) {
        Button(
            enabled = onClick != null,
            onClick = onClick ?: {},
            shape = MaterialTheme.shapes.largeIncreased,
            modifier = modifier
                .fillMaxHeight()
                .aspectRatio(1f)
        ) {
            Box(Modifier.weight(1f)) {
                Column(
                    Modifier
                        .fillMaxSize(.75f)
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.weight(1f)) {
                        Box(
                            Modifier.align(Alignment.Center),
                            content = content,
                        )
                    }
                    Text(title)
                }
            }
        }
    }
}