import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun <T> rememberPagedListState(
    list: List<T>,
    hasNext: Boolean
): PagedList<T> {
    return remember(list, hasNext) {
        PagedList(
            actualList = list,
            hasNext = hasNext
        )
    }
}

inline fun <T> checkAndCallLoadMore(
    isInPreviewMode: Boolean,
    index: Int,
    isManualRefresh: Boolean,
    pagedList: PagedList<T>,
    prefetchTriggerCount: Int,
    loadMore: ((index: Int) -> Unit),
) {
    if (isInPreviewMode) {
        return
    }
    if (NetworkUtils.isNetAvailable && pagedList.canRequestMore(
            isManualRefresh = isManualRefresh,
            prefetchTriggerCount = prefetchTriggerCount,
            index = index
        )
    ) {
        pagedList.updateHasCallLoadMore(true)
        loadMore(pagedList.loaderIndexForApi())
    }
}

fun <T> LazyListScope.pagedItemsIndexed(
    pagedList: PagedList<T>,
    key: ((index: Int, item: T) -> String),
    loadMore: ((index: Int) -> Unit),
    itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit,
    prefetchTriggerCount: Int = 25,
    noInternetContent: @Composable ((index: Int, retry: () -> Unit) -> Unit)? = null,
    loaderContent: @Composable (
        LazyItemScope.(
            index: Int,
        ) -> Unit
    )? = null,
) {
    // Note: itemCount means (no of items + loader item)
    items(
        count = pagedList.size,
        key = { index ->
            if (pagedList.isLoaderItem(index)) {
                PagedList.LoaderKey
            } else {
                key(index, pagedList[index])
            }
        },
        itemContent = { index ->
            val isInPreviewMode = isInPreviewMode
            checkAndCallLoadMore(
                isInPreviewMode = isInPreviewMode,
                index = index,
                isManualRefresh = false,
                pagedList = pagedList,
                prefetchTriggerCount = prefetchTriggerCount,
                loadMore = loadMore
            )
            if (pagedList.isLoaderItem(index)) {
                handleLoaderItem(
                    index = index,
                    loaderContent = loaderContent,
                    noInternetContent = noInternetContent,
                    onManualRefresh = {
                        checkAndCallLoadMore(
                            isInPreviewMode = isInPreviewMode,
                            index = index,
                            isManualRefresh = true,
                            pagedList = pagedList,
                            prefetchTriggerCount = prefetchTriggerCount,
                            loadMore = loadMore
                        )
                    }
                )
            } else {
                val initialTime = System.nanoTime()
                itemContent(index, pagedList[index])
                log("paging", "time taken to checkAndCallLoadMore: ${(System.nanoTime() - initialTime)}")
            }
        }
    )
}

@Composable
private fun LazyItemScope.handleLoaderItem(
    index: Int,
    loaderContent: @Composable() (LazyItemScope.(index: Int) -> Unit)? = null,
    noInternetContent: @Composable() ((index: Int, retry: () -> Unit) -> Unit)? = null,
    onManualRefresh: () -> Unit
) {
    val showLoader = remember { mutableStateOf(NetworkUtils.isNetAvailable) }

    if (showLoader.value) {
        if (loaderContent != null) {
            loaderContent(index = index)
        } else {
            DefaultLoaderItem(index = index)
        }
    } else {
        val context = LocalContext.current
        val text = stringResource(R.string.no_internet_connection)
        val retry = {
            showLoader.value = NetworkUtils.isNetAvailable
            if (NetworkUtils.isNetAvailable) {
                onManualRefresh()
            } else {
                context.toast(text)
            }
        }
        if (noInternetContent != null) {
            noInternetContent(index = index, retry = retry)
        } else {
            DefaultNoInternetContent(index = index, retry = retry)
        }
    }
}

@Preview
@Composable
fun DefaultNoInternetContent() {
    ZPScaffold(title = "", onBackPressed = { }) {
        DefaultLoaderItem(index = 0)
    }
}

@Composable
fun DefaultNoInternetContent(
    index: Int,
    retry: () -> Unit
) {
    val modifier = if (index == 0) {
        Modifier
            .fillMaxHeight()
            .fillMaxWidth()
    } else {
        Modifier
            .fillMaxWidth()
            .height(50.dp)
    }
    Box(
        modifier = modifier.clickable {
            retry()
        },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = R.string.retry),
            style = LocalTextStyle.current.copy(
                fontSize = 14.sp,
                color = MaterialTheme.colors.onPrimary
            ),
        )
    }

    val showToast = remember { mutableStateOf(false) }

    if (showToast.value) {
        LocalContext.current.toast(R.string.no_internet_connection)
    }

    LaunchedEffect(index, block = {
        showToast.value = true
    })
}

@Composable
fun DefaultLoaderItem(
    index: Int,
) {
    val modifier = if (index == 0) {
        Modifier
            .fillMaxHeight()
            .fillMaxWidth()
    } else {
        Modifier
            .fillMaxWidth()
            .height(50.dp)
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
