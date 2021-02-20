import java.util.concurrent.ConcurrentSkipListSet
import kotlin.system.measureTimeMillis

class IpScanner {
    companion object {
        private const val BASE_IP = "2606:4700:"
        private val ipType: IpType = IPV6
        private val ipNumber = if (ipType is IPV6) {
            65536
        } else {
            255
        }
    }

    private val client = RequestUtil.getClient()
    private val set = ConcurrentSkipListSet<String>()

    fun run() {
        println("start to scan ip")
        val timeCost = measureTimeMillis {
            generateIpList()
                .parallelStream()
                .forEach {
                    doRequest(it)
                }
        }
        println("finish scanning ip, using $timeCost ms")

        println("start to write location set")
        Logger.write(set.toString())
        println("finish writing location set")

        Logger.close()
    }


    // 发起同步请求
    private fun doRequest(ip: String) {
        val url = getUrl(ip)
        try {
            val response = client.newCall(RequestUtil.getRequest(url)).execute()

            if (response.body != null) {
                val location = getLocation(response.body!!.string())
                Logger.write("ip: $ip, location: $location")
                set.add(location)
            } else {
                println("$ip response body is null")
            }
        } catch (e: Exception) {
            println("ip $ip error!")
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
    private fun generateIpList(start: Int = 1, end: Int = ipNumber): List<String> {
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