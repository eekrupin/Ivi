package ru.ekrupin.ivi.data.sync

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex

@Singleton
class SyncExecutionGate @Inject constructor() {
    private val mutex = Mutex()

    suspend fun <T> runOrSkip(block: suspend () -> T): T? {
        if (!mutex.tryLock()) return null
        return try {
            block()
        } finally {
            mutex.unlock()
        }
    }
}
