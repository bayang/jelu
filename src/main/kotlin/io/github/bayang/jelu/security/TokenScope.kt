package io.github.bayang.jelu.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

/**
 * Defines the available scopes for API tokens.
 * Scopes control what operations a token can perform.
 */
enum class TokenScope(
    val scopeName: String,
    val description: String,
    val category: ScopeCategory,
) {
    BOOKS_READ("books:read", "View books, authors, tags, series, publishers", ScopeCategory.BOOKS),
    BOOKS_WRITE("books:write", "Create/modify/delete books and metadata", ScopeCategory.BOOKS),
    READING_READ("reading:read", "View reading events and statistics", ScopeCategory.READING),
    READING_WRITE("reading:write", "Create/modify/delete reading events", ScopeCategory.READING),
    REVIEWS_READ("reviews:read", "View reviews", ScopeCategory.REVIEWS),
    REVIEWS_WRITE("reviews:write", "Create/modify/delete reviews", ScopeCategory.REVIEWS),
    LISTS_READ("lists:read", "View custom lists, shelves, quotes", ScopeCategory.LISTS),
    LISTS_WRITE("lists:write", "Create/modify/delete lists, shelves, quotes", ScopeCategory.LISTS),
    IMPORT_WRITE("import:write", "Import/export data, browse filesystem", ScopeCategory.IMPORT),
    METADATA_READ("metadata:read", "Fetch external metadata from providers", ScopeCategory.METADATA),
    ;

    fun toGrantedAuthority(): GrantedAuthority = SimpleGrantedAuthority("SCOPE_$scopeName")

    companion object {
        private val scopeNameMap = entries.associateBy { it.scopeName }

        fun fromScopeName(scopeName: String): TokenScope? = scopeNameMap[scopeName]

        fun fromScopeNames(scopeNames: List<String>): List<TokenScope> = scopeNames.mapNotNull { fromScopeName(it) }

        fun toGrantedAuthorities(scopes: List<String>): List<GrantedAuthority> = fromScopeNames(scopes).map { it.toGrantedAuthority() }

        fun allScopeNames(): List<String> = entries.map { it.scopeName }

        fun isValidScope(scopeName: String): Boolean = scopeNameMap.containsKey(scopeName)

        fun validateScopes(scopeNames: List<String>): Boolean = scopeNames.all { isValidScope(it) }
    }
}

enum class ScopeCategory(
    val displayName: String,
) {
    BOOKS("Books & Metadata"),
    READING("Reading Events"),
    REVIEWS("Reviews"),
    LISTS("Lists & Shelves"),
    IMPORT("Import/Export"),
    METADATA("External Metadata"),
}

/**
 * Helper class to check if a request path matches a scope.
 */
object ScopePathMatcher {
    private val pathScopeMap =
        mapOf(
            // Books endpoints
            Pair("/api/v1/books", "GET") to listOf(TokenScope.BOOKS_READ),
            Pair("/api/v1/books", "POST") to listOf(TokenScope.BOOKS_WRITE),
            Pair("/api/v1/books", "PUT") to listOf(TokenScope.BOOKS_WRITE),
            Pair("/api/v1/books", "DELETE") to listOf(TokenScope.BOOKS_WRITE),
            Pair("/api/v1/authors", "GET") to listOf(TokenScope.BOOKS_READ),
            Pair("/api/v1/authors", "POST") to listOf(TokenScope.BOOKS_WRITE),
            Pair("/api/v1/authors", "PUT") to listOf(TokenScope.BOOKS_WRITE),
            Pair("/api/v1/authors", "DELETE") to listOf(TokenScope.BOOKS_WRITE),
            Pair("/api/v1/tags", "GET") to listOf(TokenScope.BOOKS_READ),
            Pair("/api/v1/tags", "POST") to listOf(TokenScope.BOOKS_WRITE),
            Pair("/api/v1/tags", "PUT") to listOf(TokenScope.BOOKS_WRITE),
            Pair("/api/v1/tags", "DELETE") to listOf(TokenScope.BOOKS_WRITE),
            Pair("/api/v1/series", "GET") to listOf(TokenScope.BOOKS_READ),
            Pair("/api/v1/series", "POST") to listOf(TokenScope.BOOKS_WRITE),
            Pair("/api/v1/series", "PUT") to listOf(TokenScope.BOOKS_WRITE),
            Pair("/api/v1/series", "DELETE") to listOf(TokenScope.BOOKS_WRITE),
            Pair("/api/v1/publishers", "GET") to listOf(TokenScope.BOOKS_READ),
            // User books (reading events)
            Pair("/api/v1/userbooks", "GET") to listOf(TokenScope.READING_READ),
            Pair("/api/v1/userbooks", "POST") to listOf(TokenScope.READING_WRITE),
            Pair("/api/v1/userbooks", "PUT") to listOf(TokenScope.READING_WRITE),
            Pair("/api/v1/userbooks", "DELETE") to listOf(TokenScope.READING_WRITE),
            Pair("/api/v1/reading-events", "GET") to listOf(TokenScope.READING_READ),
            Pair("/api/v1/reading-events", "POST") to listOf(TokenScope.READING_WRITE),
            Pair("/api/v1/reading-events", "PUT") to listOf(TokenScope.READING_WRITE),
            Pair("/api/v1/reading-events", "DELETE") to listOf(TokenScope.READING_WRITE),
            // Reviews
            Pair("/api/v1/reviews", "GET") to listOf(TokenScope.REVIEWS_READ),
            Pair("/api/v1/reviews", "POST") to listOf(TokenScope.REVIEWS_WRITE),
            Pair("/api/v1/reviews", "PUT") to listOf(TokenScope.REVIEWS_WRITE),
            Pair("/api/v1/reviews", "DELETE") to listOf(TokenScope.REVIEWS_WRITE),
            // Lists, shelves, quotes
            Pair("/api/v1/shelves", "GET") to listOf(TokenScope.LISTS_READ),
            Pair("/api/v1/shelves", "POST") to listOf(TokenScope.LISTS_WRITE),
            Pair("/api/v1/shelves", "DELETE") to listOf(TokenScope.LISTS_WRITE),
            Pair("/api/v1/quotes", "GET") to listOf(TokenScope.LISTS_READ),
            Pair("/api/v1/quotes", "POST") to listOf(TokenScope.LISTS_WRITE),
            Pair("/api/v1/quotes", "PUT") to listOf(TokenScope.LISTS_WRITE),
            Pair("/api/v1/quotes", "DELETE") to listOf(TokenScope.LISTS_WRITE),
            Pair("/api/v1/custom-lists", "GET") to listOf(TokenScope.LISTS_READ),
            Pair("/api/v1/custom-lists", "POST") to listOf(TokenScope.LISTS_WRITE),
            Pair("/api/v1/custom-lists", "PUT") to listOf(TokenScope.LISTS_WRITE),
            Pair("/api/v1/custom-lists", "DELETE") to listOf(TokenScope.LISTS_WRITE),
            // Import/Export
            Pair("/api/v1/imports", "GET") to listOf(TokenScope.IMPORT_WRITE),
            Pair("/api/v1/imports", "POST") to listOf(TokenScope.IMPORT_WRITE),
            Pair("/api/v1/files", "GET") to listOf(TokenScope.IMPORT_WRITE),
            // Metadata
            Pair("/api/v1/metadata", "GET") to listOf(TokenScope.METADATA_READ),
            Pair("/api/v1/metadata", "POST") to listOf(TokenScope.METADATA_READ),
        )

    /**
     * Check if the given scopes allow access to the specified path and method.
     * Returns true if access is allowed, false otherwise.
     */
    fun hasRequiredScope(
        path: String,
        method: String,
        userScopes: List<String>,
    ): Boolean {
        val userTokenScopes = TokenScope.fromScopeNames(userScopes)

        // Find matching path prefix
        for ((key, requiredScopes) in pathScopeMap) {
            val (pathPattern, httpMethod) = key
            if (path.startsWith(pathPattern) && method.equals(httpMethod, ignoreCase = true)) {
                // Check if user has at least one of the required scopes
                return requiredScopes.any { it in userTokenScopes }
            }
        }

        /*
         * Default: if path is not in map, deny by default to be secure, this means that paths
         * MUST be added to the map in order to be useable by an API token
         */
        return false
    }
}
