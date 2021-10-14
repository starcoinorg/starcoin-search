package org.starcoin.scan.service;

import org.starcoin.bean.Event;
import org.starcoin.bean.Transaction;

import java.util.List;

public class TransactionWithEvent extends Transaction {
    List<Event> events;

    @Override
    public List<Event> getEvents() {
        return events;
    }

    @Override
    public void setEvents(List<Event> events) {
        this.events = events;
    }
}
