package xyz.block.moneyaddress.nostr

import app.cash.nostrino.client.RelayClient
import app.cash.nostrino.client.RelaySet
import app.cash.nostrino.crypto.PubKey
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import xyz.block.moneyaddress.MoneyAddress
import xyz.block.moneyaddress.urn.DapUrn
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyAddressResolverTest {

    @Test
    fun testResolvingMoneyAddressFromPublicKey() {
        // TODO - goose wrote this mock for me but there must be an easier way
        val mockClient = object : OkHttpClient() {
            override fun newWebSocket(request: Request, listener: WebSocketListener): WebSocket {
                // Create a mock WebSocket that immediately sends our test event
                return object : WebSocket {
                    override fun request(): Request = request
                    override fun queueSize(): Long = 0
                    override fun send(text: String): Boolean {
                        // Ignore outgoing messages
                        return true
                    }
                    override fun send(bytes: ByteString): Boolean = true
                    override fun close(code: Int, reason: String?): Boolean = true
                    override fun cancel() {}
                }.also {
                    // Simulate the WebSocket connection and send our test event
                    listener.onOpen(it, Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(101)
                        .message("Switching Protocols")
                        .build())
                        
                    // Send a nostr message with the event
                    listener.onMessage(it, """["EVENT", "test-subscription", ${
                        """
                        {
                          "id": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                          "pubkey": "${VALID_PUBKEY.key.hex()}",
                          "created_at": 1234567890,
                          "kind": 33277,
                          "tags": [
                            ["currency", "btc"],
                            ["protocol", "lnaddr"],
                            ["address", "person@domain.com"]
                          ],
                          "content": "",
                          "sig": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2"
                        }
                        """.trimIndent()
                    }]""".trimIndent())
                }
            }
        }

        val moneyAddressResolver = MoneyAddressResolver {
            relaySet = RelaySet(
                setOf(RelayClient("wss://somerelay.xyz", mockClient))
            )
            timeoutMillis = 500
        }

        val moneyAddresses = moneyAddressResolver.resolveMoneyAddresses(VALID_PUBKEY)
        assertEquals(1, moneyAddresses.size)
        assertEquals(
            MoneyAddress(
                id = "ma-0",
                urn = VALID_BITCOIN_ADDRESS_URN,
                currency = VALID_BITCOIN_ADDRESS_URN_CURRENCY,
                protocol = VALID_BITCOIN_ADDRESS_URN_PROTOCOL,
                pss = VALID_BITCOIN_ADDRESS_URN_PSS,
            ),
            moneyAddresses[0]
        )
    }

    companion object {
        val VALID_PUBKEY = PubKey("eb02ec8d113f8ab1c569ff69cb7b6dded6a63e745c52979f36c8a8dbc41c3d48".decodeHex())

        const val VALID_BITCOIN_ADDRESS_URN_CURRENCY = "btc"
        const val VALID_BITCOIN_ADDRESS_URN_PROTOCOL = "lnaddr"
        const val VALID_BITCOIN_ADDRESS_URN_PSS = "person@domain.com"
        val VALID_BITCOIN_ADDRESS_URN = DapUrn.parse("urn:$VALID_BITCOIN_ADDRESS_URN_CURRENCY:$VALID_BITCOIN_ADDRESS_URN_PROTOCOL:$VALID_BITCOIN_ADDRESS_URN_PSS")
    }
}