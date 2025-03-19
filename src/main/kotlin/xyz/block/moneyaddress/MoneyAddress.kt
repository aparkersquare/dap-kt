package xyz.block.moneyaddress

import xyz.block.moneyaddress.urn.DapUrn

/**
 * A generic representation of a MoneyAddress, without specific details of the currency or protocol.
 *
 * For a more specific representation, see [TypedMoneyAddress].
 */
data class MoneyAddress(
  val id: String,
  val urn: DapUrn,
  val currency: String,
  val protocol: String,
  val pss: String
) {

  override fun toString(): String =
    "MoneyAddress($currency, $protocol, $pss, $id)"

  companion object {
    const val KIND: String = "MoneyAddress"
  }
}

/**
 * Converts a DAP URN to a MoneyAddress.
 */
fun DapUrn.toMoneyAddress(id: String): MoneyAddress =
  MoneyAddress(
    id = id,
    urn = this,
    currency = currency,
    protocol = protocol,
    pss = pss
  )

object InvalidMoneyAddressException : Throwable("Invalid MoneyAddress")
