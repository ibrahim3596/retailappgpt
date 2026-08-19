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
import com.retailpos.app.core.products.ProductMetadataFormState

/** Thin UI adapter for the metadata form state; persistence remains in the ViewModel. */
@Composable
fun ProductMetadataEditor(
    form: ProductMetadataFormState,
    onChange: ((ProductMetadataFormState) -> ProductMetadataFormState) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("PRODUCT DETAILS")
        OutlinedTextField(
            value = form.category,
            onValueChange = { onChange { it.copy(category = it) } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Category") },
            singleLine = true
        )
        OutlinedTextField(
            value = form.subcategory,
            onValueChange = { value -> onChange { it.copy(subcategory = value) } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Subcategory") },
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = form.packSize,
                onValueChange = { value -> onChange { it.copy(packSize = value) } },
                modifier = Modifier.weight(1f),
                label = { Text("Pack size") },
                singleLine = true
            )
            OutlinedTextField(
                value = form.packUnit,
                onValueChange = { value -> onChange { it.copy(packUnit = value) } },
                modifier = Modifier.weight(1f),
                label = { Text("Pack unit") },
                singleLine = true
            )
        }
        OutlinedTextField(
            value = form.description,
            onValueChange = { value -> onChange { it.copy(description = value) } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Description / notes") },
            minLines = 3,
            maxLines = 5
        )
    }
}
