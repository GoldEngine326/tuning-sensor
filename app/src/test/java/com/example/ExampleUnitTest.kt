package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testMultiConnectorTextTableParsing() {
    val text = """
      Connector A (8 circuits)
      1 - Power Ground
      2 - Power Ground
      3 - +12V Switched Power

      Connector B (18 circuits)
      B1 - Injector 1
      B2 - Injector 2
      B7 - Ignition 1

      Connector C
      C1 - +5V VREF
      C2 - Sensor GND

      Connector D
      D1 - DBW Motor +
      D6 - CAN High

      Connector E
      E1 - LSU 4.9 IP
    """.trimIndent()

    val result = com.example.util.DatasheetExtractor.parseTextTableLocally(text, "uaefi_pinout.txt")
    assertNotNull(result)
    val connectors = result!!.pins.map { it.connectorName }.distinct()
    assertTrue(connectors.contains("Wtyczka A"))
    assertTrue(connectors.contains("Wtyczka B"))
    assertTrue(connectors.contains("Wtyczka C"))
    assertTrue(connectors.contains("Wtyczka D"))
    assertTrue(connectors.contains("Wtyczka E"))
    assertEquals(5, connectors.size)
    assertEquals(com.example.model.ComponentCategory.ECU, result.category)
  }

  @Test
  fun testHtmlRichContentFormatting() {
    val html = """
      <html>
        <head><title>UAEFI Pinout</title></head>
        <body>
          <h2>Connector A</h2>
          <table>
            <tr><th>Pin</th><th>Name</th></tr>
            <tr><td>1</td><td>Ground</td></tr>
          </table>
          <script>
            var pins = [{"pin": "B1", "name": "Inj 1"}];
          </script>
        </body>
      </html>
    """.trimIndent()

    val formatted = com.example.util.DatasheetExtractor.extractRichContentFromHtml(html)
    assertTrue(formatted.contains("### Connector A"))
    assertTrue(formatted.contains("SUROWE DANE PINOUTU"))
  }
}
