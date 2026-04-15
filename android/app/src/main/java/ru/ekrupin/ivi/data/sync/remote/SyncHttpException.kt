package ru.ekrupin.ivi.data.sync.remote

import java.io.IOException

class SyncHttpException(
    val code: Int,
    message: String,
) : IOException(message)
