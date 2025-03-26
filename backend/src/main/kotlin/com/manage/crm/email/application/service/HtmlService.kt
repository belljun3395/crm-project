package com.manage.crm.email.application.service

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import org.springframework.stereotype.Service

@Service
class HtmlService {

    fun prettyPrintHtml(input: String): String {
        val document: Document = Jsoup.parse(input, "", Parser.htmlParser())
        document.normalise()
        return document.outerHtml()
    }

    fun extractVariables(input: String): List<String> {
        val document: Document = Jsoup.parse(input, "", Parser.htmlParser())
        val variables = mutableSetOf<String>()

        document.allElements.forEach { element ->
            val value = element.toString()
            val regex = """\$\{([a-zA-Z0-9_.]+)}""".toRegex()
            regex.findAll(value).forEach { matchResult ->
                variables.add(matchResult.groupValues[1])
            }
        }

        return variables.toList()
    }
}
