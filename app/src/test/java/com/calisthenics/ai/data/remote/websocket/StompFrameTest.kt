package com.calisthenics.ai.data.remote.websocket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StompFrameTest {

    @Test
    fun parseMessageFrameExtractsHeadersAndBody() {
        val raw = "MESSAGE\nsubscription:sub-1\ndestination:/topic/conversations/42\ncontent-type:application/json\n\n{\"id\":10,\"content\":\"Hello\"}\u0000"
        val frame = StompFrame.parse(raw)

        assertNotNull(frame)
        assertEquals("MESSAGE", frame!!.command)
        assertEquals("sub-1", frame.headers["subscription"])
        assertEquals("/topic/conversations/42", frame.headers["destination"])
        assertEquals("{\"id\":10,\"content\":\"Hello\"}", frame.body)
    }

    @Test
    fun parseHeartbeatReturnsNull() {
        val frame = StompFrame.parse("\n")
        assertNull(frame)
    }

    @Test
    fun toWireStringFormatsValidStompFrame() {
        val frame = StompFrame(
            command = "SUBSCRIBE",
            headers = mapOf(
                "id" to "sub-0",
                "destination" to "/user/queue/messages"
            )
        )
        val wire = frame.toWireString()
        assertEquals("SUBSCRIBE\nid:sub-0\ndestination:/user/queue/messages\n\n\u0000", wire)
    }
}
