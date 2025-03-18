package xyz.block.dap.nostr

import app.cash.nostrino.crypto.PubKey
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * This resolves a NIP-05 to the PukKey, using nostrino.
 * This is part of the DAP resolution process.
 * See the [DAP spec](https://github.com/TBD54566975/dap#resolution)
 *
 * NOTE: This is a fork that implements DAPs on [nostr](https://nostr.org).
 *
 * Any errors in the process will throw a [PubKeyResolutionException].
 */
class PublicKeyResolver {

  fun resolvePublicKey(nip05: String): PubKey {
    TODO()
  }

  private val logger = KotlinLogging.logger {}

  companion object {
  }
}

class PubKeyResolutionException : Throwable {
  constructor(message: String, cause: Throwable?) : super(message, cause)
  constructor(message: String) : super(message)
}
