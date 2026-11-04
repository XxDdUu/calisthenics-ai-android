package com.calisthenics.ai.data.remote.websocket

data class StompFrame(
    val command: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String = ""
) {
    fun toWireString(): String {
        val sb = StringBuilder()
        sb.append(command).append("\n")
        headers.forEach { (key, value) ->
            sb.append(key).append(":").append(value).append("\n")
        }
        sb.append("\n")
        sb.append(body)
        sb.append("\u0000")
        return sb.toString()
    }

    companion object {
        fun parse(wireText: String): StompFrame? {
            val trimmed = wireText.trimStart()
            if (trimmed.isEmpty() || trimmed == "\n" || trimmed == "\r\n") {
                // Heartbeat ping
                return null
            }

            val nullIndex = wireText.indexOf('\u0000')
            val content = if (nullIndex >= 0) wireText.substring(0, nullIndex) else wireText
            val lines = content.lines()
            if (lines.isEmpty()) return null

            val command = lines[0].trim()
            val headers = mutableMapOf<String, String>()
            var lineIdx = 1

            while (lineIdx < lines.size) {
                val line = lines[lineIdx]
                if (line.isEmpty() || line == "\r") {
                    lineIdx++
                    break
                }
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    val key = line.substring(0, colonIdx).trim()
                    val value = line.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
                lineIdx++
            }

            val body = if (lineIdx < lines.size) {
                lines.subList(lineIdx, lines.size).joinToString("\n")
            } else {
                ""
            }

            return StompFrame(command = command, headers = headers, body = body)
        }
    }
}
