package io.github.bayang.jelu.service

interface IQuoteProvider {

    fun quotes(query: String): List<String>

}