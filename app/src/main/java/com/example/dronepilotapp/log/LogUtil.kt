package com.example.dronepilotapp.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LogUtil {
    companion object {
        fun getLogger(forClass: Class<*>): Logger {
            return LoggerFactory.getLogger(forClass)
        }
    }
}