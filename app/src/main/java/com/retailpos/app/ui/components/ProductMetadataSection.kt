package com.retailpos.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProductMetadataSection(
    category: String,
    onCategoryChange: (String) -> Unit,
    subcategory: String,
    onSubcategoryChange: (String) -> Unit,
    packSize: String,
    onPackSizeChange: (String) -> Unit,
    packUnit: String,
    onPackUnitChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("PRODUCT DETAILS")
        OutlinedTextField(
            value = category,
            onValueChange = onCategoryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Category") },
            singleLine = true
        )
        OutlinedTextField(
            value = subcategory,
            onValueChange = onSubcategoryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Subcategory") },
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = packSize,
                onValueChange = onPackSizeChange,
                modifier = Modifier.weight(1f),
                label = { Text("Pack size") },
                singleLine = true
            )
            OutlinedTextField(
                value = packUnit,
                onValueChange = onPackUnitChange,
                modifier = Modifier.weight(1f),
                label = { Text("Pack unit") },
                singleLine = true
            )
        }
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Description / notes") },
            minLines = 3,
            maxLines = 5
        )
    }
}
