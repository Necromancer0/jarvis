package com.jarvis.mobile.core.routing

import com.jarvis.mobile.core.model.LlamaCppProvider
import com.jarvis.mobile.core.model.RemoteOpenAiProvider
import com.jarvis.mobile.data.settings.SettingsRepository
import com.jarvis.mobile.util.Logx
import kotlinx.coroutines.flow.first

/**
 * Agent router (spec: AGENT ROUTER / MODEL FALLBACK LADDER):
 *   local model → optional remote → deterministic rule engine (always available).
 */
class ModelRouter(
    private val settings: SettingsRepository,
    private val llama: LlamaCppProvider,
    private val remote: RemoteOpenAiProvider,
) {
    enum class Route { LOCAL, REMOTE, RULES }

    data class Decision(val route: Route, val reason: String)

    suspend fun decide(forceLocalOnly: Boolean = false): Decision {
        val snap = settings.snapshot()
        val localReady = llama.isReady()

        // LOCAL ONLY and unattended routines must never use remote inference.
        if (snap.localOnly || forceLocalOnly) {
            return selectRoute(
                localOnly = snap.localOnly,
                forceLocalOnly = forceLocalOnly,
                apiMode = snap.apiMode,
                localReady = localReady,
                remoteReady = false,
            )
        }

        val checkRemote = snap.apiMode || !localReady
        val remoteReady = if (checkRemote) {
            remote.refreshConfig()
            remote.isReady()
        } else {
            false
        }

        return selectRoute(
            localOnly = false,
            forceLocalOnly = false,
            apiMode = snap.apiMode,
            localReady = localReady,
            remoteReady = remoteReady,
        )
    }

    suspend fun generate(
        prompt: String,
        maxTokens: Int,
        stopSequences: List<String> = emptyList(),
        grammar: String? = null,
        forceLocalOnly: Boolean = false,
    ): Pair<Result<String>, Decision> {
        val d = decide(forceLocalOnly)
        return when (d.route) {
            // Grammar-constrained decoding is a local-runtime feature: the native
            // sampler owns it. Remote providers ignore it (API mode has its own
            // JSON-mode controls we do not manage here).
            Route.LOCAL -> llama.generate(prompt, maxTokens, stopSequences, grammar) to d
            Route.REMOTE -> remote.generate(prompt, maxTokens, stopSequences) to d
            Route.RULES -> Result.failure<String>(IllegalStateException("no LLM route available")) to d
        }
    }

    fun cancelLocal() = llama.cancel()

    companion object {
        internal fun selectRoute(
            localOnly: Boolean,
            forceLocalOnly: Boolean,
            apiMode: Boolean,
            localReady: Boolean,
            remoteReady: Boolean,
        ): Decision {
            if (localOnly || forceLocalOnly) {
                return if (localReady) {
                    Decision(
                        Route.LOCAL,
                        if (forceLocalOnly) "Routine local-only mode" else "LOCAL ONLY mode",
                    )
                } else {
                    Decision(
                        Route.RULES,
                        "Local-only mode: no on-device model; deterministic engine only",
                    )
                }
            }

            if (apiMode && remoteReady) {
                return Decision(Route.REMOTE, "API mode: remote provider")
            }

            if (localReady) {
                return Decision(Route.LOCAL, "on-device model")
            }

            if (remoteReady) {
                return Decision(Route.REMOTE, "remote provider configured")
            }

            return Decision(
                Route.RULES,
                "No usable model provider; using deterministic engine",
            )
        }

        init {
            Logx.i("router", "ModelRouter ready")
        }
    }
}
