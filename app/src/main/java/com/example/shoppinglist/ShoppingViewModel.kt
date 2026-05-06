package com.example.shoppinglist

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.collections.emptyList
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers

class ShoppingViewModel(app: Application) : AndroidViewModel(app) {

    private val sdao = DatabaseProvider.get(app).shoppingDao()
    private val cdao = DatabaseProvider.get(app).categoryDao()
    private val idao = DatabaseProvider.get(app).itemSuggestionDao()

    private val _selectedListId = MutableStateFlow<String?>(null)

    val lists = sdao.observeLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val selectedListId: MutableStateFlow<String?> = _selectedListId

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId
    val categories = cdao.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val firestore = FirestoreRepository(FirebaseFirestore.getInstance())

    private val query = MutableStateFlow("")
    val suggestions: StateFlow<List<ItemSuggestionEntity>> =
        query
            .debounce(300)
            .distinctUntilChanged()
            .flatMapLatest { q ->
                if (q.isBlank()) {
                    idao.getAll()
                } else {
                    idao.search(q)
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<ShoppingItemEntity>> =
        _selectedListId.flatMapLatest { shareId ->
            if (shareId == null) {
                flowOf(emptyList())
            } else {
                sdao.observeItems(shareId)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    init {
        viewModelScope.launch {
            val existing = cdao.getCategories().first()

            if (existing.isEmpty()) {
                cdao.insert(CategoryEntity(name = "Annat"))
                cdao.insert(CategoryEntity(name = "Grönsaker"))
                cdao.insert(CategoryEntity(name = "Hygien"))
                cdao.insert(CategoryEntity(name = "Torrvaror"))
                cdao.insert(CategoryEntity(name = "Bröd"))
                cdao.insert(CategoryEntity(name = "Mejeri"))
                cdao.insert(CategoryEntity(name = "Kylvaror"))
                cdao.insert(CategoryEntity(name = "Konserver/skafferi"))
                cdao.insert(CategoryEntity(name = "Drickbart"))
                cdao.insert(CategoryEntity(name = "Godis"))
                cdao.insert(CategoryEntity(name = "Djupfryst"))
                cdao.insert(CategoryEntity(name = "Städning"))

            }

            if (idao.getAll().first().isEmpty()) {
                idao.insertAll(defaultSuggestions)
            }


        }
    }

    fun selectCategory(id: Long?) {
        _selectedCategoryId.value = id
    }
    fun selectList(shareId: String) {
        _selectedListId.value = shareId
        viewModelScope.launch(Dispatchers.IO) {
            val list = sdao.getList(shareId) ?: return@launch
            startItemSync(shareId)
        }
    }

    fun addList(name: String) = viewModelScope.launch {
        viewModelScope.launch(Dispatchers.IO) {
            val shareId = UUID.randomUUID().toString()

            val list = ShoppingListEntity(
                shareId = shareId,
                name = name,
                createdAt = System.currentTimeMillis()
            )

            firestore.upsertList(list)
            _selectedListId.value = shareId

            startItemSync(shareId)
            //val list = sdao.getList(shareId) ?: return@launch
            //firestore.upsertList(list)

        }
    }
    fun deleteList(list: ShoppingListEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            sdao.deleteList(list)

            // Delete list in firestore
            if(list.shareId.isNotBlank()) {
                firestore.deleteListCascade(list.shareId)
            }

            // If we deleted the currently selected list, clear selection
            if (_selectedListId.value == list.shareId) {
                _selectedListId.value = null
            }
        }
    }

    fun addItem(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val shareId = _selectedListId.value ?: return@launch
            val list = sdao.getList(shareId)

            if (list == null) {
                Log.e("SYNC", "List not found for id=$shareId")
                return@launch
            }

            //val categoryId = selectedCategoryId.value

            val item = ShoppingItemEntity(
                id =  UUID.randomUUID().toString(),
                name = name,
                shareId = shareId,
                categoryId = selectedCategoryId.value
            )
            //val newId = sdao.insertItem(item)
            //val itemWithId = item.copy(id=newId)

            if (shareId.isNotBlank()) {
                firestore.upsertItem(shareId, item)
            }

        }

    }

    fun startListSync() {
        firestore.observeLists { lists ->
            viewModelScope.launch(Dispatchers.IO) {
                sdao.replaceAllLists(lists)
            }
        }
    }

    fun startItemSync(shareId: String) {

        firestore.observeItems(shareId) { items ->
            viewModelScope.launch(Dispatchers.IO) {
                sdao.replaceItemsForList(shareId, items)
            }
        }
    }

    fun delete(item: ShoppingItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val shareId = _selectedListId.value ?: return@launch
            val list = sdao.getList(shareId)
            if(list == null) return@launch
            //val shareId = list.shareId

            sdao.deleteItem(item)
            firestore.deleteItem(shareId, item.id)
        }
    }

    fun togglePicked(item: ShoppingItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val listId = _selectedListId.value ?: return@launch
            val list = sdao.getList(listId) ?: return@launch
            val shareId = list.shareId

            sdao.setPicked(item.id, !item.picked)

            val pickedItem = item.copy(picked = !item.picked)

            if (shareId.isNotBlank()) {
                firestore.upsertItem(shareId, pickedItem)
            }
        }
    }

    fun updateQuantity(item: ShoppingItemEntity, quantity: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val listId = _selectedListId.value ?: return@launch
            val list = sdao.getList(listId) ?: return@launch
            val shareId = list.shareId
            sdao.setQuantity(item.id, quantity)
            val updatedItem = item.copy(quantity = quantity)
            if(shareId.isNotBlank()) {
                firestore.upsertItem(shareId, updatedItem)
            }
        }
    }

    fun onQueryChanged(text: String) {
        query.value = text
    }

    fun selectSuggestion(s: ItemSuggestionEntity) {
        _selectedCategoryId.value = s.categoryId
    }

}