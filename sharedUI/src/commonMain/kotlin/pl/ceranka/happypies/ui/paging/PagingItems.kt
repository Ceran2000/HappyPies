package pl.ceranka.happypies.ui.paging

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import pl.ceranka.happypies.ui.common.ErrorWithRetry
import pl.ceranka.happypies.ui.common.toUserMessage

val LazyPagingItems<*>.isEmpty: Boolean get() = itemCount == 0

val LazyPagingItems<*>.isRefreshing: Boolean get() = loadState.refresh is LoadState.Loading && !isEmpty

fun <T : Any> LazyListScope.pagingItems(
    items: LazyPagingItems<T>,
    key: (T) -> Any,
    contentType: String,
    emptyContent: @Composable () -> Unit,
    itemContent: @Composable (T) -> Unit
) {
    val refresh = items.loadState.refresh
    val append = items.loadState.append
    val isEmpty = items.isEmpty

    when (refresh) {
        is LoadState.Loading -> if (isEmpty) item(contentType = "progress") { PagingProgress() }
        is LoadState.Error -> {
            item(contentType = "error") { PagingError(refresh, items) }
            return
        }
        is LoadState.NotLoading -> if (isEmpty) item(contentType = "empty") { emptyContent() }
    }
    items(
        count = items.itemCount,
        key = items.itemKey(key),
        contentType = items.itemContentType { contentType }
    ) { index ->
        items[index]?.let { itemContent(it) }
    }
    when (append) {
        is LoadState.Loading -> item(contentType = "progress") { PagingProgress() }
        is LoadState.Error -> item(contentType = "error") { PagingError(append, items) }
        is LoadState.NotLoading -> Unit
    }
}

@Composable
private fun PagingError(error: LoadState.Error, items: LazyPagingItems<*>) {
    ErrorWithRetry(error.error.toUserMessage(), onRetry = items::retry)
}

@Composable
private fun PagingProgress() {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}