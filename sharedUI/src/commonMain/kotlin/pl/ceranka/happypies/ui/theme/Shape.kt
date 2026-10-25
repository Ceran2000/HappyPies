package pl.ceranka.happypies.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

internal val HappyPiesShapes = Shapes(
    extraSmall = RoundedCornerShape(18.dp), // text fields
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp), // cards
    large = RoundedCornerShape(28.dp), // FAB
    extraLarge = RoundedCornerShape(28.dp) // dialogs
)