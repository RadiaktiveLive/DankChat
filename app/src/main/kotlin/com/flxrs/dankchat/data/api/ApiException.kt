package com.flxrs.dankchat.data.api

import androidx.annotation.Keep
import com.flxrs.dankchat.utils.extensions.decodeOrNull
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

open class ApiException(
    open val status: HttpStatusCode,
    override val message: String?,
    override val cause: Throwable? = null
) : Throwable(message, cause) {

    override fun toString(): String {
        return "ApiException(status=$status, message=$message, cause=$cause)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ApiException) return false

        if (status != other.status) return false
        if (message != other.message) return false
        if (cause != other.cause) return false

        return true
    }

    override fun hashCode(): Int {
        var result = status.hashCode()
        result = 31 * result + (message?.hashCode() ?: 0)
        result = 31 * result + (cause?.hashCode() ?: 0)
        return result
    }

}

fun <R, T : R> Result<T>.recoverNotFoundWith(default: R): Result<R> = recoverCatching {
    when {
        it is ApiException && it.status == HttpStatusCode.NotFound -> default
        else                                                       -> throw it
    }
}

suspend fun HttpResponse.throwApiErrorOnFailure(json: Json): HttpResponse {
    if (status.isSuccess()) {
        return this
    }

    val errorBody = bodyAsText()
    val errorMessage = json.decodeOrNull<GenericError>(errorBody)?.message
    val betterStatus = HttpStatusCode.fromValue(status.value)
    val message = buildString {
        append("$betterStatus ${request.url}")
        if (!errorMessage.isNullOrBlank()) {
            append(": $errorMessage")
        }
    }

    throw ApiException(betterStatus, message)
}

@Keep
@Serializable
data class GenericError(val message: String)