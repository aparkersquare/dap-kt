package xyz.block.dap.nostr

import app.cash.nostrino.crypto.PubKey
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import okio.ByteString.Companion.decodeHex
import web5.sdk.common.Json
import xyz.block.dap.Dap
import java.io.File
import java.net.InetAddress
import java.net.URL

// This uses a modified version of the scheme used in web5-kt to provide customization
// of the configuration (see `DidWeb`).
// The difference is that we provide two overloaded functions for constructing the resolver,
// which look like two overloaded constructors to the caller.
// This is instead of a single function and the use of the `Default` companion object.
// We also don't need to expose both a `PublicKeyResolver` and `PublicKeyResolverApi`.
// This is probably still too complicated.

/**
 * A PublicKeyResolver with the default configuration.
 */
fun PublicKeyResolver(): PublicKeyResolver = PublicKeyResolver.default

/**
 * Constructs a PublicKeyResolver with the block configuration applied.
 */
fun PublicKeyResolver(
  blockConfiguration: PublicKeyResolverConfiguration.() -> Unit
): PublicKeyResolver {
  val config = PublicKeyResolverConfiguration().apply(blockConfiguration)
  return PublicKeyResolverImpl(config)
}

// This allows the PublicKeyResolver to be sealed
private class PublicKeyResolverImpl(
  configuration: PublicKeyResolverConfiguration
) : PublicKeyResolver(configuration)

/**
 * This is part of the DAP resolution process.
 * See [DapResolver] for the full resolution process.
 * See the [DAP spec](https://github.com/TBD54566975/dap#resolution)
 *
 * NOTE: This is a fork that implements DAPs on [nostr](https://nostr.org).
 */
sealed class PublicKeyResolver(
  configuration: PublicKeyResolverConfiguration
) {
  /**
   * This resolves a DAP to the nostr PukKey.
   * The process is
   * - the DAP is mapped to the equivalent NIP-05
   * - request the JSON according to [NIP-05 ](https://github.com/nostr-protocol/nips/blob/master/05.md)
   * - extract the public key for the matching name from the response
   *
   * Any errors in the process will throw a [PublicKeyResolutionException].
   */
  fun resolvePublicKey(dap: Dap): PubKey {
    val fullUrl = URL("https://${dap.domain}/.well-known/nostr.json?name=${dap.handle}")
    logger.info { "fetching nostr pubkey from $fullUrl" }

    val resp: HttpResponse = try {
      runBlocking {
        client.get(fullUrl) {
          contentType(ContentType.Application.Json)
        }
      }
    } catch (e: Throwable) {
      throw PublicKeyResolutionException(
        "Error fetching the NIP-05 [dap=$dap][url=$fullUrl][error=${e.message}]",
        e
      )
    }

    val body = runBlocking { resp.bodyAsText() }

    if (!resp.status.isSuccess()) {
      throw PublicKeyResolutionException("Error reading NIP-05 response [dap=$dap][url=$fullUrl][status=${resp.status}]")
    }

    val nip05Response = try {
      mapper.readValue(body, NostrNip05Response::class.java)
    } catch (e: Throwable) {
      throw PublicKeyResolutionException(
        "Failed to parse NIP-05 response [dap=$dap][url=$fullUrl][error=${e.message}]",
        e
      )
    }

    val maybeNip05 =
      nip05Response.names.filter { it.key == dap.handle }.map { it.value }.firstOrNull()
    if (maybeNip05 == null) {
      throw PublicKeyResolutionException("NIP-05 response does not have matching name [dap=$dap][url=$fullUrl]")
    } else {
      val pubkey = PubKey(maybeNip05.decodeHex())
      logger.info { "fetched nostr pubkey [${pubkey.npub}] from $fullUrl" }
      return pubkey
    }
  }

  companion object {
    /**
     * A singleton PublicKeyResolver with the default configuration
     */
    internal val default: PublicKeyResolver by lazy {
      PublicKeyResolverImpl(PublicKeyResolverConfiguration())
    }
  }

  private val engine: HttpClientEngine = configuration.engine ?: OkHttp.create {
    val appCache = Cache(File("cacheDir", "okhttpcache"), 10 * 1024 * 1024)
    val bootstrapClient = OkHttpClient.Builder().cache(appCache).build()

    val dns = DnsOverHttps.Builder()
      .client(bootstrapClient)
      .url("https://dns.quad9.net/dns-query".toHttpUrl())
      .bootstrapDnsHosts(
        InetAddress.getByName("9.9.9.9"),
        InetAddress.getByName("149.112.112.112")
      )
      .build()

    val client = bootstrapClient.newBuilder().dns(dns).build()
    preconfigured = client
  }
  private val client = HttpClient(engine) {
    install(ContentNegotiation) {
      jackson { mapper }
    }
  }

  private val mapper = Json.jsonMapper

  private val logger = KotlinLogging.logger {}
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class NostrNip05Response(
  val names: Map<String, String>,
)

/**
 * Configuration options for the [PublicKeyResolver].
 *
 * - [engine] is used to override the ktor HTTP engine.
 * The default HTTP engine uses [OkHttp] with [DnsOverHttps] and a 10MB cache.
 */
class PublicKeyResolverConfiguration internal constructor(
  var engine: HttpClientEngine? = null
)

class PublicKeyResolutionException : Throwable {
  constructor(message: String, cause: Throwable?) : super(message, cause)
  constructor(message: String) : super(message)
}
