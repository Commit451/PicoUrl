# PicoUrl

Create tiny shareable URLs that can be parsed back into the original URLs.

This is now a **pure Kotlin/JVM** library and uses **Ktor client + CIO**.

## Dependency

```kotlin
dependencies {
    implementation("com.commit451:picourl:latest.version.here")
}
```

## Usage

```kotlin
import com.commit451.picourl.PicoUrl

suspend fun demo() {
    val picoUrl = PicoUrl.Builder()
        .baseUrl("https://mysite.com")
        .build()

    val shareUrl = "https://mysite.com?id=someId&referer=userId&unicorns=true"

    // Turns this into something like: https://mysite.com?tinyUrl=j71a1h
    val shortened = picoUrl.generate(shareUrl)

    // Parse it back to the original URL
    val parsed = picoUrl.parse(shortened)

    println(shortened)
    println(parsed)

    picoUrl.close()
}
```

## About

This library is powered by [tinyurl](https://tinyurl.com/).
Please read their [terms of service](https://tinyurl.com/app/terms).

License
--------

    Copyright 2017 Commit 451

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
