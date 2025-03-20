package xyz.block.dap.examples

import xyz.block.dap.Dap
import xyz.block.dap.nostr.DapResolver

fun main(args: Array<String>) {
  args.toList().ifEmpty { listOf("@aparker/block.xyz", "@jack/block.xyz") }.forEach { dapString ->
    try {
      val dap = Dap.parse(dapString)
      val (pubkey, moneyAddresses) = DapResolver().resolveMoneyAddresses(dap)
      println("Resolved [${moneyAddresses.size}] money addresses and npub [${pubkey.npub}] for DAP [$dap]")
      moneyAddresses.forEach { moneyAddress ->
        println("  $moneyAddress")
      }
    } catch (t: Throwable) {
      println("Failed to resolve npub and money addresses for $dapString: $t")
    }
  }
}
