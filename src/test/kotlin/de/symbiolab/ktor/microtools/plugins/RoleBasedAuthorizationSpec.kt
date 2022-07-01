package de.symbiolab.ktor.microtools.plugins

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.Principal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication

enum class Roles {
    ADMIN,
    USER
}

data class StringPrincipal(val role: Roles) : Principal {
    override fun toString(): String = role.name
}

fun withTestApplication(block: suspend (ApplicationTestBuilder) -> Unit) = testApplication {
    install(RoleBasedAuthorizationPlugin)

    install(Authentication) {
        basic("auth-basic") {
            realm = "auth-basic"
            validate { credentials ->
                // only user name has to match, password is irrelevant
                when (credentials.name) {
                    "admin" -> StringPrincipal(Roles.ADMIN)
                    "user" -> StringPrincipal(Roles.USER)
                    else -> null
                }
            }
        }
    }

    routing {
        authenticate("auth-basic") {
            withRoles(Roles.ADMIN) {
                get("/admin") {
                    call.respond(HttpStatusCode.OK, "admin")
                }
            }

            withRoles(Roles.USER) {
                get("/user") {
                    call.respond(HttpStatusCode.OK, "user")
                }
            }

            withRoles(Roles.ADMIN, Roles.USER) {
                get("/admin-or-user") {
                    call.respond(HttpStatusCode.OK, "admin-or-user")
                }
            }
        }
    }

    block(this)
}

class RoleBasedAuthorizationSpec : StringSpec({
    "Wrong credentials" {
        withTestApplication { app ->
            val response = app.client.get("/admin") {
                basicAuth("fkbr", "")
            }
            response.shouldHaveStatus(HttpStatusCode.Unauthorized)
        }
    }

    "Admin can access /admin" {
        withTestApplication { app ->
            val response = app.client.get("/admin") {
                basicAuth("admin", "")
            }

            response shouldHaveStatus HttpStatusCode.OK
            response.bodyAsText() shouldBe "admin"
        }
    }

    "User can access /user" {
        withTestApplication { app ->
            val response = app.client.get("/user") {
                basicAuth("user", "")
            }

            response shouldHaveStatus HttpStatusCode.OK
            response.bodyAsText() shouldBe "user"
        }
    }

    "Admin can access /admin-or-user" {
        withTestApplication { app ->
            val adminResponse = app.client.get("/admin-or-user") {
                basicAuth("admin", "")
            }

            adminResponse shouldHaveStatus HttpStatusCode.OK
            adminResponse.bodyAsText() shouldBe "admin-or-user"

            val userResponse = app.client.get("/admin-or-user") {
                basicAuth("user", "")
            }

            userResponse shouldHaveStatus HttpStatusCode.OK
            userResponse.bodyAsText() shouldBe "admin-or-user"
        }
    }
})
