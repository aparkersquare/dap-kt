package xyz.block.dap.examples

import xyz.block.dap.Dap
import xyz.block.dap.nostr.DapResolver
import kotlin.test.assertEquals

fun main(args: Array<String>) {
  val dapString = if (args.isNotEmpty()) { args[0] } else { "@aparker/block.xyz" }
  try {
    val dap = Dap.parse(dapString)
    val (_, moneyAddresses) = DapResolver().resolveMoneyAddresses(dap)
    println("Resolved money addresses for $dap: $moneyAddresses")

    // filter by Currency and Protocol
    val btcLightningAddresses = moneyAddresses
      .filter { it.currency == "btc" }
      .filter { it.protocol == "lnaddr" }
    if (btcLightningAddresses.isNotEmpty()) {
      println("  found ${btcLightningAddresses.count()} BTC lightning addresses")
      btcLightningAddresses.forEach {
        println("    $it")
        assertEquals("btc", it.currency)
        assertEquals("addr", it.protocol)
      }
    }
  } catch (t: Throwable) {
    println("Failed to resolve money addresses for $dapString: $t")
  }
}
