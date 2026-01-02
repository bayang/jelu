package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.dto.CreateShelfDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.ShelfDto
import io.github.bayang.jelu.service.ShelfService
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class ShelvesController(
    private val shelvesService: ShelfService,
) {
    @GetMapping(path = ["/shelves"])
    fun shelves(
        @RequestParam(name = "name", required = false) name: String?,
        @RequestParam(name = "targetId", required = false) targetId: UUID?,
        principal: Authentication,
    ): List<ShelfDto> = shelvesService.find((principal.principal as JeluUser).user, name, targetId)

    @GetMapping(path = ["/shelves/{id}"])
    fun shelfById(
        @PathVariable("id") shelfId: UUID,
        principal: Authentication,
    ): ShelfDto = shelvesService.findById(shelfId)

    @PostMapping(path = ["/shelves"])
    fun saveShelf(
        @RequestBody @Valid
        createShelfDto: CreateShelfDto,
        principal: Authentication,
    ): ShelfDto = shelvesService.save(createShelfDto, (principal.principal as JeluUser).user)

    @ApiResponse(responseCode = "204", description = "Deleted the shelf")
    @DeleteMapping(path = ["/shelves/{id}"])
    fun deleteShelfById(
        @PathVariable("id") shelfId: UUID,
    ): ResponseEntity<Unit> {
        shelvesService.delete(shelfId)
        return ResponseEntity.noContent().build()
    }
}
