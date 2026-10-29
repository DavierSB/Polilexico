package app.lexico.games.export

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Test

class GifEncoderTest {
  private val width = 90
  private val height = 70

  private fun frame(seed: Int): IntArray = IntArray(width * height) { i ->
    val x = i % width
    val y = i / width
    if ((x + seed) % 7 < 3 && y > seed) 0xFFD9522A.toInt() else (0xFF000000.toInt() or ((x * 3 + y) % 25 * 8 shl 8))
  }

  private fun encoded(frames: List<IntArray>): ByteArray =
    ByteArrayOutputStream().also { GifEncoder(width, height).write(frames.map { GifFrame(it, 10) }, it) }.toByteArray()

  @Test fun firstFrameDecodesToSamePixels() {
    val pixels = frame(0)
    val image = ImageIO.read(ByteArrayInputStream(encoded(listOf(pixels))))
    assertEquals(width, image.width)
    for (i in pixels.indices) assertEquals("pixel $i", pixels[i] and 0xFFFFFF, image.getRGB(i % width, i / width) and 0xFFFFFF)
  }

  @Test fun everyFrameIsReadable() {
    val reader = ImageIO.getImageReadersByFormatName("gif").next()
    reader.input = ImageIO.createImageInputStream(ByteArrayInputStream(encoded((0 until 5).map(::frame))))
    assertEquals(5, reader.getNumImages(true))
    (0 until 5).forEach { reader.read(it) }
  }

  @Test fun longFramesResetTheDictionary() {
    val noise = java.util.Random(1)
    val pixels = IntArray(width * height) { 0xFF000000.toInt() or (noise.nextInt(16) * 0x111111) }
    val image = ImageIO.read(ByteArrayInputStream(encoded(listOf(pixels))))
    for (i in pixels.indices) assertEquals("pixel $i", pixels[i] and 0xFFFFFF, image.getRGB(i % width, i / width) and 0xFFFFFF)
  }
}
