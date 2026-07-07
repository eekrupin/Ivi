package ru.ekrupin.ivi.core.util

import java.io.IOException
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetPhotoStorageTest {
    @Test
    fun writePetPhotoAtomically_successWritesFinalFile() {
        val directory = createTempDirectory().toFile()
        val target = directory.resolve("remote_revision.jpg")

        writePetPhotoAtomically(target) { outputStream ->
            outputStream.write("complete".toByteArray())
        }

        assertTrue(target.exists())
        assertEquals("complete", target.readText())
        assertFalse(directory.listFiles().orEmpty().any { it.name.endsWith(".tmp") })
    }

    @Test
    fun writePetPhotoAtomically_failureDoesNotExposeFinalFile() {
        val directory = createTempDirectory().toFile()
        val target = directory.resolve("remote_revision.jpg")

        runCatching {
            writePetPhotoAtomically(target) { outputStream ->
                outputStream.write("partial".toByteArray())
                throw IOException("network interrupted")
            }
        }

        assertFalse(target.exists())
        assertFalse(directory.listFiles().orEmpty().any { it.name.endsWith(".tmp") })
    }

    @Test
    fun writePetPhotoAtomically_failureKeepsPreviousFinalFile() {
        val directory = createTempDirectory().toFile()
        val target = directory.resolve("remote_revision.jpg")
        target.writeText("previous")

        runCatching {
            writePetPhotoAtomically(target) { outputStream ->
                outputStream.write("partial".toByteArray())
                throw IOException("network interrupted")
            }
        }

        assertTrue(target.exists())
        assertEquals("previous", target.readText())
        assertFalse(directory.listFiles().orEmpty().any { it.name.endsWith(".tmp") })
    }
}
