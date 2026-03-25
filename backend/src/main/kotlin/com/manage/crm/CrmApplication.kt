package com.manage.crm

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class CrmApplication

fun main(args: Array<String>) {
    runApplication<CrmApplication>(*args)
}
