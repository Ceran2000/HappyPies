package pl.ceranka.happypies.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable

@Composable
fun filledFieldColors(): TextFieldColors {
    val surface = MaterialTheme.colorScheme.surface
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = surface,
        unfocusedContainerColor = surface,
        disabledContainerColor = surface,
        errorContainerColor = surface
    )
}