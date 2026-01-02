package io.github.bayang.jelu.service

import io.github.bayang.jelu.dao.CustomListRepository
import io.github.bayang.jelu.dto.BookDto
import io.github.bayang.jelu.dto.CustomListDto
import io.github.bayang.jelu.dto.CustomListRemoveDto
import io.github.bayang.jelu.errors.JeluAuthenticationException
import io.github.bayang.jelu.search.LuceneEntity
import io.github.bayang.jelu.search.LuceneHelper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Component
class CustomListService(
    private val customListRepository: CustomListRepository,
    private val luceneHelper: LuceneHelper,
    private val bookService: BookService,
) {
    @Transactional
    fun save(
        createListDto: CustomListDto,
        userId: UUID,
    ): CustomListDto = customListRepository.save(createListDto, userId).toCustomListDto()

    @Transactional
    fun update(createListDto: CustomListDto): CustomListDto = customListRepository.update(createListDto).toCustomListDto()

    @Transactional
    fun find(
        user: UUID,
        name: String?,
        pageable: Pageable,
    ): Page<CustomListDto> = customListRepository.find(user, name, pageable).map { it.toCustomListDto() }

    @Transactional
    fun delete(listId: UUID) {
        customListRepository.delete(listId)
    }

    @Transactional
    fun findById(listId: UUID): CustomListDto = customListRepository.findById(listId).toCustomListDto()

    @Transactional
    fun findListBooks(
        listId: UUID,
        pageable: Pageable,
        principal: Authentication?,
    ): Page<BookDto> {
        val list = customListRepository.findById(listId)
        if (!list.public && principal == null) {
            throw JeluAuthenticationException("Resource unauthorized")
        }
        if (list == null) {
            return PageImpl(
                listOf(),
                pageable,
                0,
            )
        }
        val entitiesIds = luceneHelper.searchEntitiesIdsForTags(list.tags.split(","), LuceneEntity.Book)
        return if (entitiesIds.isNullOrEmpty()) {
            PageImpl(
                listOf(),
                pageable,
                0,
            )
        } else {
            customListRepository.findListBooks(entitiesIds, pageable).map { it.toBookDto() }
        }
    }

    @Transactional
    fun removeBooksFromList(customListRemoveDto: CustomListRemoveDto) {
        customListRemoveDto.books.forEach {
            bookService.deleteTagsFromBook(UUID.fromString(it), customListRemoveDto.tags.map { it -> UUID.fromString(it) })
        }
    }
}
