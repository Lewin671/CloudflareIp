import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit


object RequestUtil {
    private const val TIMEOUT = 3L

    // 返回一个request对象
    fun getRequest(url: String): Request {
        return Request.Builder()
            .url(url)
            .build()
    }

    // 返回一个client对象
    fun getClient(): OkHttpClient {
        return OkHttpClient().newBuilder()
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
    }
}