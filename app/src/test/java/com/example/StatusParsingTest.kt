package com.example

import com.example.data.StatusApi
import com.example.data.model.StatusDto
import com.example.data.model.StatusMapper
import com.example.data.model.asDoubleOrNull
import com.example.data.model.asIntOrNull
import com.example.data.model.asLongOrNull
import com.example.data.model.asStringOrNull
import com.example.data.model.parseTemperatures
import com.example.util.FormatUtils
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusParsingTest {

    // Reference Linux PC server response from addendum
    private val linuxPcJson = """
    {"cpu":{"model":"Intel(R) Core(TM) i5-6500T CPU @ 2.50GHz","utilisation":0.025125628140703515,"temperatures":{"Core 0":[43.0,100.0],"Core 1":[45.0,100.0],"Core 2":[43.0,100.0],"Core 3":[43.0,100.0]},"frequencies":{"cpu0":{"now":3067,"min":800,"base":2500,"max":3100},"cpu1":{"now":800,"min":800,"base":2500,"max":3100},"cpu2":{"now":3085,"min":800,"base":2500,"max":3100},"cpu3":{"now":800,"min":800,"base":2500,"max":3100}},"count":4,"cache":6144,"cores":4},
    "memory":{"total":7426150,"available":3612103,"cached":3803877,"swap_total":7830761,"swap_available":7825256,"processes":341},
    "storage":{"OS":{"icon":"settings","total":75094818816,"available":63152017408},"Home":{"icon":"folder","total":42194698240,"available":40292311040}},
    "network":{"interface":"wlp1s0","speed":-1,"rx":754738702,"tx":34030853},
    "host":{"uptime":2866.06,"os":"Red Hat Enterprise Linux 10.2 (Coughlan)","hostname":"redhat","app_memory":"37820","loadavg":[0.18,0.56,0.58]}}
    """.trimIndent()

    // TV-box ARM server response
    private val tvBoxJson = """
    {"cpu":{"model":"ARMv7 Processor rev 4 (v7l)","utilisation":0.0,"temperatures":[],"frequencies":{"cpu0":{"now":null,"min":null,"base":null,"max":null},"cpu1":{"now":null,"min":null,"base":null,"max":null}},"count":4,"cache":null,"cores":4},
    "memory":{"total":984170,"available":789185,"cached":286994,"swap_total":0,"swap_available":0,"processes":120},
    "storage":{"OS":{"icon":"settings","total":3632794828.8,"available":2089771008}},
    "network":{"interface":"eth0","speed":100,"rx":15040272,"tx":5705022},
    "host":{"uptime":32330.78,"os":"Debian GNU/Linux 12 (bookworm)","hostname":"debian","app_memory":"23116","loadavg":[0.05,0.16,0.08]}}
    """.trimIndent()

    // --- REQUIREMENT: LINUX PC REFERENCE SERVER TEST ---

    @Test
    fun `parse Linux PC reference response with exact units and rules`() {
        val dto = StatusApi.json.decodeFromString<StatusDto>(linuxPcJson)
        val (status, _) = StatusMapper.map(
            dto = dto,
            previousRxBytes = null,
            previousTxBytes = null,
            previousTimestamp = null,
            currentTimestamp = System.currentTimeMillis()
        )

        // 1. avg temperature 43.5
        assertEquals(43.5, status.cpu.averageTemp ?: 0.0, 0.001)

        // 2. cpu load 2.5%
        assertEquals("2.5%", FormatUtils.formatPercent(status.cpu.loadPercent))

        // 3. cache "6 MB"
        assertEquals("6 MB", status.cpu.cacheInfo)

        // 4. memory total 7.08 GB
        assertEquals("7.08 GB", status.memory.totalGbFormatted)

        // 5. two storage volumes in the order OS then Home
        assertEquals(2, status.storage.volumes.size)
        assertEquals("OS", status.storage.volumes[0].name)
        assertEquals("settings", status.storage.volumes[0].iconHint)
        assertEquals("Home", status.storage.volumes[1].name)
        assertEquals("folder", status.storage.volumes[1].iconHint)

        // 6. network speed "Unknown"
        assertEquals("Unknown", status.network.linkSpeedFormatted)
        assertEquals("wlp1s0", status.network.interfaceName)

        // 7. app_memory 37820.0
        assertEquals(37820.0, status.host.appMemoryKb ?: 0.0, 0.001)

        // 8. Frequencies: 4 cores, > 1000 MHz shown as GHz
        assertEquals(4, status.cpu.frequencies.size)
        assertEquals("3.07 GHz", FormatUtils.formatFrequency(status.cpu.frequencies["cpu0"]?.now))
        assertEquals("800 MHz", FormatUtils.formatFrequency(status.cpu.frequencies["cpu1"]?.now))
    }

    // --- REQUIREMENT: TV-BOX STYLE FIXTURE TEST ---

    @Test
    fun `parse TV-box style response where temperatures is empty and frequencies and cache are null`() {
        val dto = StatusApi.json.decodeFromString<StatusDto>(tvBoxJson)
        val (status, _) = StatusMapper.map(
            dto = dto,
            previousRxBytes = null,
            previousTxBytes = null,
            previousTimestamp = null,
            currentTimestamp = System.currentTimeMillis()
        )

        // temperatures is []
        assertTrue(status.cpu.tempReadings.isEmpty())
        assertFalse(status.cpu.hasTemperature)
        assertNull(status.cpu.averageTemp)

        // all frequencies had now = null -> skipped
        assertTrue(status.cpu.frequencies.isEmpty())
        assertFalse(status.cpu.hasFrequencies)

        // cache is null -> "N/A"
        assertEquals("N/A", status.cpu.cacheInfo)

        // swap_total is 0 -> hasSwap is false
        assertFalse(status.memory.hasSwap)

        // link speed 100 Mbit/s
        assertEquals("100 Mbit/s", status.network.linkSpeedFormatted)
        assertEquals("eth0", status.network.interfaceName)

        // hostname
        assertEquals("debian", status.host.hostname)
    }

    // --- REQUIREMENT 5: parseTemperatures EXACT INPUTS ---

    @Test
    fun `parseTemperatures with empty array`() {
        val el = StatusApi.json.parseToJsonElement("[]")
        val readings = parseTemperatures(el)
        assertTrue(readings.isEmpty())
    }

    @Test
    fun `parseTemperatures with object of core names and current-limit pairs`() {
        val el = StatusApi.json.parseToJsonElement("""{"Core 0":[43.0,100.0],"Core 1":[41.0,100.0]}""")
        val readings = parseTemperatures(el)
        assertEquals(2, readings.size)

        assertEquals("Core 0", readings[0].label)
        assertEquals(43.0, readings[0].current, 0.001)
        assertEquals(100.0, readings[0].limit ?: 0.0, 0.001)

        assertEquals("Core 1", readings[1].label)
        assertEquals(41.0, readings[1].current, 0.001)
        assertEquals(100.0, readings[1].limit ?: 0.0, 0.001)
    }

    @Test
    fun `parseTemperatures with list of numbers`() {
        val el = StatusApi.json.parseToJsonElement("[43.0,41.0]")
        val readings = parseTemperatures(el)
        assertEquals(2, readings.size)

        assertEquals("Core 0", readings[0].label)
        assertEquals(43.0, readings[0].current, 0.001)
        assertNull(readings[0].limit)

        assertEquals("Core 1", readings[1].label)
        assertEquals(41.0, readings[1].current, 0.001)
        assertNull(readings[1].limit)
    }

    @Test
    fun `parseTemperatures with object containing single numeric value`() {
        val el = StatusApi.json.parseToJsonElement("""{"Package":43.5}""")
        val readings = parseTemperatures(el)
        assertEquals(1, readings.size)

        assertEquals("Package", readings[0].label)
        assertEquals(43.5, readings[0].current, 0.001)
        assertNull(readings[0].limit)
    }

    @Test
    fun `parseTemperatures with null`() {
        val readings = parseTemperatures(null)
        assertTrue(readings.isEmpty())
    }

    @Test
    fun `parseTemperatures with string text`() {
        val el = StatusApi.json.parseToJsonElement("\"text\"")
        val readings = parseTemperatures(el)
        assertTrue(readings.isEmpty())
    }

    @Test
    fun `parseTemperatures filters out-of-range temperatures`() {
        val el = StatusApi.json.parseToJsonElement("""{"Sensor A":[-100.0, 100.0], "Sensor B":[50.0, 300.0], "Sensor C":[60.0, 95.0]}""")
        val readings = parseTemperatures(el)
        val sensorA = readings.firstOrNull { it.label == "Sensor A" }
        assertNotNull(sensorA)
        assertEquals(100.0, sensorA!!.current, 0.001)
        assertNull(sensorA.limit)

        val sensorB = readings.firstOrNull { it.label == "Sensor B" }
        assertNotNull(sensorB)
        assertEquals(50.0, sensorB!!.current, 0.001)
        assertNull(sensorB.limit)

        val sensorC = readings.firstOrNull { it.label == "Sensor C" }
        assertNotNull(sensorC)
        assertEquals(60.0, sensorC!!.current, 0.001)
        assertEquals(95.0, sensorC.limit ?: 0.0, 0.001)
    }

    // --- HELPER CONVERSIONS TESTS ---

    @Test
    fun `helper conversions handle numbers and strings gracefully`() {
        assertEquals(42.5, JsonPrimitive("42.5").asDoubleOrNull() ?: 0.0, 0.001)
        assertEquals(100L, JsonPrimitive("100").asLongOrNull())
        assertEquals(10, JsonPrimitive(10).asIntOrNull())
        assertEquals("hello", JsonPrimitive("hello").asStringOrNull())
        assertNull(JsonPrimitive("invalid").asDoubleOrNull())
        assertNull(null.asDoubleOrNull())
    }

    @Test
    fun `malformed section is isolated without failing the rest`() {
        val partialJson = """
        {
            "cpu": "unexpected string instead of object",
            "memory": {"total": 500000, "available": 250000, "cached": 50000, "swap_total": 0, "swap_available": 0, "processes": 50},
            "host": {"hostname": "partial-box", "os": "Linux"}
        }
        """.trimIndent()

        val dto = StatusApi.json.decodeFromString<StatusDto>(partialJson)
        val (status, _) = StatusMapper.map(
            dto = dto,
            previousRxBytes = null,
            previousTxBytes = null,
            previousTimestamp = null,
            currentTimestamp = System.currentTimeMillis()
        )

        // CPU failed to parse because it's not a JsonObject
        assertTrue(status.partialDataSections.contains("CPU"))
        assertEquals("N/A", status.cpu.model)

        // Memory and Host parsed successfully
        assertEquals(500000L * 1024L, status.memory.totalBytes)
        assertEquals("partial-box", status.host.hostname)
        assertEquals("Linux", status.host.os)
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
        assertTrue(status.partialDataSections.isEmpty())
    }
}
