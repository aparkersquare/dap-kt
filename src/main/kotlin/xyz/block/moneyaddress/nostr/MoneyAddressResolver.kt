package xyz.block.moneyaddress.nostr

import app.cash.nostrino.crypto.PubKey
import io.github.oshai.kotlinlogging.KotlinLogging
import xyz.block.dap.nostr.DapResolver
import xyz.block.moneyaddress.MoneyAddress

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
 * - TODO
 */
class MoneyAddressResolverConfiguration internal constructor(
)

/**
 * This is part of the DAP resolution process.
 * See [DapResolver] for the full resolution process.
 * See the [DAP spec](https://github.com/aparkersquare/dap#resolution) for the resolution process.
 *
 * NOTE: This is a fork that implements DAPs on [nostr](https://nostr.org).
 */
sealed class MoneyAddressResolver(
  configuration: MoneyAddressResolverConfiguration
) {
  /**
   * This resolves the list of Money Addresses for the nostr PukKey.
   * Money Addresses are advertised via Kind 33277.
   * See [NIP-100](https://github.com/aparkersquare/nips/blob/aparker-2025-03-17-add-money-address-kind/100.md) for the specification.
   *
   * Any errors in the process will throw a [MoneyAddressResolutionException].
   */
  fun resolveMoneyAddresses(pubKey: PubKey): List<MoneyAddress> {
    val moneyAddresses = emptyList<MoneyAddress>() // TODO
    return moneyAddresses
  }

  companion object {
    /**
     * A singleton PublicKeyResolver with the default configuration
     */
    internal val default: MoneyAddressResolver by lazy {
      MoneyAddressResolverImpl(MoneyAddressResolverConfiguration())
    }
  }

  private val logger = KotlinLogging.logger {}
}

class MoneyAddressResolutionException : Throwable {
  constructor(message: String, cause: Throwable?) : super(message, cause)
  constructor(message: String) : super(message)
}
