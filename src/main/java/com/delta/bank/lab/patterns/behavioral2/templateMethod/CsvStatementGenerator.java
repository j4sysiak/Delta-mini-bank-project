package com.delta.bank.lab.patterns.behavioral2.templateMethod;

public final class CsvStatementGenerator extends StatementGenerator {

    @Override
    protected String header() {
        return "description,amount\n";
    }

    @Override
    protected String format(Transaction transaction) {
        return STR."\{transaction.description()},\{transaction.amount().toPlainString()}";
    }
}