package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.AuthorDto
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.UUID

object AuthorTable : UUIDTable("author") {
    val name: Column<String> = varchar("name", 1000)
    val biography: Column<String?> = varchar("biography", 5000).nullable()
    val dateOfBirth: Column<String?> = varchar("date_of_birth", 100).nullable()
    val dateOfDeath: Column<String?> = varchar("date_of_death", 100).nullable()
    val image: Column<String?> = varchar("image", 1000).nullable()
    val creationDate = timestamp("creation_date")
    val modificationDate = timestamp("modification_date")
    val notes: Column<String?> = varchar("notes", 5000).nullable()
    val officialPage: Column<String?> = varchar("official_page", 5000).nullable()
    val wikipediaPage: Column<String?> = varchar("wikipedia_page", 5000).nullable()
    val goodreadsPage: Column<String?> = varchar("goodreads_page", 5000).nullable()
    val twitterPage: Column<String?> = varchar("twitter_page", 5000).nullable()
    val facebookPage: Column<String?> = varchar("facebook_page", 5000).nullable()
    val instagramPage: Column<String?> = varchar("instagram_page", 5000).nullable()
}
class Author(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Author>(AuthorTable)
    var name by AuthorTable.name
    var creationDate by AuthorTable.creationDate
    var modificationDate by AuthorTable.modificationDate
    var biography by AuthorTable.biography
    var dateOfBirth by AuthorTable.dateOfBirth
    var dateOfDeath by AuthorTable.dateOfDeath
    var image by AuthorTable.image
    var notes by AuthorTable.notes
    var officialPage by AuthorTable.officialPage
    var wikipediaPage by AuthorTable.wikipediaPage
    var goodreadsPage by AuthorTable.goodreadsPage
    var twitterPage by AuthorTable.twitterPage
    var facebookPage by AuthorTable.facebookPage
    var instagramPage by AuthorTable.instagramPage

    fun toAuthorDto(): AuthorDto =
        AuthorDto(
            id = this.id.value,
            creationDate = this.creationDate,
            modificationDate = this.modificationDate,
            name = this.name,
            biography = this.biography,
            dateOfBirth = this.dateOfBirth,
            dateOfDeath = this.dateOfDeath,
            image = this.image,
            notes = this.notes,
            officialPage = this.officialPage,
            wikipediaPage = this.wikipediaPage,
            goodreadsPage = this.goodreadsPage,
            twitterPage = this.twitterPage,
            facebookPage = this.facebookPage,
            instagramPage = this.instagramPage
        )
}
object BookAuthors : Table(name = "book_authors") {
    val book = reference("book", BookTable, fkName = "fk_bookauthors_book_id", onUpdate = ReferenceOption.CASCADE, onDelete = ReferenceOption.CASCADE)
    val author = reference("author", AuthorTable, fkName = "fk_bookauthors_author_id", onUpdate = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(book, author, name = "pk_bookauthor_act")
}
object BookTranslators : Table(name = "book_translators") {
    val book = reference("book", BookTable, fkName = "fk_booktranslators_book_id", onUpdate = ReferenceOption.CASCADE, onDelete = ReferenceOption.CASCADE)
    val translator = reference("translator", AuthorTable, fkName = "fk_booktranslators_translator_id", onUpdate = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(book, translator, name = "pk_booktranslator_act")
}
