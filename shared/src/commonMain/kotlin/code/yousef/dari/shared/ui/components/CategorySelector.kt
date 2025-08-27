package code.yousef.dari.shared.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.Category

/**
 * Category selector component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Category",
    categories: List<Category> = defaultCategories,
    enabled: Boolean = true,
    placeholder: String = "Select category"
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCategory?.name ?: "",
            onValueChange = { /* Read-only */ },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = {
                selectedCategory?.let { category ->
                    Icon(
                        imageVector = getCategoryIcon(category),
                        contentDescription = category.name,
                        tint = getCategoryColor(category)
                    )
                }
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { 
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(category),
                                contentDescription = category.name,
                                tint = getCategoryColor(category),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(category.name)
                        }
                    },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Category selector dialog with search and custom categories
 */
@Composable
fun CategorySelectorDialog(
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    categories: List<Category> = defaultCategories,
    allowCustomCategory: Boolean = true
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCreateCategory by remember { mutableStateOf(false) }
    
    val filteredCategories = remember(searchQuery, categories) {
        if (searchQuery.isEmpty()) {
            categories
        } else {
            categories.filter { 
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text("Select Category") },
        text = {
            Column {
                // Search field
                CompactSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { /* Auto-search on type */ },
                    placeholder = "Search categories...",
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Categories list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredCategories) { category ->
                        CategoryListItem(
                            category = category,
                            isSelected = category == selectedCategory,
                            onSelect = { 
                                onCategorySelected(category)
                                onDismiss()
                            }
                        )
                    }
                    
                    if (allowCustomCategory && filteredCategories.isEmpty() && searchQuery.isNotEmpty()) {
                        item {
                            CreateCategoryItem(
                                categoryName = searchQuery,
                                onCreateCategory = {
                                    showCreateCategory = true
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (allowCustomCategory) {
                TextButton(
                    onClick = { showCreateCategory = true }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Category")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
    
    if (showCreateCategory) {
        CreateCategoryDialog(
            initialName = searchQuery,
            onCategoryCreated = { category ->
                onCategorySelected(category)
                showCreateCategory = false
                onDismiss()
            },
            onDismiss = { showCreateCategory = false }
        )
    }
}

/**
 * Category list item
 */
@Composable
private fun CategoryListItem(
    category: Category,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect,
                role = Role.RadioButton
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    MaterialTheme.colorScheme.primary
                ).brush
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getCategoryIcon(category),
                contentDescription = category.name,
                tint = getCategoryColor(category),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                if (category.description.isNotEmpty()) {
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Create category item
 */
@Composable
private fun CreateCategoryItem(
    categoryName: String,
    onCreateCategory: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCreateCategory() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Create category",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = "Create \"$categoryName\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Create category dialog
 */
@Composable
private fun CreateCategoryDialog(
    initialName: String = "",
    onCategoryCreated: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    var categoryName by remember { mutableStateOf(initialName) }
    var categoryDescription by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(Icons.Filled.Category) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Category") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = categoryDescription,
                    onValueChange = { categoryDescription = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                
                // Icon selector (simplified)
                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.labelMedium
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val iconOptions = listOf(
                        Icons.Filled.Category,
                        Icons.Filled.ShoppingCart,
                        Icons.Filled.Restaurant,
                        Icons.Filled.DirectionsCar,
                        Icons.Filled.Home,
                        Icons.Filled.Movie
                    )
                    
                    iconOptions.forEach { icon ->
                        FilterChip(
                            selected = icon == selectedIcon,
                            onClick = { selectedIcon = icon },
                            label = { 
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (categoryName.isNotBlank()) {
                        val newCategory = Category(
                            id = "custom_${System.currentTimeMillis()}",
                            name = categoryName.trim(),
                            description = categoryDescription.trim(),
                            type = Category.CategoryType.EXPENSE,
                            isCustom = true
                        )
                        onCategoryCreated(newCategory)
                    }
                },
                enabled = categoryName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Get icon for category
 */
private fun getCategoryIcon(category: Category): ImageVector {
    return when (category.name.lowercase()) {
        "food", "food & dining", "restaurants" -> Icons.Filled.Restaurant
        "transportation", "transport", "fuel" -> Icons.Filled.DirectionsCar
        "shopping", "retail" -> Icons.Filled.ShoppingCart
        "bills", "utilities" -> Icons.Filled.Receipt
        "entertainment", "movies" -> Icons.Filled.Movie
        "health", "medical" -> Icons.Filled.LocalHospital
        "education", "books" -> Icons.Filled.School
        "home", "housing" -> Icons.Filled.Home
        "income", "salary" -> Icons.Filled.AttachMoney
        "investments" -> Icons.Filled.TrendingUp
        "savings" -> Icons.Filled.Savings
        "insurance" -> Icons.Filled.Security
        else -> Icons.Filled.Category
    }
}

/**
 * Get color for category
 */
@Composable
private fun getCategoryColor(category: Category): androidx.compose.ui.graphics.Color {
    return when (category.type) {
        Category.CategoryType.INCOME -> code.yousef.dari.shared.ui.theme.DariTheme.financialColors.Income
        Category.CategoryType.EXPENSE -> code.yousef.dari.shared.ui.theme.DariTheme.financialColors.Expense
        Category.CategoryType.TRANSFER -> MaterialTheme.colorScheme.tertiary
    }
}

/**
 * Default categories
 */
private val defaultCategories = listOf(
    Category("1", "Food & Dining", "Restaurants, groceries, food delivery", Category.CategoryType.EXPENSE),
    Category("2", "Transportation", "Gas, public transport, car maintenance", Category.CategoryType.EXPENSE),
    Category("3", "Shopping", "Clothes, electronics, general purchases", Category.CategoryType.EXPENSE),
    Category("4", "Bills & Utilities", "Electricity, water, phone, internet", Category.CategoryType.EXPENSE),
    Category("5", "Entertainment", "Movies, games, subscriptions", Category.CategoryType.EXPENSE),
    Category("6", "Health & Medical", "Doctor visits, pharmacy, insurance", Category.CategoryType.EXPENSE),
    Category("7", "Education", "School, courses, books", Category.CategoryType.EXPENSE),
    Category("8", "Home & Garden", "Rent, mortgage, home improvements", Category.CategoryType.EXPENSE),
    Category("9", "Salary", "Job income, bonuses", Category.CategoryType.INCOME),
    Category("10", "Business", "Business income, freelance", Category.CategoryType.INCOME),
    Category("11", "Investments", "Dividends, capital gains", Category.CategoryType.INCOME),
    Category("12", "Other Income", "Gifts, refunds, other income", Category.CategoryType.INCOME)
)