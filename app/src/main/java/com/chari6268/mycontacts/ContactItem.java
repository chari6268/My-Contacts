package com.chari6268.mycontacts;

import java.util.ArrayList;
import java.util.List;

public class ContactItem {
    private String name;
    private List<String> phoneNumbers = new ArrayList<>();
    private List<String> emails = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    @Override
    public String toString() {
        return name;
    }
}