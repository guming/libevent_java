package org.jinn.libevent;


import java.nio.channels.SelectableChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by gumingcn on 2016/3/14.
 */
public class Event  implements Comparable<Event>, EventConfig {
    EventBase eventBase;
    SelectableChannel selectableChannel;
    int ev_events;//event status,such as read write accept
    int ev_ncalls;//callback counts
    AtomicInteger ev_pncalls;//Allows deletes in callback
    int ev_res;//result passed to event callback
    int ev_flags=EVLIST_INIT; //libevent status
    long ev_timeout;//
    public int compareTo(Event o) {
        if (this.ev_timeout > o.ev_timeout)
            return 1;
        else if (this.ev_timeout == o.ev_timeout)
            return 0;
        else
            return -1;
    }
    int ev_pri;  // smaller numbers are higher priority
    Object[] args;
    EventCallBackHandler eventCallBackHandler;

    public EventBase getEventBase() {
        return eventBase;
    }

    public void setEventBase(EventBase eventBase) {
        this.eventBase = eventBase;
    }

    public SelectableChannel getSelectableChannel() {
        return selectableChannel;
    }

    public void setSelectableChannel(SelectableChannel selectableChannel) {
        this.selectableChannel = selectableChannel;
    }

    public int getEv_events() {
        return ev_events;
    }

    public void setEv_events(int ev_events) {
        this.ev_events = ev_events;
    }

    public int getEv_ncalls() {
        return ev_ncalls;
    }

    public void setEv_ncalls(int ev_ncalls) {
        this.ev_ncalls = ev_ncalls;
    }

    public AtomicInteger getEv_pncalls() {
        return ev_pncalls;
    }

    public void setEv_pncalls(AtomicInteger ev_pncalls) {
        this.ev_pncalls = ev_pncalls;
    }

    public int getEv_res() {
        return ev_res;
    }

    public void setEv_res(int ev_res) {
        this.ev_res = ev_res;
    }

    public int getEv_flags() {
        return ev_flags;
    }

    public void setEv_flags(int ev_flags) {
        this.ev_flags = ev_flags;
    }

    public long getEv_timeout() {
        return ev_timeout;
    }

    public void setEv_timeout(long ev_timeout) {
        this.ev_timeout = ev_timeout;
    }

    public int getEv_pri() {
        return ev_pri;
    }

    public void setEv_pri(int ev_pri) {
        this.ev_pri = ev_pri;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public EventCallBackHandler getEventCallBackHandler() {
        return eventCallBackHandler;
    }

    public void setEventCallBackHandler(EventCallBackHandler eventCallBackHandler) {
        this.eventCallBackHandler = eventCallBackHandler;
    }
}
