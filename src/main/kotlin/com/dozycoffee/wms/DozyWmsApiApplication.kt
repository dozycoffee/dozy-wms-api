package com.dozycoffee.wms

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DozyWmsApiApplication

fun main(args: Array<String>) {
    runApplication<DozyWmsApiApplication>(*args)
}
