package com.commit451.picourl

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRedirect

internal fun picoUrlHttpClient(): HttpClient {
    return HttpClient(CIO) {
        install(HttpRedirect)
    }
}
