package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.errors.JeluException
import org.jetbrains.exposed.dao.exceptions.EntityNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import javax.validation.ConstraintViolationException

@ControllerAdvice
class GlobalControllerExceptionHandler {

    @ExceptionHandler(ConstraintViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun handleConstraintValidationException(
        e: ConstraintViolationException
    ): ApiError =
        ApiError(
            "Constraint violation",
            e.constraintViolations.map { ApiValidationErrorItem(it.propertyPath.toString(), it.message) }
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun handleMethodArgumentNotValidException(
        e: MethodArgumentNotValidException
    ): ApiError =
        ApiError(
            "Validation error(s)",
            e.bindingResult.fieldErrors.map { ApiValidationErrorItem(it.field, it.defaultMessage) }
        )

    @ExceptionHandler(EntityNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    fun handleNonExistingEntityException(
        e: EntityNotFoundException
    ): ApiError =
        ApiError(e.message)

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun handleHttpMessageNotReadableException(
        e: HttpMessageNotReadableException
    ): ApiError =
        ApiError(e.message)

    @ExceptionHandler(JeluException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    fun handleJeluException(
        e: JeluException
    ): ApiError =
        ApiError(e.message)
}

data class ApiError(
    val message: String? = null,
    val violations: List<ApiValidationErrorItem> = emptyList()
)

data class ApiValidationErrorItem(
    val field: String? = null,
    val error: String? = null
)
