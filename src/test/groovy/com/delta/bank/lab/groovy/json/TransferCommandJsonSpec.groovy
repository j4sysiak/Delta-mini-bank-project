package com.delta.bank.lab.groovy.json

import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.lang.Unroll

class TransferCommandJsonSpec extends Specification {

    private final TransferCommandParser parser = new TransferCommandParser()
    private final TransferCommandJson writer = new TransferCommandJson()

    def "parses a valid transfer command"() {
        when:
        def command = parser.parse(validJson())

        then:
        command.requestId == "req-1001"
        command.fromAccount == "PLN-1001"
        command.toAccount == "PLN-2002"
        command.amount.compareTo(new BigDecimal("125.50")) == 0
    }

    def "writes JSON that can be parsed back to the same values"() {
        given:
        def command = parser.parse(validJson())

        when:
        def output = writer.write(command)
        def parsedAgain = new JsonSlurper().parseText(output)

        then:
        parsedAgain.requestId == "req-1001"
        parsedAgain.fromAccount == "PLN-1001"
        parsedAgain.toAccount == "PLN-2002"
        new BigDecimal(parsedAgain.amount.toString())
                .compareTo(new BigDecimal("125.50")) == 0
    }

    @Unroll
    def "rejects JSON with an invalid root or required field: #caseName"() {
        when:
        parser.parse(json)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == expectedMessage

        where:
        caseName             | json                                                                    | expectedMessage
        "array root"         | "[]"                                                                    | "JSON root must be an object"
        "missing request id" | '{"fromAccount":"PLN-1001","toAccount":"PLN-2002","amount":1}'          | "requestId is required"
        "null request id"    | '{"requestId":null,"fromAccount":"PLN-1001","toAccount":"PLN-2002","amount":1}' | "requestId must be a String"
        "blank request id"   | '{"requestId":" ","fromAccount":"PLN-1001","toAccount":"PLN-2002","amount":1}' | "requestId must not be blank"
        "string amount"      | '{"requestId":"r1","fromAccount":"PLN-1001","toAccount":"PLN-2002","amount":"1.00"}' | "amount must be a JSON number"
        "unknown field"      | '{"requestId":"r1","fromAccount":"PLN-1001","toAccount":"PLN-2002","amount":1,"memo":"x"}' | "Unknown field(s): memo"
    }

    def "rejects a non-positive amount"() {
        when:
        parser.parse('{"requestId":"r1","fromAccount":"PLN-1001","toAccount":"PLN-2002","amount":0}')

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "amount must be greater than zero"
    }

    def "rejects identical source and destination accounts"() {
        when:
        parser.parse('{"requestId":"r1","fromAccount":"PLN-1001","toAccount":"PLN-1001","amount":1}')

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "fromAccount and toAccount must be different"
    }

    def "rejects null JSON text"() {
        when:
        parser.parse(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "json must not be null"
    }

    def "rejects null command when writing JSON"() {
        when:
        writer.write(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "command must not be null"
    }

    private static String validJson() {
        '''
        {
          "requestId": "req-1001",
          "fromAccount": "PLN-1001",
          "toAccount": "PLN-2002",
          "amount": 125.50
        }
        '''
    }
}