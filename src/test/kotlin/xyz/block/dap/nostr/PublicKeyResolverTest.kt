package xyz.block.dap.nostr

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import org.junit.jupiter.api.assertThrows
import xyz.block.dap.Dap
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PublicKeyResolverTest {

  private val publicKeyResolver = PublicKeyResolver { engine = mockEngine() }

  @Test
  fun testResolvingPublicKeyFromNip05() {
    val publicKey = publicKeyResolver.resolvePublicKey(VALID_DAP)
    assertEquals(VALID_PUBLIC_KEY, publicKey.key.utf8())
  }

  @Test
  fun testErrorsFetchingFromRegistry() {
    val errorDaps = listOf(
      UNKNOWN_DAP to "Error fetching the NIP-05",
      EMPTY_RESPONSE_DAP to "Error fetching the NIP-05",
      INVALID_RESPONSE_DAP to "Error fetching the NIP-05",
    )
    errorDaps.forEach { (dap, expectedMessage) ->
      val exception = assertThrows<PublicKeyResolutionException> {
        publicKeyResolver.resolvePublicKey(dap)
      }
      assertNotNull(exception.message)
      assertContains(exception.message!!, expectedMessage)
    }
  }

  private fun mockEngine() = MockEngine { request ->
    when (request.url.toString()) {
      "$VALID_URL/.well-known/nostr.json?name=${VALID_DAP.handle}" -> {
        respond(
          content = ByteReadChannel("""{"names": {"${VALID_DAP.handle}":"$VALID_PUBLIC_KEY"}}"""),
          status = HttpStatusCode.OK,
          headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
      }

      "$VALID_URL/.well-known/nostr.json?name=/${UNKNOWN_DAP.handle}" -> {
        respond(
          content = ByteReadChannel("""{"names": {}}"""),
          status = HttpStatusCode.OK,
          headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
      }

      "$VALID_URL/.well-known/nostr.json?name=/${EMPTY_RESPONSE_DAP.handle}" -> {
        respond(
          content = ByteReadChannel("""{}"""),
          status = HttpStatusCode.OK,
          headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
      }

      "$VALID_URL/.well-known/nostr.json?name=/${INVALID_RESPONSE_DAP.handle}" -> {
        respond(
          content = ByteReadChannel("""invalid-response"""),
          status = HttpStatusCode.OK,
          headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
      }

      else -> error("Unhandled ${request.url}")
    }
  }

  companion object {
    val VALID_URL = URL("https://didpay.me")
    val VALID_DAP = Dap("moegrammer", "didpay.me")
    val VALID_PUBLIC_KEY = "eb02ec8d113f8ab1c569ff69cb7b6dded6a63e745c52979f36c8a8dbc41c3d48"

    val UNKNOWN_DAP = Dap("i-don't-know-you", "didpay.me")
    val EMPTY_RESPONSE_DAP = Dap("empty-response", "didpay.me")
    val INVALID_RESPONSE_DAP = Dap("invalid-response", "didpay.me")
  }
}
