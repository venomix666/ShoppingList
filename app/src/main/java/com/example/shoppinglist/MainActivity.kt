package com.example.shoppinglist

import android.R.attr.onClick
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.shoppinglist.ui.theme.ShoppingListTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContent {
            val vm: ShoppingViewModel = viewModel()
            vm.startListSync()
            ShoppingListTheme {
                ShoppingApp(vm)
            }
        }
    }
}

@Composable
fun ShoppingApp(vm: ShoppingViewModel) {

    var text by remember { mutableStateOf("") }
    val items by vm.items.collectAsStateWithLifecycle()
    val lists by vm.lists.collectAsStateWithLifecycle()
    var listName by remember { mutableStateOf("") }
    var listToDelete by remember { mutableStateOf<ShoppingListEntity?>(null) }
    val selectedListId by vm.selectedListId.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val selectedCategoryId by vm.selectedCategoryId.collectAsStateWithLifecycle()

    var itemToChange by remember { mutableStateOf<ShoppingItemEntity?>(value = null) }
    //val selectedListId = vm.selectedListId.collectAsStateWithLifecycle().value
    //val suggestions by vm.suggestions.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {

        Row {
            TextField(
                value = listName,
                onValueChange = { listName = it },
                modifier = Modifier.weight(1f),
                label = { Text("Ny lista") }
            )

            Spacer(Modifier.width(8.dp))

            Button(onClick = {
                if(listName != "") vm.addList(listName)
                listName = ""
            }) {
                Text("Lägg till")
            }
        }
        LazyRow {
            items(lists) { list ->
                val isSelected = list.shareId == selectedListId

                println("Is selected? ${isSelected}")

                Surface(
                    tonalElevation = if (isSelected) 6.dp else 0.dp,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .combinedClickable(
                            onClick = {
                                vm.selectList(list.shareId)
                                println("Selected: ${list.shareId}")
                            },
                            onLongClick = {
                                listToDelete = list
                            }
                        )
                ) {


                    Text(
                        text = list.name,
                        modifier = Modifier.padding(8.dp),
                        color = if (isSelected) Color(0xFF1B5E20) else Color.Unspecified
                    )

                }

            }
        }

        Spacer(Modifier.height(16.dp))
        Row (
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ){
            ItemAutocompleteField(
                text = text,
                onTextChange = {
                    text = it
                    vm.onQueryChanged(it)
                },
                suggestions = vm.suggestions.collectAsStateWithLifecycle().value,
                onSuggestionSelected = { suggestion ->
                    text = suggestion.name
                    vm.selectSuggestion(suggestion)
                },
                modifier = Modifier.weight(0.1f)
            )
            Spacer(Modifier.width(8.dp))

            Button(
                onClick = {
                            if(text != "" && selectedCategoryId != null) {
                                vm.addItem(text)
                                text = ""
                            }
                          },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(" Lägg till ", maxLines=1)
            }
        }

        LazyRow {
            items(categories) { cat ->

                val isSelected = cat.id == selectedCategoryId

                FilterChip(
                    selected = isSelected,
                    onClick = { vm.selectCategory(cat.id) },
                    label = { Text(cat.name) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        val grouped = items.groupBy { it.categoryId }
        val categoryMap = categories.associateBy { it.id }

        LazyColumn {

            //grouped.forEach { (categoryId, itemsInCategory) ->
            categories.sortedBy { it.id }.forEach { category ->
                //val categoryName =
                //    categoryMap[categoryId]?.name ?: "Uncategorized"

                val categoryItems = grouped[category.id].orEmpty()
                if (categoryItems.isEmpty()) return@forEach

                // SECTION HEADER
                item {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(categoryItems) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left group
                        Row (
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.picked,
                                onCheckedChange = { vm.togglePicked(item) },
                            )

                            Spacer(Modifier.width(4.dp))   // small gap

                            Text(text = (if(item.quantity > 1) "${item.quantity} " else "") + item.name,
                                modifier = Modifier.combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        itemToChange = item
                                    }
                                ).weight(1.0f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if(item.picked) Color.Gray else Color.Black,
                                style = if(item.picked)
                                    TextStyle(textDecoration = TextDecoration.LineThrough)
                                else TextStyle.Default,


                            )
                            TextButton(onClick = { vm.delete(item) }) {
                                Text("Ta bort")
                            }
                        }

                    }


                }

                // SEPARATOR (optional visual spacing)
                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }

    listToDelete?.let { list ->

        AlertDialog(
            onDismissRequest = { listToDelete = null },
            title = { Text("Ta bort lista") },
            text = { Text("Vill du ta bort '${list.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteList(list)
                    listToDelete = null
                }) {
                    Text("Ta bort")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    listToDelete = null
                }) {
                    Text("Avbryt")
                }
            }
        )
    }


    itemToChange?.let { item->
        var text by remember(item.id) { mutableStateOf(item.quantity.toString()) }
        AlertDialog(
            onDismissRequest = { itemToChange = null },
            title = { Text("Ändra antal") },
            text = { TextField(
                value = text,
                onValueChange = { text = it },
                //modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text("Nytt antal") },
            ) },
            confirmButton = {
                TextButton(onClick = {
                    val quantity = text.toIntOrNull()
                    if(quantity != null) vm.updateQuantity(item, quantity)
                    itemToChange = null
                }) {
                    Text("Ändra")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    itemToChange = null
                }) {
                    Text("Avbryt")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemAutocompleteField(
    text: String,
    onTextChange: (String) -> Unit,
    suggestions: List<ItemSuggestionEntity>,
    onSuggestionSelected: (ItemSuggestionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {

        OutlinedTextField(
            value = text,
            onValueChange = {
                onTextChange(it)
                expanded = true
            },
            modifier = modifier.menuAnchor(),
                //.menuAnchor().fillMaxWidth(),
                //.fillMaxWidth(),
            label = { Text("Ny vara") },
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = { expanded = false }
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion.name) },
                    onClick = {
                        onSuggestionSelected(suggestion)
                        expanded = false
                    }
                )
            }
        }
    }
}