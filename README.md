# paging_compose
Paging in compose without using Android paging adapter to make pagination simple

Instead of using itemsIndexed, use pagedItemsIndexed and pass necessary arguments. That's all

* I felt the official one adds more complexity, hard to understand and difficult to debug, so created a simple one
* Using it in a production app that has more than 2 million active users, have not faced any issues until now
Note - This is not a published SDK, just copy pasted the relevant code here. It might not have latest code changes from the actual project
