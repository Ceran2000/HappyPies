package pl.ceranka.happypies.ui.common

import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.error_malformed_data
import happypies.sharedui.generated.resources.error_not_found
import happypies.sharedui.generated.resources.error_permission_denied
import happypies.sharedui.generated.resources.error_unavailable
import happypies.sharedui.generated.resources.error_unknown
import org.jetbrains.compose.resources.StringResource
import pl.ceranka.happypies.core.DataException

fun Throwable.toUserMessage(): StringResource = when (this) {
    is DataException.Unavailable -> Res.string.error_unavailable
    is DataException.PermissionDenied -> Res.string.error_permission_denied
    is DataException.NotFound -> Res.string.error_not_found
    is DataException.MalformedData -> Res.string.error_malformed_data
    else -> Res.string.error_unknown
}