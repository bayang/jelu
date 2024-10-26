package io.github.bayang.jelu.config

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.security.web.authentication.WebAuthenticationDetails

class UserAgentWebAuthenticationDetails : WebAuthenticationDetails {

    var userAgent: String = ""

    constructor(remoteAddress: String?, sessionId: String?, userAgent: String?) : super(remoteAddress, sessionId) {
        if (userAgent != null) {
            this.userAgent = userAgent
        }
    }

    constructor(request: HttpServletRequest) : super(request) {
        this.userAgent = request.getHeader(HttpHeaders.USER_AGENT).orEmpty()
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(javaClass.simpleName).append(" [")
        sb.append("RemoteIpAddress=").append(this.remoteAddress).append(", ")
        sb.append("UserAgent=").append(this.userAgent).append(", ")
        sb.append("SessionId=").append(this.sessionId).append("]")
        return sb.toString()
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.ANY,
)
class UserAgentWebAuthenticationDetailsMixin @JsonCreator constructor(
    @JsonProperty("remoteAddress") remoteAddress: String?,
    @JsonProperty("sessionId") sessionId: String?,
    @JsonProperty("userAgent") userAgent: String?,
)
