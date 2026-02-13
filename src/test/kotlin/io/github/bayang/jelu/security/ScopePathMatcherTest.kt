package io.github.bayang.jelu.security

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScopePathMatcherTest {
    // Books endpoints - READ scope
    @Test
    fun `test books GET with books read scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/books",
                "GET",
                listOf("books:read"),
            ),
        )
    }

    @Test
    fun `test books GET with path parameters and books read scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/books/123",
                "GET",
                listOf("books:read"),
            ),
        )
    }

    @Test
    fun `test books GET without required scope returns false`() {
        assertFalse(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/books",
                "GET",
                listOf("reading:read"),
            ),
        )
    }

    @Test
    fun `test books GET with multiple scopes including required returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/books",
                "GET",
                listOf("reading:read", "books:read", "reviews:read"),
            ),
        )
    }

    // Books endpoints - WRITE scope
    @Test
    fun `test books POST with books write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/books",
                "POST",
                listOf("books:write"),
            ),
        )
    }

    @Test
    fun `test books PUT with books write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/books",
                "PUT",
                listOf("books:write"),
            ),
        )
    }

    @Test
    fun `test books DELETE with books write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/books",
                "DELETE",
                listOf("books:write"),
            ),
        )
    }

    @Test
    fun `test books POST with only read scope returns false`() {
        assertFalse(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/books",
                "POST",
                listOf("books:read"),
            ),
        )
    }

    // Authors endpoints
    @Test
    fun `test authors GET with books read scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/authors",
                "GET",
                listOf("books:read"),
            ),
        )
    }

    @Test
    fun `test authors POST with books write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/authors",
                "POST",
                listOf("books:write"),
            ),
        )
    }

    @Test
    fun `test authors PUT with books write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/authors",
                "PUT",
                listOf("books:write"),
            ),
        )
    }

    @Test
    fun `test authors DELETE with books write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/authors",
                "DELETE",
                listOf("books:write"),
            ),
        )
    }

    // Tags endpoints
    @Test
    fun `test tags GET with books read scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/tags",
                "GET",
                listOf("books:read"),
            ),
        )
    }

    @Test
    fun `test tags POST with books write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/tags",
                "POST",
                listOf("books:write"),
            ),
        )
    }

    // Series endpoints
    @Test
    fun `test series GET with books read scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/series",
                "GET",
                listOf("books:read"),
            ),
        )
    }

    @Test
    fun `test series POST with books write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/series",
                "POST",
                listOf("books:write"),
            ),
        )
    }

    // Publishers endpoints
    @Test
    fun `test publishers GET with books read scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/publishers",
                "GET",
                listOf("books:read"),
            ),
        )
    }

    // Reading events - userbooks
    @Test
    fun `test userbooks GET with reading read scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/userbooks",
                "GET",
                listOf("reading:read"),
            ),
        )
    }

    @Test
    fun `test userbooks POST with reading write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/userbooks",
                "POST",
                listOf("reading:write"),
            ),
        )
    }

    @Test
    fun `test userbooks PUT with reading write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/userbooks",
                "PUT",
                listOf("reading:write"),
            ),
        )
    }

    @Test
    fun `test userbooks DELETE with reading write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/userbooks",
                "DELETE",
                listOf("reading:write"),
            ),
        )
    }

    @Test
    fun `test userbooks POST with only read scope returns false`() {
        assertFalse(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/userbooks",
                "POST",
                listOf("reading:read"),
            ),
        )
    }

    // Reading events
    @Test
    fun `test reading-events GET with reading read scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/reading-events",
                "GET",
                listOf("reading:read"),
            ),
        )
    }

    @Test
    fun `test reading-events POST with reading write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/reading-events",
                "POST",
                listOf("reading:write"),
            ),
        )
    }

    // Reviews
    @Test
    fun `test reviews GET with reviews read scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/reviews",
                "GET",
                listOf("reviews:read"),
            ),
        )
    }

    @Test
    fun `test reviews POST with reviews write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/reviews",
                "POST",
                listOf("reviews:write"),
            ),
        )
    }

    @Test
    fun `test reviews PUT with reviews write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/reviews",
                "PUT",
                listOf("reviews:write"),
            ),
        )
    }

    @Test
    fun `test reviews DELETE with reviews write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/reviews",
                "DELETE",
                listOf("reviews:write"),
            ),
        )
    }

    @Test
    fun `test reviews POST with only read scope returns false`() {
        assertFalse(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/reviews",
                "POST",
                listOf("reviews:read"),
            ),
        )
    }

    // Lists - shelves
    @Test
    fun `test shelves GET with lists read scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/shelves",
                "GET",
                listOf("lists:read"),
            ),
        )
    }

    @Test
    fun `test shelves POST with lists write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/shelves",
                "POST",
                listOf("lists:write"),
            ),
        )
    }

    @Test
    fun `test shelves DELETE with lists write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/shelves",
                "DELETE",
                listOf("lists:write"),
            ),
        )
    }

    // Lists - quotes
    @Test
    fun `test quotes GET with lists read scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/quotes",
                "GET",
                listOf("lists:read"),
            ),
        )
    }

    @Test
    fun `test quotes POST with lists write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/quotes",
                "POST",
                listOf("lists:write"),
            ),
        )
    }

    @Test
    fun `test quotes PUT with lists write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/quotes",
                "PUT",
                listOf("lists:write"),
            ),
        )
    }

    @Test
    fun `test quotes DELETE with lists write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/quotes",
                "DELETE",
                listOf("lists:write"),
            ),
        )
    }

    // Lists - custom-lists
    @Test
    fun `test custom-lists GET with lists read scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/custom-lists",
                "GET",
                listOf("lists:read"),
            ),
        )
    }

    @Test
    fun `test custom-lists POST with lists write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/custom-lists",
                "POST",
                listOf("lists:write"),
            ),
        )
    }

    @Test
    fun `test custom-lists PUT with lists write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/custom-lists",
                "PUT",
                listOf("lists:write"),
            ),
        )
    }

    @Test
    fun `test custom-lists DELETE with lists write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/custom-lists",
                "DELETE",
                listOf("lists:write"),
            ),
        )
    }

    // Import/Export
    @Test
    fun `test imports GET with import write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/imports",
                "GET",
                listOf("import:write"),
            ),
        )
    }

    @Test
    fun `test imports POST with import write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/imports",
                "POST",
                listOf("import:write"),
            ),
        )
    }

    @Test
    fun `test files GET with import write scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/files",
                "GET",
                listOf("import:write"),
            ),
        )
    }

    // Metadata
    @Test
    fun `test metadata GET with metadata read scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/metadata",
                "GET",
                listOf("metadata:read"),
            ),
        )
    }

    @Test
    fun `test metadata POST with metadata read scope returns true`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/metadata",
                "POST",
                listOf("metadata:read"),
            ),
        )
    }

    // Case insensitivity tests
    @Test
    fun `test method matching is case insensitive for GET`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/books",
                "get",
                listOf("books:read"),
            ),
        )
    }

    @Test
    fun `test method matching is case insensitive for POST`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/books",
                "post",
                listOf("books:write"),
            ),
        )
    }

    @Test
    fun `test method matching is case insensitive for PUT`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/books",
                "Put",
                listOf("books:write"),
            ),
        )
    }

    @Test
    fun `test method matching is case insensitive for DELETE`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/books",
                "DeLeTe",
                listOf("books:write"),
            ),
        )
    }

    // Default deny tests - security critical
    @Test
    fun `test unmapped path returns false by default`() {
        assertFalse(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/unmapped-endpoint",
                "GET",
                listOf("books:read"),
            ),
        )
    }

    @Test
    fun `test unmapped path returns false even with all scopes`() {
        assertFalse(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/unmapped-endpoint",
                "GET",
                TokenScope.allScopeNames(),
            ),
        )
    }

    @Test
    fun `test empty path returns false`() {
        assertFalse(
            ScopePathMatcher.hasRequiredScope(
                "",
                "GET",
                listOf("books:read"),
            ),
        )
    }

    @Test
    fun `test empty scopes returns false`() {
        assertFalse(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/books",
                "GET",
                emptyList(),
            ),
        )
    }

    @Test
    fun `test invalid scope name returns false`() {
        assertFalse(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/books",
                "GET",
                listOf("invalid:scope"),
            ),
        )
    }

    @Test
    fun `test path with different method returns false`() {
        assertFalse(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/books",
                "POST",
                listOf("books:read"),
            ),
        )
    }

    @Test
    fun `test partial path match does not grant access`() {
        assertFalse(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/book", // Missing 's', should not match /api/v1/books
                "GET",
                listOf("books:read"),
            ),
        )
    }

    // Path prefix matching tests
    @Test
    fun `test path with trailing slash matches`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/books/",
                "GET",
                listOf("books:read"),
            ),
        )
    }

    @Test
    fun `test path with query parameters matches`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/books?page=1&size=10",
                "GET",
                listOf("books:read"),
            ),
        )
    }

    @Test
    fun `test path with multiple segments matches`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/books/123/reviews",
                "GET",
                listOf("books:read"),
            ),
        )
    }

    // Edge cases
    @Test
    fun `test root API path returns false`() {
        assertFalse(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1",
                "GET",
                listOf("books:read"),
            ),
        )
    }

    @Test
    fun `test non-API path returns false`() {
        assertFalse(
            ScopePathMatcher.hasRequiredScope(
                "/health",
                "GET",
                listOf("books:read"),
            ),
        )
    }

    @Test
    fun `test different API version returns false`() {
        assertFalse(
            ScopePathMatcher.hasRequiredScope(
                "/api/v2/books",
                "GET",
                listOf("books:read"),
            ),
        )
    }

    // Multiple scopes scenarios
    @Test
    fun `test user with both read and write scopes can read`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/books",
                "GET",
                listOf("books:read", "books:write"),
            ),
        )
    }

    @Test
    fun `test user with both read and write scopes can write`() {
        assertTrue(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/books",
                "POST",
                listOf("books:read", "books:write"),
            ),
        )
    }

    @Test
    fun `test user with unrelated scopes cannot access endpoint`() {
        assertFalse(
            ScopePathMatcher.hasRequiredScope(
                "/api/v1/books",
                "GET",
                listOf("reading:read", "reviews:read", "lists:read"),
            ),
        )
    }
}
