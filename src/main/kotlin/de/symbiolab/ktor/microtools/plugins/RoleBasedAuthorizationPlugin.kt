package de.symbiolab.ktor.microtools.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.application.call
import io.ktor.server.application.plugin
import io.ktor.server.auth.Principal
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext
import io.ktor.server.routing.application
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase
import mu.KLogging

class RoleBasedAuthorizationPlugin {
    companion object Plugin : BaseApplicationPlugin<ApplicationCallPipeline, Unit, RoleBasedAuthorizationPlugin>,
        KLogging() {
        override val key = AttributeKey<RoleBasedAuthorizationPlugin>("RoleBasedAuthorization")

        val AuthorizationPhase = PipelinePhase("Authorization")

        override fun install(
            pipeline: ApplicationCallPipeline,
            configure: Unit.() -> Unit
        ): RoleBasedAuthorizationPlugin {
            return RoleBasedAuthorizationPlugin()
        }
    }

    inline fun <reified T : Enum<T>> intercept(
        pipeline: ApplicationCallPipeline,
        expectedRoles: Set<T>,
    ) {
        pipeline.insertPhaseAfter(ApplicationCallPipeline.Plugins, PipelinePhase("Challenge"))
        pipeline.insertPhaseBefore(ApplicationCallPipeline.Call, AuthorizationPhase)

        pipeline.intercept(AuthorizationPhase) {
            val maybeRole = when (val principal = call.authentication.principal) {
                is JWTPrincipal -> principal["role"]
                is Principal -> principal.toString()
                else -> null
            }

            if (maybeRole == null) {
                call.respond(HttpStatusCode.Unauthorized)
                finish()
            } else {
                try {
                    val actualRole = enumValueOf<T>(maybeRole)

                    if (!expectedRoles.contains(actualRole)) {
                        logger.debug { "Role $maybeRole not found in roles: ${enumValues<T>()}" }
                        call.respond(HttpStatusCode.Forbidden)
                        finish()
                    }
                } catch (e: IllegalArgumentException) {
                    logger.warn { "Cannot parse jwt role '$maybeRole'" }

                    call.respond(HttpStatusCode.Forbidden, "Cannot parse token role")
                    finish()
                }
            }
        }
    }
}

class AuthorizedRouteSelector : RouteSelector() {
    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
        RouteSelectorEvaluation.Constant
}

inline fun <reified T : Enum<T>> Route.authorizedRoute(expectedRoles: Set<T>, build: Route.() -> Unit): Route {
    val authorizedRoute = createChild(AuthorizedRouteSelector())
    application.plugin(RoleBasedAuthorizationPlugin).intercept(authorizedRoute, expectedRoles)
    authorizedRoute.build()
    return authorizedRoute
}

inline fun <reified T : Enum<T>> Route.withRoles(vararg expectedRoles: T, build: Route.() -> Unit) =
    authorizedRoute(expectedRoles.toSet(), build)
