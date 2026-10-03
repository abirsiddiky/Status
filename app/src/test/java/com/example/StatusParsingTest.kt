package com.example

import com.example.data.StatusApi
import com.example.data.model.StatusDto
import com.example.data.model.StatusMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusParsingTest {

    private val sampleJson = """
    {"cpu":{"model":"ARMv7 Processor rev 4 (v7l)","utilisation":0.0,"temperatures":[],"frequencies":{"cpu0":{"now":null,"min":null,"base":null,"max":null},"cpu1":{"now":null,"min":null,"base":null,"max":null}},"count":4,"cache":null,"cores":4},
    "memory":{"total":984170,"available":789185,"cached":286994,"swap_total":0,"swap_available":0,"processes":120},
    "storage":{"OS":{"icon":"settings","total":3632794828.8,"available":2089771008}},
    "network":{"interface":"eth0","speed":100,"rx":15040272,"tx":5705022},
    "host":{"uptime":32330.78,"os":"Debian GNU/Linux 12 (bookworm)","hostname":"debian","app_memory":"23116","loadavg":[0.05,0.16,0.08]}}
    """.trimIndent()

    @Test
    fun `parse real server response accurately and defensively`() {
        val dto = StatusApi.json.decodeFromString<StatusDto>(sampleJson)

        assertNotNull(dto.cpu)
        assertEquals("ARMv7 Processor rev 4 (v7l)", dto.cpu?.model)
        assertEquals(4, dto.cpu?.cores)
        assertEquals(0.0, dto.cpu?.utilisation ?: -1.0, 0.001)

        val (status, rates) = StatusMapper.map(
            dto = dto,
            previousRxBytes = 15000000L,
            previousTxBytes = 5700000L,
            previousTimestamp = 1000L,
            currentTimestamp = 4000L // 3 seconds elapsed
        )

        // CPU validation
        assertEquals("ARMv7 Processor rev 4 (v7l)", status.cpu.model)
        assertEquals(0f, status.cpu.loadPercent, 0.01f)
        assertFalse(status.cpu.hasTemperature)
        assertFalse(status.cpu.hasFrequencies) // all frequencies were null

        // Memory validation (values converted from kB to bytes)
        assertEquals(984170L * 1024L, status.memory.totalBytes)
        assertEquals(789185L * 1024L, status.memory.availableBytes)
        val expectedInUse = (984170L - 789185L) * 1024L
        assertEquals(expectedInUse, status.memory.inUseBytes)
        assertFalse(status.memory.hasSwap) // swap_total was 0
        assertEquals(120, status.memory.processCount)

        // Storage validation
        assertTrue(status.storage.hasVolumes)
        val osVol = status.storage.volumes.first { it.name == "OS" }
        assertEquals(3632794828.8, osVol.totalBytes, 0.01)
        assertEquals(2089771008.0, osVol.availableBytes, 0.01)

        // Network validation
        assertEquals("eth0", status.network.interfaceName)
        assertEquals(100.0, status.network.linkSpeedMbit ?: 0.0, 0.01)
        // Rate: (15040272 - 15000000) bytes / 3 seconds = 13424 bytes/sec
        assertEquals(13424.0, status.network.rxRateBps, 1.0)

        // Host validation
        assertEquals("debian", status.host.hostname)
        assertEquals("Debian GNU/Linux 12 (bookworm)", status.host.os)
        assertEquals(3, status.host.loadAvg.size)
    }

    @Test
    fun `parse empty or missing json without crashing`() {
        val emptyDto = StatusApi.json.decodeFromString<StatusDto>("{}")
        val (status, _) = StatusMapper.map(
            dto = emptyDto,
            previousRxBytes = null,
            previousTxBytes = null,
            previousTimestamp = null,
            currentTimestamp = System.currentTimeMillis()
        )

        assertEquals("N/A", status.cpu.model)
        assertEquals(0f, status.cpu.loadPercent, 0.01f)
        assertFalse(status.cpu.hasTemperature)
        assertEquals(0L, status.memory.totalBytes)
        assertFalse(status.storage.hasVolumes)
        assertEquals("N/A", status.network.interfaceName)
        assertEquals("N/A", status.host.hostname)
    }
}
