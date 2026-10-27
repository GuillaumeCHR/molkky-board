package com.yogistudio.molkkyboard.crash

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CrashReportTest {
    private val context = CrashContext("1.0.0", "16 (API 36)", "Fairphone FP6", Instant.parse("2026-10-27T10:15:42Z"))

    @Test
    fun `le rapport contient la pile et le contexte, jamais les messages`() {
        val cause = IllegalStateException("équipe « Les Bouleaux » de Léa")
        val report = formatCrashReport(RuntimeException("Alice a 49 points", cause), context)
        assertTrue(report.startsWith("Molkky Board 1.0.0\nAndroid 16 (API 36) · Fairphone FP6\n2026-10-27T10:15:00Z"))
        assertTrue(report.contains("java.lang.RuntimeException"))
        assertTrue(report.contains("Caused by: java.lang.IllegalStateException"))
        assertTrue(report.contains("    at com.yogistudio.molkkyboard.crash.CrashReportTest"))
        listOf("Alice", "49 points", "Bouleaux", "Léa").forEach { assertFalse("fuite : $it", report.contains(it)) }
    }

    @Test
    fun `une cause circulaire ne boucle pas`() {
        val a = RuntimeException()
        val b = IllegalArgumentException(a)
        a.initCause(b)
        val report = formatCrashReport(a, context)
        assertTrue(report.lines().count { it.startsWith("Caused by") } <= 1)
    }
}
