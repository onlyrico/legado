package io.legado.app.help.http

import io.legado.app.constant.AppConst
import io.legado.app.help.AppConfig
import io.legado.app.utils.EncodingDetect
import io.legado.app.utils.GSON
import io.legado.app.utils.UTF8BOMFighter
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun OkHttpClient.newCallResponse(
    retry: Int = 0,
    builder: Request.Builder.() -> Unit
): Response {
    return withContext(IO) {
        val requestBuilder = Request.Builder()
        requestBuilder.header(AppConst.UA_NAME, AppConfig.userAgent)
        requestBuilder.apply(builder)
        var response: Response? = null
        for (i in 0..retry) {
            response = this@newCallResponse.newCall(requestBuilder.build()).await()
            if (response.isSuccessful) {
                return@withContext response
            }
        }
        return@withContext response!!
    }
}

suspend fun OkHttpClient.newCallResponseBody(
    retry: Int = 0,
    builder: Request.Builder.() -> Unit
): ResponseBody {
    return withContext(IO) {
        val requestBuilder = Request.Builder()
        requestBuilder.header(AppConst.UA_NAME, AppConfig.userAgent)
        requestBuilder.apply(builder)
        var response: Response? = null
        for (i in 0..retry) {
            response = this@newCallResponseBody.newCall(requestBuilder.build()).await()
            if (response.isSuccessful) {
                return@withContext response.body!!
            }
        }
        return@withContext response!!.body ?: throw IOException(response.message)
    }
}

suspend fun OkHttpClient.newCallStrResponse(
    retry: Int = 0,
    builder: Request.Builder.() -> Unit
): StrResponse {
    return withContext(IO) {
        val requestBuilder = Request.Builder()
        requestBuilder.header(AppConst.UA_NAME, AppConfig.userAgent)
        requestBuilder.apply(builder)
        var response: Response? = null
        for (i in 0..retry) {
            response = this@newCallStrResponse.newCall(requestBuilder.build()).await()
            if (response.isSuccessful) {
                return@withContext StrResponse(response, response.body!!.text())
            }
        }
        return@withContext StrResponse(response!!, response.body?.text() ?: response.message)
    }
}

suspend fun Call.await(): Response = suspendCancellableCoroutine { block ->

    block.invokeOnCancellation {
        cancel()
    }

    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            block.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            block.resume(response)
        }
    })

}

fun ResponseBody.text(encode: String? = null): String {
    val responseBytes = UTF8BOMFighter.removeUTF8BOM(bytes())
    var charsetName: String? = encode

    charsetName?.let {
        return String(responseBytes, Charset.forName(charsetName))
    }

    //根据http头判断
    contentType()?.charset()?.let {
        return String(responseBytes, it)
    }

    //根据内容判断
    charsetName = EncodingDetect.getHtmlEncode(responseBytes)
    return String(responseBytes, Charset.forName(charsetName))
}

fun Request.Builder.addHeaders(headers: Map<String, String>) {
    headers.forEach {
        if (it.key == AppConst.UA_NAME) {
            //防止userAgent重复
            removeHeader(AppConst.UA_NAME)
        }
        addHeader(it.key, it.value)
    }
}

fun Request.Builder.get(url: String, queryMap: Map<String, String>, encoded: Boolean = false) {
    val httpBuilder = url.toHttpUrl().newBuilder()
    queryMap.forEach {
        if (encoded) {
            httpBuilder.addEncodedQueryParameter(it.key, it.value)
        } else {
            httpBuilder.addQueryParameter(it.key, it.value)
        }
    }
    url(httpBuilder.build())
}

fun Request.Builder.postForm(form: Map<String, String>, encoded: Boolean = false) {
    val formBody = FormBody.Builder()
    form.forEach {
        if (encoded) {
            formBody.addEncoded(it.key, it.value)
        } else {
            formBody.add(it.key, it.value)
        }
    }
    post(formBody.build())
}

fun Request.Builder.postMultipart(type: String?, form: Map<String, Any>) {
    val multipartBody = MultipartBody.Builder()
    type?.let {
        multipartBody.setType(type.toMediaType())
    }
    form.forEach {
        when (val value = it.value) {
            is Map<*, *> -> {
                val fileName = value["fileName"] as String
                val file = value["file"]
                val mediaType = (value["contentType"] as? String)?.toMediaType()
                val requestBody = when (file) {
                    is File -> {
                        file.asRequestBody(mediaType)
                    }
                    is ByteArray -> {
                        file.toRequestBody(mediaType)
                    }
                    is String -> {
                        file.toRequestBody(mediaType)
                    }
                    else -> {
                        GSON.toJson(file).toRequestBody(mediaType)
                    }
                }
                multipartBody.addFormDataPart(it.key, fileName, requestBody)
            }
            else -> multipartBody.addFormDataPart(it.key, it.value.toString())
        }
    }
    post(multipartBody.build())
}

fun Request.Builder.postJson(json: String?) {
    json?.let {
        val requestBody = json.toRequestBody("application/json; charset=UTF-8".toMediaType())
        post(requestBody)
    }
}