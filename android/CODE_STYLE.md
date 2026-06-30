# Code Style Guide

## 1. Giới hạn độ sâu lồng nhau (Max Nesting Depth)

Không được lồng quá **2 cặp `{}`**. Nếu vượt quá, phải tách thành hàm riêng.

**❌ Sai — lồng quá sâu:**
```kotlin
fun process(user: User?) {
    if (user != null) {
        if (user.isActive) {
            for (item in user.items) {
                if (item.isValid) {
                    // logic ở đây — đã 4 cấp
                }
            }
        }
    }
}
```

**✅ Đúng — tách hàm:**
```kotlin
fun process(user: User?) {
    val activeUser = user?.takeIf { it.isActive } ?: return
    activeUser.items.forEach { processItem(it) }
}

fun processItem(item: Item) {
    if (!item.isValid) return
    // logic ở đây
}
```

---

## 2. Cách 1 dòng sau dấu `{`

Sau mỗi dấu `{` mở khối (hàm, class, if, for, lambda...) phải có **1 dòng trắng** trước khi bắt đầu nội dung.

**❌ Sai:**
```kotlin
fun loadData() {
    val result = repository.fetch()
    render(result)
}
```

**✅ Đúng:**
```kotlin
fun loadData() {

    val result = repository.fetch()
    render(result)
}
```

```kotlin
if (isReady) {

    start()
}
```

---

## 3. Không dùng Callback — dùng Flow hoặc Coroutines

Callback gây callback hell và khó đọc. Thay thế bằng **Kotlin Coroutines** hoặc **Flow**.

### 3.1 Thay Callback đơn bằng `suspend fun`

**❌ Sai — callback:**
```kotlin
fun fetchUser(id: String, onSuccess: (User) -> Unit, onError: (Throwable) -> Unit) {
    api.getUser(id, object : Callback<User> {
        override fun onResponse(user: User) = onSuccess(user)
        override fun onFailure(e: Throwable) = onError(e)
    })
}
```

**✅ Đúng — suspend:**
```kotlin
suspend fun fetchUser(id: String): User {
    return api.getUser(id)
}
```

### 3.2 Thay stream / listener bằng `Flow`

**❌ Sai — callback liên tục:**
```kotlin
database.observeUsers(object : Listener<List<User>> {
    override fun onChange(users: List<User>) {
        updateUI(users)
    }
})
```

**✅ Đúng — Flow:**
```kotlin
fun observeUsers(): Flow<List<User>> = database.usersFlow()

// Collect trong ViewModel / UI
viewModelScope.launch {

    observeUsers().collect { users ->
        updateUI(users)
    }
}
```

### 3.3 Wrap API Java callback sẵn có bằng `suspendCoroutine`

Khi buộc phải làm việc với thư viện Java dùng callback, wrap lại thành suspend:

```kotlin
suspend fun legacyFetch(id: String): Data = suspendCoroutine { cont ->

    legacyApi.fetch(id, object : LegacyCallback {
        override fun onSuccess(data: Data) = cont.resume(data)
        override fun onError(e: Exception) = cont.resumeWithException(e)
    })
}
```

---

## Tóm tắt nhanh

| Quy tắc | Yêu cầu |
|---|---|
| Độ sâu lồng `{}` | Tối đa 3 cấp; nếu hơn → tách hàm |
| Sau dấu `{` | Luôn có 1 dòng trắng |
| Async | Dùng `suspend` / `Flow`; **không** dùng callback |
