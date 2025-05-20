package org.winlogon.echolite

import org.scalatest.funsuite.AnyFunSuite

class StringExtensionSpec extends AnyFunSuite {
  private val bridge = new MinecraftChatBridge(
    null.asInstanceOf[Configuration], 
    null.asInstanceOf[DiscordBotManager]
  )
  import bridge.getDiscordCompatible

  test("empty string remains empty") {
    assert("".getDiscordCompatible() === "")
  }

  test("string without any underscores remains unchanged") {
    val original = "HelloWorld123"
    assert(original.getDiscordCompatible() === original)
  }

  test("single underscore at the very beginning is escaped") {
    val original = "_hello"
    // the underscore at index 0 is not preceded by '_' and not followed by '_', so it becomes "\_"
    assert(original.getDiscordCompatible() === "\\_hello")
  }

  test("single underscore at the very end is escaped") {
    val original = "hello_"
    // the underscore at the end is not preceded by '_' and not followed by '_', so it becomes "\_"
    assert(original.getDiscordCompatible() === "hello\\_")
  }

  test("single underscore in the middle is escaped") {
    val original = "hello_world"
    // the '_' between "hello" and "world" is not adjacent to any other '_', so it becomes "\_"
    assert(original.getDiscordCompatible() === "hello\\_world")
  }

  test("double underscore remains unchanged") {
    val original = "hello__world"
    // neither of those two underscores match '(?<!_)_(?!_)', because each one is either preceded or followed by '_'
    assert(original.getDiscordCompatible() === original)
  }

  test("multiple single underscores get escaped individually") {
    val original = "a_b_c"
    // both underscores are standalone, so each should become "\_"
    assert(original.getDiscordCompatible() === "a\\_b\\_c")
  }

  test("triple underscore remains unchanged") {
    val original = "hello___world"
    // any character in that "___" fails either (?<!_) or (?!_), so none get replaced
    assert(original.getDiscordCompatible() === original)
  }

  test("underscore right next to a letter at both ends (e.g. '_a_') both get escaped") {
    val original = "_a_"
    // index 0: not preceded by '_' and next char is 'a' => escape it
    // index 2: preceded by 'a' and no following char => escape it
    assert(original.getDiscordCompatible() === "\\_a\\_")
  }

  test("mixed pattern: '__a_' leaves the first two together, but escapes the final one") {
    val original = "__a_"
    // positions:
    //   index 0: '_' but next char is '_' ⇒ (?!_) fails ⇒ not replaced
    //   index 1: '_' but previous char is '_' ⇒ (?<!_) fails ⇒ not replaced
    //   index 3: '_' is standalone ⇒ replaced
    val expected = "__a\\_"
    assert(original.getDiscordCompatible() === expected)
  }
}

