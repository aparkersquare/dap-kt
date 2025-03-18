package xyz.block.dap

import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class DapTest {

  @Test
  fun testParseDap() {
    val dap = Dap.parse("@handle/domain.com")
    assertEquals("handle", dap.handle)
    assertEquals("domain.com", dap.domain)
  }

  @Test
  fun testDapToString() {
    val dap = Dap("handle", "domain.com")
    assertEquals("@handle/domain.com", dap.toString())
  }

  @Test
  fun testDapToNip05() {
    val dap = Dap("handle", "domain.com")
    assertEquals("handle@domain.com", dap.toNip05())
  }

  @Test
  fun testParseValidDaps() {
    val validDaps = listOf(
      "@abc/domain.com",
      "@ABC/domain.com",
      "@AbC/domain.com",
      "@123/domain.com",
      "@a_b/domain.com", // '_' is allowed
      "@a.b/domain.com", // '.' is allowed
      "@a/domain.com", // short names are allowed
      "@A/domain.com", // short names are allowed
      "@1234567890123456789012345678901234567890/domain.com", // long names are allowed
    )
    validDaps.forEach { dap ->
      try {
        Dap.parse(dap)
      } catch (e: InvalidDapException) {
        throw AssertionError("expect [$dap] to be valid")
      }
    }
  }

  @Test
  fun testParseInvalidDaps() {
    val invalidDaps = listOf(
      "",
      "a",
      "@handle",
      "@handle/",
      "@handle@/domain.com",
      "@handle@handle/domain.com",
      "@handle//domain.com",
      "@handle/handle/domain.com",
      "@handle/@domain.com",
      "@handle/domain.com@",
      "@handle/domain.com/",
      "@handle/domain.com/extra-stuff",
      "@a-b/domain.com", // '-' not allowed
    )
    for (dap in invalidDaps) {
      val exception = assertThrows<InvalidDapException>("expect [$dap] to be invalid") {
        Dap.parse(dap)
      }
      assertEquals("Invalid DAP", exception.message)
    }
  }
}
