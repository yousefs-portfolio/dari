package code.yousef.dari.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class CategoryItem(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val parentId: String? = null,
    val subcategories: List<CategoryItem> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(
    selectedCategory: CategoryItem?,
    onCategorySelected: (CategoryItem) -> Unit,
    categories: List<CategoryItem>,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "Select category",
    enabled: Boolean = true
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    OutlinedTextField(
        value = selectedCategory?.name ?: "",
        onValueChange = { },
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { showBottomSheet = true },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        readOnly = true,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        leadingIcon = selectedCategory?.let {
            {
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = it.color.copy(alpha = 0.2f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = it.icon,
                            contentDescription = null,
                            tint = it.color,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select category"
            )
        }
    )
    
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            CategoryBottomSheet(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { category ->
                    onCategorySelected(category)
                    showBottomSheet = false
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CategoryBottomSheet(
    categories: List<CategoryItem>,
    selectedCategory: CategoryItem?,
    onCategorySelected: (CategoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text(
            text = "Select Category",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn {
            items(categories) { category ->
                CategoryItemRow(
                    category = category,
                    isSelected = category.id == selectedCategory?.id,
                    onClick = { onCategorySelected(category) }
                )
                
                // Show subcategories if they exist
                if (category.subcategories.isNotEmpty()) {
                    items(category.subcategories) { subcategory ->
                        CategoryItemRow(
                            category = subcategory,
                            isSelected = subcategory.id == selectedCategory?.id,
                            onClick = { onCategorySelected(subcategory) },
                            isSubcategory = true
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun CategoryItemRow(
    category: CategoryItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSubcategory: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (isSubcategory) 24.dp else 0.dp,
                bottom = 8.dp
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = category.color.copy(alpha = 0.2f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = category.color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = category.name,
                style = if (isSubcategory) {
                    MaterialTheme.typography.bodyMedium
                } else {
                    MaterialTheme.typography.titleMedium
                },
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChipSelector(
    categories: List<CategoryItem>,
    selectedCategories: Set<String>,
    onCategoryToggle: (CategoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        categories.forEach { category ->
            val isSelected = selectedCategories.contains(category.id)
            
            Surface(
                onClick = { onCategoryToggle(category) },
                modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = category.color,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun CategorySelectorPreview() {
    val sampleCategories = listOf(
        CategoryItem(
            id = "1",
            name = "Shopping",
            icon = Icons.Default.ShoppingCart,
            color = MaterialTheme.colorScheme.primary
        )
    )
    
    var selectedCategory by remember { mutableStateOf<CategoryItem?>(null) }
    
    MaterialTheme {
        CategorySelector(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            categories = sampleCategories,
            label = "Category",
            modifier = Modifier.padding(16.dp)
        )
    }
}