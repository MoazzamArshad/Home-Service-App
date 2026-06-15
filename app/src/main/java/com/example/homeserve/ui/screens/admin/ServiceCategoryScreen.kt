package com.example.homeserve.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.homeserve.ui.theme.BrandBlue
import com.example.homeserve.ui.components.PrimaryButton
import com.example.homeserve.ui.viewmodel.AdminViewModel
import com.example.homeserve.data.model.Category
import com.example.homeserve.data.model.ServiceModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceCategoryScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit
) {
    val categories by viewModel.categoriesList.collectAsState()
    val services by viewModel.servicesList.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    
    // Master-Detail tracking: null = categories list, not-null = managing sub-services of this category
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    
    // Category dialog controllers
    var isCategoryModalOpen by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var showCategoryDeleteDialog by remember { mutableStateOf<Category?>(null) }

    // Service dialog controllers
    var isServiceModalOpen by remember { mutableStateOf(false) }
    var editingService by remember { mutableStateOf<ServiceModel?>(null) }
    var showServiceDeleteDialog by remember { mutableStateOf<ServiceModel?>(null) }

    // Synchronize selected category in real-time if it was deleted or updated externally
    LaunchedEffect(categories) {
        selectedCategory?.let { current ->
            val updated = categories.find { it.categoryId == current.categoryId }
            if (updated == null) {
                selectedCategory = null
            } else if (updated != current) {
                selectedCategory = updated
            }
        }
    }

    // Category delete verification dialog
    if (showCategoryDeleteDialog != null) {
        val cat = showCategoryDeleteDialog!!
        AlertDialog(
            onDismissRequest = { showCategoryDeleteDialog = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Delete Category?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${cat.name}'? This will cascadingly delete ALL sub-services inside it and cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(cat.categoryId)
                        showCategoryDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDeleteDialog = null }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            }
        )
    }

    // Category add/edit modal
    if (isCategoryModalOpen) {
        AddCategoryModal(
            category = editingCategory,
            onDismiss = { isCategoryModalOpen = false },
            onSave = { name, icon ->
                editingCategory?.let {
                    viewModel.updateCategory(it.categoryId, name, icon, it.isActive)
                } ?: run {
                    viewModel.addCategory(name, icon)
                }
                isCategoryModalOpen = false
            }
        )
    }

    // Service delete verification dialog
    if (showServiceDeleteDialog != null) {
        val srv = showServiceDeleteDialog!!
        AlertDialog(
            onDismissRequest = { showServiceDeleteDialog = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Delete Sub-Service?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${srv.name}'? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteService(srv.serviceId)
                        showServiceDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showServiceDeleteDialog = null }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            }
        )
    }

    // Service add/edit modal
    if (isServiceModalOpen && selectedCategory != null) {
        AddServiceModal(
            service = editingService,
            onDismiss = { isServiceModalOpen = false },
            onSave = { name, desc, price ->
                editingService?.let {
                    viewModel.updateService(it.serviceId, it.categoryId, name, desc, price, it.isActive)
                } ?: run {
                    viewModel.addService(selectedCategory!!.categoryId, name, desc, price)
                }
                isServiceModalOpen = false
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // Master-detail adaptive Blue Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BrandBlue,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .padding(top = 48.dp, bottom = 24.dp, start = 16.dp, end = 24.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (selectedCategory != null) {
                                selectedCategory = null
                                searchQuery = ""
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = selectedCategory?.let { "${it.name} Services" } ?: "Service Categories",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = selectedCategory?.let { "Manage sub-service listings and pricing" } ?: "Manage your platform offerings",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(selectedCategory?.let { "Search services..." } ?: "Search categories...", color = Color(0xFF9CA3AF)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF111827),
                        unfocusedTextColor = Color(0xFF111827),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = BrandBlue
                    )
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            val activeCategory = selectedCategory
            if (activeCategory == null) {
                // CATEGORIES VIEW (Master List)
                val filteredCategories = categories.filter {
                    it.name.contains(searchQuery, ignoreCase = true)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (filteredCategories.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No categories found", color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        items(filteredCategories) { category ->
                            val subServiceCount = services.count { it.categoryId == category.categoryId }
                            CategoryCard(
                                category = category,
                                subServiceCount = subServiceCount,
                                onClick = { selectedCategory = category },
                                onEdit = {
                                    editingCategory = category
                                    isCategoryModalOpen = true
                                },
                                onDelete = { showCategoryDeleteDialog = category },
                                onToggleStatus = { isChecked ->
                                    viewModel.updateCategory(category.categoryId, category.name, category.icon, isChecked)
                                }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }

                FloatingActionButton(
                    onClick = {
                        editingCategory = null
                        isCategoryModalOpen = true
                    },
                    containerColor = BrandBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Category")
                }
            } else {
                // SUB-SERVICES VIEW (Detail List)
                val filteredServices = services
                    .filter { it.categoryId == activeCategory.categoryId }
                    .filter { it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (filteredServices.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No sub-services found under this category", color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        items(filteredServices) { service ->
                            SubServiceCard(
                                service = service,
                                onEdit = {
                                    editingService = service
                                    isServiceModalOpen = true
                                },
                                onDelete = { showServiceDeleteDialog = service },
                                onToggleStatus = { isChecked ->
                                    viewModel.updateService(service.serviceId, service.categoryId, service.name, service.description, service.price, isChecked)
                                }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }

                FloatingActionButton(
                    onClick = {
                        editingService = null
                        isServiceModalOpen = true
                    },
                    containerColor = BrandBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Service")
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: Category,
    subServiceCount: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF3F4F6)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(category.icon.ifBlank { "✨" }, fontSize = 28.sp)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = "$subServiceCount sub-services",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                }
                Switch(
                    checked = category.isActive,
                    onCheckedChange = onToggleStatus,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF10B981),
                        uncheckedThumbColor = Color(0xFF9CA3AF),
                        uncheckedTrackColor = Color(0xFFE5E7EB)
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    onEdit()
                }) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun SubServiceCard(
    service: ServiceModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF111827)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = service.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280),
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = service.isActive,
                    onCheckedChange = onToggleStatus,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF10B981),
                        uncheckedThumbColor = Color(0xFF9CA3AF),
                        uncheckedTrackColor = Color(0xFFE5E7EB)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rs. ${service.price}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = BrandBlue
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit", fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddCategoryModal(
    category: Category?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var icon by remember { mutableStateOf(category?.icon ?: "🔧") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = if (category != null) "Edit Category" else "New Category",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF111827)
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                Text("Category Name", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFF374151))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF111827),
                        unfocusedTextColor = Color(0xFF111827),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        cursorColor = BrandBlue
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text("Icon (Emoji)", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFF374151))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF111827),
                        unfocusedTextColor = Color(0xFF111827),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        cursorColor = BrandBlue
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    PrimaryButton(
                        text = "Save",
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name.trim(), icon.trim())
                            }
                        },
                        modifier = Modifier.width(100.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddServiceModal(
    service: ServiceModel?,
    onDismiss: () -> Unit,
    onSave: (String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf(service?.name ?: "") }
    var description by remember { mutableStateOf(service?.description ?: "") }
    var priceStr by remember { mutableStateOf(service?.price?.toString() ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = if (service != null) "Edit Sub-Service" else "New Sub-Service",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF111827)
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                Text("Service Name", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFF374151))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF111827),
                        unfocusedTextColor = Color(0xFF111827),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        cursorColor = BrandBlue
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Description", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFF374151))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF111827),
                        unfocusedTextColor = Color(0xFF111827),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        cursorColor = BrandBlue
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Base Price (Rs.)", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFF374151))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it.filter { char -> char.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF111827),
                        unfocusedTextColor = Color(0xFF111827),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        cursorColor = BrandBlue
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    PrimaryButton(
                        text = "Save",
                        onClick = {
                            val price = priceStr.toIntOrNull() ?: 0
                            if (name.isNotBlank() && price > 0) {
                                onSave(name.trim(), description.trim(), price)
                            }
                        },
                        modifier = Modifier.width(100.dp)
                    )
                }
            }
        }
    }
}
