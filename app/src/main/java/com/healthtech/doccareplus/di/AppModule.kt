package com.healthtech.doccareplus.di

import android.content.Context
import android.net.ConnectivityManager
import com.healthtech.doccareplus.data.remote.api.PaymentApiClient
import com.healthtech.doccareplus.utils.NetworkUtils
import com.healthtech.doccareplus.utils.NotificationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/*
@InstallIn(SingletonComponent::class) //nghĩa là các dependency trong module này sẽ tồn tại xuyên suốt vòng đời của ứng dụng.

@Singleton //Là một scope annotation, đảm bảo rằng chỉ có một instance duy nhất của dependency trong phạm vi của component mà nó được cung cấp.

-----

Mặc dù cùng là `@Singleton`, nhưng cách hoạt động trong 2 trường hợp này có sự khác biệt tinh tế:

1. **@Singleton trên class (DoctorRepositoryImpl)**:
```kotlin
@Singleton
class DoctorRepositoryImpl @Inject constructor(
    private val firebaseApi: FirebaseApi,
    private val localDataSource: DoctorLocalDataSource
) : DoctorRepository
```

- Đánh dấu rằng class này **có thể** được quản lý như một singleton
- Chỉ là một "marker" để Hilt biết rằng class này có thể được tạo như singleton
- **Không tự động** làm cho class trở thành singleton
- Cần phải được sử dụng kết hợp với `@Singleton` trong module để có hiệu lực

2. **@Singleton trong Module (AppModule)**:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton  // <- Đây mới là quyết định thực sự
    fun provideConnectivityManager(@ApplicationContext context: Context): ConnectivityManager {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
}
```

- **Thực sự** làm cho dependency trở thành singleton
- Hilt sẽ chỉ tạo một instance duy nhất và tái sử dụng nó
- Có hiệu lực ngay lập tức không cần thêm annotation khác

3. **Mối quan hệ giữa hai @Singleton**:

```kotlin
// Case 1: Đúng - Cả hai nơi đều có @Singleton
@Singleton  // Trên class
class DoctorRepositoryImpl @Inject constructor(...) : DoctorRepository

@Binds
@Singleton  // Trong module
abstract fun bindDoctorRepository(impl: DoctorRepositoryImpl): DoctorRepository

// Case 2: Sai - Có thể gây lỗi compile
@Singleton  // Trên class
class DoctorRepositoryImpl @Inject constructor(...) : DoctorRepository

@Binds  // Thiếu @Singleton trong module
abstract fun bindDoctorRepository(impl: DoctorRepositoryImpl): DoctorRepository

// Case 3: Đúng - Không cần @Singleton trên class nếu không cần
class DoctorRepositoryImpl @Inject constructor(...) : DoctorRepository

@Binds
@Singleton  // Chỉ cần trong module là đủ
abstract fun bindDoctorRepository(impl: DoctorRepositoryImpl): DoctorRepository
```

4. **Best Practices**:

- Nên đặt `@Singleton` ở cả hai nơi nếu muốn class là singleton:
```kotlin
@Singleton  // Marker cho class
class DoctorRepositoryImpl @Inject constructor(...) : DoctorRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton  // Thực sự làm cho nó thành singleton
    abstract fun bindDoctorRepository(impl: DoctorRepositoryImpl): DoctorRepository
}
```

- Điều này giúp:
  1. Code rõ ràng hơn về ý định sử dụng
  2. Tránh lỗi khi refactor
  3. Dễ dàng debug và maintain

5. **Lưu ý quan trọng**:
- `@Singleton` chỉ có hiệu lực trong phạm vi của component được install (`SingletonComponent` trong trường hợp này)
- Nếu module được install trong component khác, `@Singleton` sẽ không hoạt động
- Scope của dependency phải match với scope của component chứa nó


 */

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideConnectivityManager(@ApplicationContext context: Context): ConnectivityManager {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    @Provides
    @Singleton
    fun provideNetworkUtils(connectivityManager: ConnectivityManager): NetworkUtils {
        return NetworkUtils(connectivityManager)
    }

    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper {
        return NotificationHelper(context)
    }

    @Provides
    @Singleton
    fun providePaymentApiClient(@ApplicationContext context: Context): PaymentApiClient {
        return PaymentApiClient(context)
    }
}