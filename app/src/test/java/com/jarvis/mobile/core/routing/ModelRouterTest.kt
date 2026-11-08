package com.jarvis.mobile.core.routing

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelRouterTest {

    @Test
    fun `local only overrides api mode and remote provider`() {
        val result = ModelRouter.selectRoute(true, false, true, false, true)
        assertEquals(ModelRouter.Route.RULES, result.route)
    }

    @Test
    fun `local only prefers device model when available`() {
        val result = ModelRouter.selectRoute(true, false, true, true, true)
        assertEquals(ModelRouter.Route.LOCAL, result.route)
    }

    @Test
    fun `routine without local model falls back to rules`() {
        val result = ModelRouter.selectRoute(false, true, true, false, true)
        assertEquals(ModelRouter.Route.RULES, result.route)
    }

    @Test
    fun `routine uses local model despite enabled api mode`() {
        val result = ModelRouter.selectRoute(false, true, true, true, true)
        assertEquals(ModelRouter.Route.LOCAL, result.route)
    }

    @Test
    fun `normal api mode can still use remote provider`() {
        val result = ModelRouter.selectRoute(false, false, true, true, true)
        assertEquals(ModelRouter.Route.REMOTE, result.route)
    }

    @Test
    fun `normal task uses local model when api mode is off`() {
        val result = ModelRouter.selectRoute(false, false, false, true, true)
        assertEquals(ModelRouter.Route.LOCAL, result.route)
    }
}
