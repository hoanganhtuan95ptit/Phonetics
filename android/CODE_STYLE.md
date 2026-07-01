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

## 2. Giới hạn độ sâu thụt dòng theo dấu `(` (Max Parentheses Indent Depth)

Không được để biểu thức bị thụt dòng theo quá **2 cặp `()`**. Nếu vượt quá, phải tách thành biến trung gian hoặc hàm riêng.

**❌ Sai — lồng `(` quá sâu:**
```kotlin
val result = mapper.map(
    repository.load(
        requestFactory.create(
            user.id,
            config
        )
    )
)
```

**✅ Đúng — tách biến trung gian:**
```kotlin
val request = requestFactory.create(user.id, config)
val response = repository.load(request)
val result = mapper.map(response)
```

**✅ Đúng — tách hàm khi logic có ý nghĩa riêng:**
```kotlin
val result = mapper.map(loadUserResponse(user, config))

private fun loadUserResponse(user: User, config: Config): Response {

    val request = requestFactory.create(user.id, config)
    return repository.load(request)
}
```

---

## 3. Cách 1 dòng sau dấu `{`

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

## 4. Không dùng Callback — dùng Flow hoặc Coroutines

Callback gây callback hell và khó đọc. Thay thế bằng **Kotlin Coroutines** hoặc **Flow**.

### 4.1 Thay Callback đơn bằng `suspend fun`

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

### 4.2 Thay stream / listener bằng `Flow`

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

### 4.3 Wrap API Java callback sẵn có bằng `suspendCoroutine`

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
| Độ sâu lồng `{}` | Tối đa 2 cấp; nếu hơn → tách hàm |
| Độ sâu thụt dòng theo `()` | Tối đa 2 cấp; nếu hơn → tách biến / tách hàm |
| Sau dấu `{` | Luôn có 1 dòng trắng |
| Async | Dùng `suspend` / `Flow`; **không** dùng callback |
