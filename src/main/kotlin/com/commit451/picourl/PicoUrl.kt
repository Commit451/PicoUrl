package com.commit451.picourl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.isSuccess
import java.io.Closeable
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Shorten your URLs, powered by tinyurl.com.
 */
class PicoUrl private constructor(
    private val baseUrl: String,
    private val param: String,
    private val client: HttpClient,
    private val ownsClient: Boolean,
) : Closeable {

    class Builder {
        private var baseUrl: String? = null
        private var param: String? = null
        private var client: HttpClient? = null

        fun baseUrl(baseUrl: String) = apply {
            this.baseUrl = baseUrl
        }

        fun client(client: HttpClient?) = apply {
            this.client = client
        }

        fun tinyQueryParam(param: String?) = apply {
            this.param = param
        }

        fun build(): PicoUrl {
            val requiredBaseUrl = baseUrl ?: error("You need to specify a base url")
            val requiredParam = param ?: DEFAULT_QUERY_PARAM_TINY_URL
            val configuredClient = client ?: picoUrlHttpClient()
            return PicoUrl(
                baseUrl = requiredBaseUrl,
                param = requiredParam,
                client = configuredClient,
                ownsClient = client == null,
            )
        }
    }

    suspend fun generate(url: URI): URI = URI(generate(url.toString()))

    suspend fun generate(url: String): String {
        val tinyResponse = client.get(TinyUrl.BASE_URL + TinyUrl.CREATE_PATH) {
            parameter("url", url)
        }

        if (!tinyResponse.status.isSuccess()) {
            error("Unable to generate TinyUrl. Response status: ${tinyResponse.status}")
        }

        val tinyUrl: String = tinyResponse.body<String>().trim()
        val tinyPath = URI(tinyUrl).path.removePrefix("/")
        check(tinyPath.isNotBlank()) { "TinyUrl response did not contain a path: $tinyUrl" }

        return URLBuilder(baseUrl)
            .apply {
                parameters.append(param, tinyPath)
            }
            .buildString()
    }

    suspend fun parse(url: URI): URI = URI(parse(url.toString()))

    suspend fun parse(url: String): String {
        val tinyUrlPath = Url(url).parameters[param]
            ?: throw IllegalArgumentException("Passed url does not contain a tiny url param")
        val followUrl = TinyUrl.BASE_URL + tinyUrlPath

        return backpedalRedirectsTillYouDie(followUrl = followUrl, expectedBaseUrl = baseUrl)
            ?: throw RedirectNotFoundException()
    }

    override fun close() {
        if (ownsClient) {
            client.close()
        }
    }

    private suspend fun backpedalRedirectsTillYouDie(
        followUrl: String,
        expectedBaseUrl: String,
        maxRedirects: Int = 20,
    ): String? {
        var nextUrl = followUrl

        repeat(maxRedirects) {
            val response = client.get(nextUrl)

            response.request.url.toString()
                .takeIf { it.contains(expectedBaseUrl) }
                ?.let { return decodeUrl(it) }

            val location = response.headers[HttpHeaders.Location]
            if (location == null) {
                return null
            }

            val resolvedUrl = resolveUrl(nextUrl, location)
            if (resolvedUrl.contains(expectedBaseUrl)) {
                return decodeUrl(resolvedUrl)
            }

            if (!response.isRedirect()) {
                return null
            }
            nextUrl = resolvedUrl
        }

        return null
    }

    private fun HttpResponse.isRedirect(): Boolean {
        return status.value in 300..399
    }

    private fun resolveUrl(currentUrl: String, location: String): String {
        return URI(currentUrl).resolve(location).toString()
    }

    private fun decodeUrl(url: String): String {
        return URLDecoder.decode(url, StandardCharsets.UTF_8)
    }

    companion object {
        private const val DEFAULT_QUERY_PARAM_TINY_URL = "tinyUrl"
    }
}
