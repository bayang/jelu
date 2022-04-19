package io.github.bayang.jelu.errors

import java.lang.Exception

class JeluAuthenticationException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}
