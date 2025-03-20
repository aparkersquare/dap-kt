package xyz.block.dap

import java.util.regex.Pattern

/**
 * A Decentralized Agnostic Paytag (DAP).
 *
 * A DAP consists of
 * @property handle - a `handle` which is a unique identifier for the user within the domain
 * @property domain - the domain of the DAP registry, effectively scopes the handles
 */
data class Dap(
  val handle: String,
  val domain: String
) {
  /**
   * The canonical string format of a DAP is `@handle/domain`.
   * An example would be `@moegrammer/didpay.me`.
   * In this case, `moegrammer` is the handle and `didpay.me` is the domain.
   */
  override fun toString(): String {
    return "$PREFIX$handle$SEPARATOR$domain"
  }

  // DELETE
  /**
   * Transform the DAP to the web DID format, used for DAP resolution.
   */
  fun toWebDid(): String {
    return "did:web:${domain}"
  }

  /**
   * Transform the DAP to the NIP-05 equivalent.
   */
  fun toNip05(): String {
    return "$handle@$domain"
  }

  companion object {
    const val PREFIX = "@"
    const val SEPARATOR = "/"
    private const val CHARS = "[a-zA-Z0-9_.]+"

    private const val DAP_REGEX =
      """^$PREFIX($CHARS)$SEPARATOR($CHARS+)$"""
    private val DAP_PATTERN = Pattern.compile(DAP_REGEX)

    /**
     * Parse a string into a DAP.
     * The string must match the canonical format of a DAP (i.e. `@handle/domain`).
     *
     * @throws InvalidDapException if the DAP is not a valid DAP.
     */
    fun parse(dap: String): Dap {
      val matcher = DAP_PATTERN.matcher(dap)
      matcher.find()
      if (!matcher.matches()) {
        throw InvalidDapException
      }
      val handle = matcher.group(1)
      val domain = matcher.group(2)

      return Dap(handle, domain)
    }
  }
}

object InvalidDapException : Throwable(message = "Invalid DAP")
