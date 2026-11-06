package pl.ceranka.happypies.ui.paging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems

@Composable
fun PagingLazyColumn(
    items: LazyPagingItems<*>,
    snackbarHostState: SnackbarHostState,
    showOfflineMessage: Boolean,
    onOfflineMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit
) {
    if (showOfflineMessage) {
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar("Brak połączenia z internetem. Lista może być nieaktualna.")
            onOfflineMessageShown()
        }
    }

    PullToRefreshBox(
        isRefreshing = items.isRefreshing,
        onRefresh = items::refresh,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
}