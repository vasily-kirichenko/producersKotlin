import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import io.reactivex.Flowable
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.reactive.consumeEach
import kotlinx.coroutines.experimental.reactive.publish
import kotlinx.coroutines.experimental.runBlocking
import kotlin.coroutines.experimental.suspendCoroutine

val urls = listOf("http://bing.com", "http://yahoo.com", "http://google.com", "http://msn.com")

suspend fun fetch(url: String): String = suspendCoroutine { cont ->
    try {
        url.httpGet().timeout(5000).responseString { _, _, result ->
            when (result) {
                is Result.Failure -> cont.resumeWithException(result.getException())
                is Result.Success -> cont.resume(result.value)
            }
        }
    } catch(e: Throwable) {
        cont.resumeWithException(e)
    }
}

data class Resource(val url: String, val length: Int)

suspend fun pages() = publish<Resource>(CommonPool) {
    for (url in urls) {
        try {
            send(Resource(url, fetch(url).length))
        } catch (_: Throwable) {
            send(Resource(url, -1))
        }
    }
}

fun main(args: Array<String>) = runBlocking {
    val job = launch(CommonPool) {
        pages().consumeEach {
            println("${it.url} (${it.length})")
        }
    }

    Flowable
        .fromPublisher(pages())
        .filter { it.length < 50_000 }
        .map { it.url }
        .consumeEach { println("$it is less than 50K") }

    job.join()
}












