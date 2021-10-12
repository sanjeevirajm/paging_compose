fun <T> LazyListScope.pagedItemsIndexed(
    items: PagedList<T>,
    key: ((index: Int, item: T) -> String),
    loadMore: ((index: Int) -> Unit),
    itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit,
    prefetchTriggerCount: Int = 25,
    noInternetContent: @Composable ((index: Int, retry: () -> Unit) -> Unit)? = null,
    loaderContent: @Composable (LazyItemScope.(
        index: Int,
        loadMore: ((skipCheck: Boolean) -> Unit)
    ) -> Unit)? = null,
) {
    var hasRequested = false

    fun checkAndCallLoadMore(skipCheck: Boolean = false) {
        if (!hasRequested || skipCheck) {
            hasRequested = true
            loadMore(items.indexOfLoader())
        }
    }

    items(count = items.size,
        key = { index ->
            //Note: itemCount means (no of items + loader item)
            //25 >= 51 - (25 + 1)
            if (items.hasLoader() && index >= items.size - (prefetchTriggerCount + 1)) {
                checkAndCallLoadMore()
            }
            if (items.isLoaderItem(index)) {
                PagedList.LoaderKey
            } else {
                key(index, items[index])
            }
        }, itemContent = { index ->
            if (items.isLoaderItem(index)) {
                val refreshCondition = remember { mutableStateOf(0) }

                if (refreshCondition.value > 0 || NetworkUtils.isNetAvailable) {
                    if (loaderContent != null) {
                        loaderContent(index = index, loadMore = { skipCheck ->
                            checkAndCallLoadMore(skipCheck = skipCheck)
                        })
                    } else {
                        Loader(index = index, loadMore = { skipCheck ->
                            checkAndCallLoadMore(skipCheck = skipCheck)
                        })
                    }
                } else {
                    if (noInternetContent != null) {
                        noInternetContent(index = index, retry = {
                            refreshCondition.value = refreshCondition.value++
                        })
                    } else {
                        InternetErrorContent(index = index, retry = {
                            refreshCondition.value = refreshCondition.value++
                        })
                    }
                }
            } else {
                itemContent(index, items[index])
            }
        })
}

@Composable
fun InternetErrorContent(
    index: Int,
    retry: () -> Unit
) {
    val modifier = when (index) {
        0 -> {
            Modifier
                .fillMaxHeight()
                .fillMaxWidth()
        }
        else -> {
            Modifier
                .fillMaxWidth()
                .height(50.dp)
        }
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(text = stringResource(id = R.string.retry),
            style = LocalTextStyle.current.copy(
                fontSize = 14.sp,
                color = MaterialTheme.colors.onPrimary
            ),
            modifier = modifier.clickable {
                retry()
            })
    }

    val showToast = remember { mutableStateOf(false) }

    if (showToast.value) {
        LocalContext.current.toast(R.string.no_internet_connection)
    }

    LaunchedEffect(key1 = index, block = {
        showToast.value = true
    })
}

@Composable
fun Loader(
    index: Int,
    loadMore: ((skipCheck: Boolean) -> Unit),
) {
    LaunchedEffect(key1 = index, block = {
        loadMore(false)
    })
    val modifier = when (index) {
        0 -> {
            Modifier
                .fillMaxHeight()
                .fillMaxWidth()
        }
        else -> {
            Modifier
                .fillMaxWidth()
                .height(50.dp)
        }
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgress()
    }
}
