package xyz.block.dap.nostr

import app.cash.nostrino.crypto.PubKey
import xyz.block.moneyaddress.MoneyAddress

// This implements part of the DAP resolution process.
// See [DapResolver] for the full resolution process.
// See the [DAP spec](https://github.com/aparkersquare/dap#resolution) for the resolution process.
// NOTE: This is a fork that implements DAPs on [nostr](https://nostr.org).
class MoneyAddressResolver {

  fun resolveMoneyAddresses(pubKey: PubKey): List<MoneyAddress> {
    val moneyAddresses = emptyList<MoneyAddress>() // TODO
    return moneyAddresses
  }
}

class MoneyAddressResolutionException : Throwable {
  constructor(message: String, cause: Throwable?) : super(message, cause)
  constructor(message: String) : super(message)
}
