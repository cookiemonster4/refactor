package com.elyonut.wow.adapter

import com.elyonut.wow.interfaces.ILogger
import timber.log.Timber

class TimberLogAdapter : ILogger {
    override fun initLogger() {
        Timber.plant(Timber.DebugTree())
    }

    override fun debug(message: String, vararg args: Object) {
        Timber.d(message, args)
    }

    override fun info(message: String, vararg args: Object) {
        Timber.i(message, args)
    }

    override fun error(message: String, vararg args: Object) {
        Timber.e(message, args)
    }

    override fun verbose(message: String, vararg args: Object) {
        Timber.v(message, args)
    }

    override fun warning(message: String, vararg args: Object) {
        Timber.w(message, args)
    }

}