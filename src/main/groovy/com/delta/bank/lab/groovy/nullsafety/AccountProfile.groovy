package com.delta.bank.lab.groovy.nullsafety

class AccountProfile {
    final String owner
    final AccountContact contact
    final List<String> tags

    AccountProfile(String owner, AccountContact contact, List<String> tags) {
        this.owner = owner
        this.contact = contact
        this.tags = tags
    }
}