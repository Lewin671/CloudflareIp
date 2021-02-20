import java.io.BufferedWriter
import java.io.FileWriter
import java.util.concurrent.locks.ReentrantLock

/**
 * Log to file
 */
object Logger {
    // 日志存储路径
    private const val PATH = "/tmp/ipv6List.txt"

    private val out = BufferedWriter(FileWriter(PATH, false))
    private val lock = ReentrantLock()


    // 同步写操作
    fun write(msg: String) {
        lock.lock()
        out.write(msg + System.lineSeparator())
        out.flush()
        lock.unlock()
    }

    fun close() {
        out.close()
    }

}