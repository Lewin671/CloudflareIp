import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

class IpScanner {
    companion object {

        private val ipType: IpType = IPV6

        private val BASE_IP =
            if (ipType is IPV6) {
                "2606:4700:"
            } else {
                "104.21.79."
            }

        private val ipNumber = if (ipType is IPV6) {
            65535
        } else {
            254
        }
    }

    private val client = RequestUtil.getClient()
    private val set = ConcurrentSkipListSet<String>()

    // 处理完的请求
    private val dealtNumber = AtomicInteger(0)

    fun run() {
        println("start to scan ip")
        val timeCost = measureTimeMillis {
            generateIpList()
                .parallelStream()
                .forEach {
                    doRequestAsync(it)
                }
            while (dealtNumber.get() < ipNumber) {
                Thread.yield()
            }
        }
        println("finish scanning ip, using $timeCost ms")

        println("start to write location set")
        Logger.write(set.toString())
        println("finish writing location set")

        Logger.close()
        client.dispatcher.executorService.shutdown();   //清除并关闭线程池
    }


    // 发起同步请求
    private fun doRequest(ip: String) {
        val url = getUrl(ip)
        try {
            val response = client.newCall(RequestUtil.getRequest(url)).execute()
            onSucceed(response, ip)
        } catch (e: Exception) {
            println("ip $ip error!")
        }

        println("dealt number is ${dealtNumber.incrementAndGet()}")
    }

    private fun doRequestAsync(ip: String) {
        val url = getUrl(ip)
        client.newCall(RequestUtil.getRequest(url))
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    println("ip $ip error!")
                    println("dealt number is ${dealtNumber.incrementAndGet()}")
                }

                override fun onResponse(call: Call, response: Response) {
                    onSucceed(response, ip)
                    println("dealt number is ${dealtNumber.incrementAndGet()}")
                }
            })
    }

    private fun onSucceed(response: Response, ip: String) {
        if (response.body != null) {
            val responseText = response.body!!.string()

            if (responseText.isNotEmpty()) {
                val location = getLocation(responseText)
                Logger.write("ip: $ip, location: $location")
                set.add(location)
                println("request $ip is ok")
            } else {
                println("empty response from $ip")
            }
        } else {
            println("$ip response body is null")
        }
    }

    // 从response文本中获取地点
    private fun getLocation(responseText: String): String {
        return responseText.split(System.lineSeparator())[6].split("=")[1]
    }

    // int -> hex
    private fun getHexInt(value: Int): String {
        return Integer.toHexString(value)
    }

    // 生成IP列表
    private fun generateIpList(start: Int = 1, end: Int = ipNumber + 1): List<String> {
        val result = ArrayList<String>()

        for (i in start until end) {
            val ip = getIp(i)
            result.add(ip)
        }
        return result
    }

    // 获取IP
    private fun getIp(i: Int): String {
        return when (ipType) {
            is IPV4 -> BASE_IP + i
            is IPV6 -> BASE_IP + getHexInt(i) + "::"
        }
    }

    // 获取URL
    private fun getUrl(ip: String): String {
        return when (ipType) {
            is IPV4 -> "http://$ip:/cdn-cgi/trace"
            is IPV6 -> "http://[$ip]:/cdn-cgi/trace"
        }
    }

}

fun main() {
    IpScanner().run()
}