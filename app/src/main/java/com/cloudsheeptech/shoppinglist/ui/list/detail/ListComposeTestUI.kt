package com.cloudsheeptech.shoppinglist.ui.list.detail

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

class ListComposeTestUI {

    data class Store(
        val id: String,
        val name: String,
        val abbreviation: String,
    )


    @Composable
    fun MyView() {
        Row {
            Text("This is a test")
            Text("Again")
            Button(onClick = {

            }) {
                Text("Todo")
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
//    @Preview
    @Composable
    fun SecondTest() {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.primary,
            ),
            title = {
                Text(
                    "Centered Top App Bar",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = { /* do something */ }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Localized description"
                    )
                }
            },
            actions = {
                IconButton(onClick = { /* do something */ }) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Localized description"
                    )
                }
            },
            scrollBehavior = scrollBehavior,
        )
    }

    @Preview(showBackground = true)
    @Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Composable
    fun ShoppingListTopBarPreview() {
        ShoppingListTopBar(
            listName = "Wocheneinkauf",
            totalPrice = 6.76,
            selectedStore = Store(
                id = "lidl",
                name = "Lidl",
                abbreviation = "L"
            ),
            onStoreClick = {},
            onOverflowShare = {},
            onOverflowStopSharing = {},
            onOverflowRename = {},
            onOverflowDelete = {},
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ShoppingListTopBar(
        listName: String,
        totalPrice: Double,
        selectedStore: Store,
        onStoreClick: () -> Unit,
        onOverflowShare: () -> Unit,
        onOverflowStopSharing: () -> Unit,
        onOverflowRename: () -> Unit,
        onOverflowDelete: () -> Unit,
    ) {
        var overflowExpanded by remember { mutableStateOf(false) }

        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            navigationIcon = {
                IconButton(onClick = { /* open drawer or navigate up */ }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu"
                    )
                }
            },
            title = {
                // Row 1 is handled by the bar itself (title slot + actions slot).
                // We only put the list name here — row 2 is injected via a Column trick.
                Column {
                    Text(
                        text = listName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // Row 2: store chip + price, full width
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, end = 48.dp), // end padding avoids overflow icon
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StoreChip(
                            store = selectedStore,
                            modifier = Modifier.weight(1f),
                            onClick = onStoreClick,
                        )
                        PriceChip(
                            price = totalPrice,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            },
            actions = {
                Box {
                    IconButton(onClick = { overflowExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                    DropdownMenu(
                        expanded = overflowExpanded,
                        onDismissRequest = { overflowExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share list") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = { overflowExpanded = false; onOverflowShare() },
                        )
                        DropdownMenuItem(
                            text = { Text("Stop sharing") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.PersonRemove,
                                    contentDescription = null
                                )
                            },
                            onClick = { overflowExpanded = false; onOverflowStopSharing() },
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = { overflowExpanded = false; onOverflowRename() },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Delete list",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = { overflowExpanded = false; onOverflowDelete() },
                        )
                    }
                }
            },
        )
    }

    @Composable
    fun StoreChip(
        store: Store,
        modifier: Modifier = Modifier,
        onClick: () -> Unit,
    ) {
        Surface(
            modifier = modifier.height(28.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
            onClick = onClick,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                // Swap this for an actual Image() with your store logo asset
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = store.abbreviation, // e.g. "L" for Lidl
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.width(5.dp))
                Text(
                    text = store.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }

    @Composable
    fun PriceChip(
        price: Double,
        modifier: Modifier = Modifier,
    ) {
        val formatted = remember(price) {
            NumberFormat.getCurrencyInstance(Locale.GERMANY).format(price)
        }
        Surface(
            modifier = modifier.height(28.dp),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.2f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = formatted,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }

}