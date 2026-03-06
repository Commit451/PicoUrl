package com.commit451.picourl

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PicoUrlTest {

    @Test
    fun `generate creates tiny query param URL`() = runTest {
        val client = HttpClient(
            MockEngine { _ ->
                respond(
                    content = "https://tinyurl.com/abc123",
                    status = HttpStatusCode.OK,
                )
            },
        )

        val picoUrl = PicoUrl.Builder()
            .baseUrl("https://mysite.com")
            .client(client)
            .build()

        val generated = picoUrl.generate("https://mysite.com/?arg1=hi&arg2=there")

        assertEquals("https://mysite.com?tinyUrl=abc123", generated)
    }

    @Test
    fun `parse follows tinyurl redirect`() = runTest {
        val client = HttpClient(
            MockEngine { request ->
                if (request.url.host == "tinyurl.com") {
                    respond(
                        content = "",
                        status = HttpStatusCode.MovedPermanently,
                        headers = headersOf(HttpHeaders.Location, "https://mysite.com/?id=someId&ref=userId"),
                    )
                } else {
                    respond(
                        content = "",
                        status = HttpStatusCode.OK,
                    )
                }
            },
        )

        val picoUrl = PicoUrl.Builder()
            .baseUrl("https://mysite.com")
            .client(client)
            .build()

        val parsed = picoUrl.parse("https://mysite.com?tinyUrl=abc123")

        assertEquals("https://mysite.com/?id=someId&ref=userId", parsed)
    }

    @Test
    fun `parse throws when tiny query param is missing`() = runTest {
        val client = HttpClient(
            MockEngine { _ ->
                respond(content = "", status = HttpStatusCode.OK)
            },
        )

        val picoUrl = PicoUrl.Builder()
            .baseUrl("https://mysite.com")
            .client(client)
            .build()

        assertFailsWith<IllegalArgumentException> {
            picoUrl.parse("https://mysite.com?id=someId")
        }
    }
}
