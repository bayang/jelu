package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.ReviewDto
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.UUID

object ReviewTable : UUIDTable("review") {
    val creationDate = timestamp("creation_date")
    val modificationDate = timestamp("modification_date")
    val reviewDate = timestamp("review_date")
    val user = reference("user", UserTable, onDelete = ReferenceOption.CASCADE)
    val book = reference("book", BookTable, onDelete = ReferenceOption.CASCADE)
    val text: Column<String> = varchar("text", 500000)
    val rating: Column<Double> = double(name = "rating")
    val visibility = enumerationByName("visibility", 200, Visibility::class)
}
class Review(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Review>(ReviewTable)
    var creationDate by ReviewTable.creationDate
    var modificationDate by ReviewTable.modificationDate
    var reviewDate by ReviewTable.reviewDate
    var user by User referencedOn ReviewTable.user
    var book by Book referencedOn ReviewTable.book
    var text by ReviewTable.text
    var rating by ReviewTable.rating
    var visibility by ReviewTable.visibility

    fun toReviewDto(): ReviewDto = ReviewDto(
        id = this.id.value,
        creationDate = this.creationDate,
        modificationDate = this.modificationDate,
        reviewDate = this.reviewDate,
        text = this.text,
        visibility = this.visibility,
        rating = this.rating,
        user = this.user.id.value,
        book = this.book.id.value,
    )
}
enum class Visibility {
    PUBLIC,
    PRIVATE,
}
