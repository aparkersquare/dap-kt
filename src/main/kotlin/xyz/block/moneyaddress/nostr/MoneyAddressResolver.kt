package xyz.block.moneyaddress.nostr

import app.cash.nostrino.client.RelayClient
import app.cash.nostrino.client.RelaySet
import app.cash.nostrino.crypto.PubKey
import app.cash.nostrino.model.Event
import app.cash.nostrino.model.Filter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import xyz.block.dap.nostr.DapResolver
import xyz.block.moneyaddress.MoneyAddress
import xyz.block.moneyaddress.urn.DapUrn
import java.io.File
import java.net.InetAddress
import kotlin.time.Duration.Companion.milliseconds

// This uses a modified version of the scheme used in web5-kt to provide customization
// of the configuration (see `DidWeb`).
// The difference is that we provide two overloaded functions for constructing the resolver,
// which look like two overloaded constructors to the caller.
// This is instead of a single function and the use of the `Default` companion object.
// We also don't need to expose both a `MoneyAddressResolver` and `MoneyAddressResolverApi`.
// This is probably still too complicated.

/**
 * A PublicKeyResolver with the default configuration.
 */
fun MoneyAddressResolver(): MoneyAddressResolver = MoneyAddressResolver.default

/**
 * Constructs a PublicKeyResolver with the block configuration applied.
 */
fun MoneyAddressResolver(
  blockConfiguration: MoneyAddressResolverConfiguration.() -> Unit
): MoneyAddressResolver {
  val config = MoneyAddressResolverConfiguration().apply(blockConfiguration)
  return MoneyAddressResolverImpl(config)
}

// This allows the PublicKeyResolver to be sealed
private class MoneyAddressResolverImpl(
  configuration: MoneyAddressResolverConfiguration
) : MoneyAddressResolver(configuration)

/**
 * Configuration options for the [MoneyAddressResolver].
 *
 * - [relaySet]
 */
class MoneyAddressResolverConfiguration internal constructor(
  var relaySet: RelaySet? = null,
  var timeoutMillis: Long = 1_000
)

/**
 * This is part of the DAP resolution process.
 * See [DapResolver] for the full resolution process.
 * See the [DAP spec](https://github.com/aparkersquare/dap#resolution) for the resolution process.
 *
 * NOTE: This is a fork that implements DAPs on [nostr](https://nostr.org).
 */
sealed class MoneyAddressResolver(
  private val configuration: MoneyAddressResolverConfiguration
) {
  /**
   * This resolves the list of Money Addresses for the nostr PukKey.
   * Money Addresses are advertised via Kind 33277.
   * See [NIP-100](https://github.com/aparkersquare/nips/blob/aparker-2025-03-17-add-money-address-kind/100.md) for the specification.
   *
   * Any errors in the process will throw a [MoneyAddressResolutionException].
   */
  fun resolveMoneyAddresses(pubKey: PubKey): List<MoneyAddress> {
    try {
      logger.info { "Connecting to relay" }
      relaySet.start()

      logger.info { "Subscribing to money address kinds" }
      relaySet.subscribe(Filter(kinds = setOf(33277)).plusAuthors(pubKey))

      // TODO - need a better way of doing this
      val events = mutableListOf<Event>()
      runBlocking {
        try {
          withTimeout(configuration.timeoutMillis.milliseconds) {
            relaySet.allEvents.collect { event ->
              logger.info { "received event $event" }
              events.add(event)
            }
          }
        } catch (t: Throwable) {
          logger.error(t) { "error in withTimeout" }
        } finally {
          try {
            relaySet.stop()
          } catch (t2: Throwable) {
            logger.error(t2) { "error in stop" }
          }
        }
      }
      logger.info { "received events $events" }
      val moneyAddresses = events.mapIndexedNotNull { index, event ->
        logger.info { "received event $event" }
        val currency = event.getTagValue("currency")
        val protocol = event.getTagValue("protocol")
        val address = event.getTagValue("address")
        if (currency == null || protocol == null || address == null) {
          null
        } else {
          MoneyAddress(
            id = "ma-$index",
            DapUrn(currency, protocol, address),
            currency = currency,
            protocol = protocol,
            pss = address
          )
        }
      }
      logger.info { "received money addresses $moneyAddresses" }
      return moneyAddresses
    } catch (t: Throwable) {
      throw MoneyAddressResolutionException(
        "Error resolving money addresses [pubKey=${pubKey.key.hex()}][error=${t.message}]",
        t
      )
    } finally {
      relaySet.stop()
    }
  }

  private fun Event.getTagValue(tagName: String): String? =
    tags.firstOrNull { it.firstOrNull() == tagName && it.size >= 2 }?.get(1)

  companion object {
    /**
     * A singleton PublicKeyResolver with the default configuration
     */
    internal val default: MoneyAddressResolver by lazy {
      MoneyAddressResolverImpl(MoneyAddressResolverConfiguration())
    }
  }

  private val appCache = Cache(File("cacheDir", "okhttpcache"), 10 * 1024 * 1024)
  private val bootstrapClient = OkHttpClient.Builder().cache(appCache).build()

  private val dns = DnsOverHttps.Builder()
    .client(bootstrapClient)
    .url("https://dns.quad9.net/dns-query".toHttpUrl())
    .bootstrapDnsHosts(
      InetAddress.getByName("9.9.9.9"),
      InetAddress.getByName("149.112.112.112")
    )
    .build()

  private val client = bootstrapClient.newBuilder().dns(dns).build()

  private val relaySet: RelaySet = configuration.relaySet ?: RelaySet(
    setOf(
      RelayClient(
        "wss://relay.dapsnostrrelay.xyz",
        client
      )
    )
  )

  private val logger = KotlinLogging.logger {}
}

class MoneyAddressResolutionException : Throwable {
  constructor(message: String, cause: Throwable?) : super(message, cause)
  constructor(message: String) : super(message)
}
