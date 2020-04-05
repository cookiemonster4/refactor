package com.elyonut.wow.interfaces

interface ILogger {
    fun initLogger()
    fun debug(message: String, vararg args: Object)
    fun info(message: String, vararg args: Object)
    fun error(message: String, vararg args: Object)
    fun verbose(message: String, vararg args: Object)
    fun warning(message: String, vararg args: Object)
}