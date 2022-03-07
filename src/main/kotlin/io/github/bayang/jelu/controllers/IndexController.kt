package io.github.bayang.jelu.controllers

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class IndexController {
    @GetMapping("/")
    fun index(model: Model): String {
        return "/index.html"
    }
}
