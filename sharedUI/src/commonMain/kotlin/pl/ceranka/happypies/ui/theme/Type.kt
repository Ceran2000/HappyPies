package pl.ceranka.happypies.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.nunito_bold
import happypies.sharedui.generated.resources.nunito_extrabold
import happypies.sharedui.generated.resources.nunito_regular
import happypies.sharedui.generated.resources.nunito_semibold
import org.jetbrains.compose.resources.Font

@Composable
private fun nunitoFamily() = FontFamily(
    Font(Res.font.nunito_regular, FontWeight.Normal),
    Font(Res.font.nunito_semibold, FontWeight.SemiBold),
    Font(Res.font.nunito_bold, FontWeight.Bold),
    Font(Res.font.nunito_extrabold, FontWeight.ExtraBold)
)

@Composable
internal fun happyPiesTypography(): Typography {
    val nunito = nunitoFamily()
    val base = Typography()
    fun TextStyle.with(weight: FontWeight) = copy(fontFamily = nunito, fontWeight = weight)
    return Typography(
        displayLarge = base.displayLarge.with(FontWeight.ExtraBold),
        displayMedium = base.displayMedium.with(FontWeight.ExtraBold),
        displaySmall = base.displaySmall.with(FontWeight.ExtraBold),
        headlineLarge = base.headlineLarge.with(FontWeight.ExtraBold),
        headlineMedium = base.headlineMedium.with(FontWeight.ExtraBold),
        headlineSmall = base.headlineSmall.with(FontWeight.ExtraBold),
        titleLarge = base.titleLarge.with(FontWeight.ExtraBold),
        titleMedium = base.titleMedium.with(FontWeight.ExtraBold).copy(fontSize = 17.sp),
        titleSmall = base.titleSmall.with(FontWeight.ExtraBold),
        bodyLarge = base.bodyLarge.with(FontWeight.SemiBold),
        bodyMedium = base.bodyMedium.with(FontWeight.SemiBold),
        bodySmall = base.bodySmall.with(FontWeight.SemiBold),
        labelLarge = base.labelLarge.with(FontWeight.Bold),
        labelMedium = base.labelMedium.with(FontWeight.Bold),
        labelSmall = base.labelSmall.with(FontWeight.Bold)
    )
}