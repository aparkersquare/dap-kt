package xyz.block.dap.nostr

import xyz.block.dap.Dap
import xyz.block.moneyaddress.MoneyAddress

/**
 * Resolves a Decentralized Agnostic Paytag (DAP).
 * See the [DAP spec](https://github.com/aparkersquare/dap#resolution) for the resolution process.
 *
 * NOTE: This is a fork that implements DAPs on [nostr](https://nostr.org).
 *
 * This wires together the PubKeyResolver, and MoneyAddressResolver.
 */
class DapResolver {
  private val publicKeyResolver: PublicKeyResolver = PublicKeyResolver()
  private val moneyAddressResolver: MoneyAddressResolver = MoneyAddressResolver()

  /**
   * Resolves the money addresses for a DAP.
   *
   * @param dap the DAP to resolve
   * @return the list of money addresses for the DAP
   */
  fun resolveMoneyAddresses(dap: Dap): List<MoneyAddress> {
    val pubKey = publicKeyResolver.resolvePublicKey(dap)
    // TODO - validate the Kind 0 for the PubKey uses the same NIP-05
    val moneyAddresses = moneyAddressResolver.resolveMoneyAddresses(pubKey)
    return moneyAddresses
  }
}
