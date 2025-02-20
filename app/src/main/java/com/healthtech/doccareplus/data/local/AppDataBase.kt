package com.healthtech.doccareplus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.healthtech.doccareplus.data.local.dao.CategoryDao
import com.healthtech.doccareplus.data.local.dao.DoctorDao
import com.healthtech.doccareplus.data.local.entity.CategoryEntity
import com.healthtech.doccareplus.data.local.entity.DoctorEntity

/*
Trong Room Database, ta cần một class mở rộng RoomDatabase để quản lý các bảng (entities) và các DAO.
@Database(entities = [CategoryEntity::class], version = 1) → Chỉ định bảng (table) nào sẽ được lưu trong database.
Kế thừa từ RoomDatabase → Đây là lớp trừu tượng, Room sẽ tự động triển khai khi database được tạo.
abstract fun categoryDao(): CategoryDao → Hàm này giúp Room biết rằng DAO nào được liên kết với database.

Room Database (AppDataBase)
    Là lớp trung gian giữa SQLite và ứng dụng.
    Chứa các bảng (CategoryEntity) và DAO (CategoryDao).
    categoryDao() là abstract function để Room tự tạo code.

Để hiểu rõ hơn về vai trò của **AppDatabase** trong Room, bạn có thể xem xét các điểm sau:

---

## 1. Vai Trò của AppDatabase

**AppDatabase** là lớp trung tâm của hệ thống Room. Nó có các vai trò chính như sau:

- **Quản lý kết nối với SQLite**: Lớp này chịu trách nhiệm mở kết nối, quản lý các phiên làm việc với cơ sở dữ liệu SQLite.
- **Khai báo các Entity**: Trong annotation `@Database`, bạn liệt kê các entity (bảng) mà database sẽ chứa. Ví dụ:

```kotlin
@Database(entities = [CategoryEntity::class], version = 1)
abstract class AppDataBase : RoomDatabase() {
    // ...
}
```

- **Khai báo các DAO**: Bạn cần khai báo tất cả các DAO mà bạn muốn sử dụng thông qua các phương thức abstract trong AppDatabase. Mỗi phương thức này trả về một instance của DAO. Ví dụ:

```kotlin
abstract fun categoryDao(): CategoryDao
```

Điều này có nghĩa là nếu bạn có nhiều DAO (ví dụ: `ProductDao`, `UserDao`, v.v.), bạn cần khai báo tất cả chúng trong lớp AppDatabase:

```kotlin
@Database(entities = [CategoryEntity::class, ProductEntity::class, UserEntity::class], version = 1)
abstract class AppDataBase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun userDao(): UserDao
}
```

**Tóm lại:**
- **Có**, tất cả các DAO và Entity mà bạn cần sử dụng trong ứng dụng đều phải được khai báo (Entity trong annotation `@Database` và DAO dưới dạng các phương thức abstract) trong AppDatabase.

---

## 2. Cách AppDatabase Được Gọi Và Sử Dụng

Khi bạn muốn sử dụng database, bạn không tạo instance của AppDatabase bằng `new` (hoặc `constructor`) trực tiếp mà sử dụng **Room** để xây dựng nó. Ví dụ:

```kotlin
Room.databaseBuilder(context, AppDataBase::class.java, "app_database")
.build()
```

Điều này cho phép Room tự động tạo một instance của AppDatabase, quản lý vòng đời và đảm bảo rằng bạn chỉ có một instance duy nhất (thông qua pattern Singleton nếu bạn muốn).

---

## 3. "categoryDao() là abstract function để Room tự tạo code" Có Nghĩa Là Gì?

Trong lớp **AppDatabase**, bạn khai báo một hàm abstract như sau:

```kotlin
abstract fun categoryDao(): CategoryDao
```

Điều này có nghĩa rằng bạn **không cần** (và không thể) cài đặt hàm này trong AppDatabase. Thay vào đó:

- **Room sẽ tự động tạo ra một lớp con của AppDatabase** trong quá trình biên dịch (compile time).
- Trong lớp con này, Room sẽ **cài đặt (implement) tất cả các hàm abstract** bạn đã khai báo, bao gồm `categoryDao()`.
- Khi bạn gọi `appDatabase.categoryDao()`, bạn sẽ nhận được một instance của **CategoryDao** đã được Room tạo ra. Instance này chứa code thực thi các truy vấn được khai báo trong CategoryDao (chẳng hạn như các annotation `@Query`, `@Insert`, v.v.).

Ví dụ, bạn chỉ cần gọi:

```kotlin
val categoryDao = appDatabase.categoryDao()
```

Sau đó, bạn có thể sử dụng `categoryDao` để truy cập database mà không cần phải viết bất cứ code nào khác để quản lý kết nối hoặc thực thi SQL.

---

## 4. Tóm Lại

- **AppDatabase** là trung tâm của hệ thống Room. Nó quản lý các Entity và DAO của ứng dụng.
- Tất cả các Entity bạn muốn lưu trữ phải được khai báo trong annotation `@Database` của AppDatabase.
- Tất cả các DAO bạn cần sử dụng phải được khai báo dưới dạng các phương thức abstract trong AppDatabase.
- **Room** sẽ tự động tạo ra một lớp con của AppDatabase trong thời gian biên dịch, cài đặt các phương thức abstract này để trả về các instance DAO có thể thực thi các truy vấn.
- Khi bạn sử dụng Hilt, bạn có thể cung cấp AppDatabase và các DAO thông qua module, giúp việc inject phụ thuộc trở nên đơn giản.

Hy vọng rằng với phần giải thích trên, bạn đã có cái nhìn rõ ràng hơn về cách hoạt động của AppDatabase và vai trò của nó trong hệ thống Room của Android. Nếu còn phần nào chưa rõ, bạn cứ hỏi thêm nhé!

*/

@Database(
    entities = [CategoryEntity::class, DoctorEntity::class],
    version = 1
)
abstract class AppDataBase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun doctorDao(): DoctorDao
}