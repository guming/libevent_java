package org.jinn.libevent;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by gumingcn on 16/3/16.
 */
public class LibEventHandler implements LibEvent {

    private EventBase eventBase;

    private EventOp eventOp;

    private volatile boolean done;

    @Override
    public void init() throws IOException {
        eventOp = new EventOp(this);
        eventBase = new EventBase();
        eventOp.init();
        eventBase.eventOp = eventOp;
    }

    @Override
    public void event_add(Event event, long timeout) throws IOException {
        EventBase eventBase = event.eventBase;
        EventOp eventOp = eventBase.eventOp;
        if(isValidEvent(event)&&!isActiveEvent(event)){
            eventOp.add(event);
            this.eventQueueInsert(event,EventConfig.EVLIST_INSERTED);
        }
        if (timeout > 0) {
            if ((event.ev_flags & EventConfig.EVLIST_TIMEOUT) > 0) {
                eventQueueRemove(event,EventConfig.EVLIST_TIMEOUT);
            }
            if ((event.ev_flags & EventConfig.EVLIST_ACTIVE) > 0 && (event.ev_res&EventConfig.EV_TIMEOUT)>0) {
                if (event.ev_pncalls != null && event.ev_ncalls > 0) {
                    event.ev_pncalls.set(0);
                }
                eventQueueRemove(event,EventConfig.EVLIST_ACTIVE);
            }
            long currentTime = getTime(eventBase);
            event.ev_timeout = currentTime + timeout;
            eventQueueInsert(event,EventConfig.EVLIST_TIMEOUT);
        }
    }

    @Override
    public void event_del(Event event) {
        EventBase eventBase = event.eventBase;
        EventOp eventOp = eventBase.eventOp;
        if ((event.ev_ncalls > 0 && event.ev_pncalls != null)) {
            event.ev_pncalls.set(0);
        }
        if ((event.ev_flags & EventConfig.EVLIST_TIMEOUT) > 0) {
            this.eventQueueRemove(event,EventConfig.EVLIST_TIMEOUT);
        }
        if ((event.ev_flags & EventConfig.EVLIST_ACTIVE) > 0) {
            this.eventQueueRemove(event,EventConfig.EVLIST_ACTIVE);
        }
        if ((event.ev_flags & EventConfig.EVLIST_INSERTED) > 0) {
            this.eventQueueRemove(event,EventConfig.EVLIST_INSERTED);
            eventOp.delete(event);
        }
    }

    @Override
    public void event_loop() {
        this.eventBase.timecache=0;
        while (!done) {
            long timeout = getTimeoutNext();
            try {
                this.eventOp.dispatch(eventBase, timeout);
                this.eventBase.timecache = getTime(eventBase);
                LinkedList<Event> activeList = null;
                for (int i=eventBase.active_queues.length-1;i>=0;i--) {
                    if (eventBase.active_queues[i].peek() != null) {
                        activeList = eventBase.active_queues[i];
                        break;
                    }
                }
                if (activeList != null) {
                    Event event = null;
                    while ((event=activeList.peek()) != null) {
                        if ((event.ev_events & EventConfig.EV_PERSIST) > 0) {
                            eventQueueRemove(event,EventConfig.EVLIST_ACTIVE);
                        }else{
                            event_del(event);
                        }
                        AtomicInteger ncalls = new AtomicInteger(event.ev_ncalls);
                        event.ev_pncalls = ncalls;
                        while (ncalls.get() > 0) {
                            event.ev_ncalls = ncalls.decrementAndGet();
                            event.eventCallBackHandler.callback(event.selectableChannel,event.ev_res,event.args);
                            if (eventBase.event_break) {
                                return;
                            }
                        }
                    }
                }
                if (!eventBase.timeheap.isEmpty()) {
                    long now = getTime(eventBase);
                    Event event = null;
                    while ((event = eventBase.timeheap.peek()) != null) {
                        if (event.ev_timeout > now) {
                            break;
                        }
                        this.event_del(event);
                        event_active(event,EventConfig.EV_TIMEOUT,1);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Event event_set(SelectableChannel channel, int ev_events, EventCallBackHandler eventCallBackHandler, Object... args) {
        Event event = new Event();
        event.eventBase = this.eventBase;
        event.setEv_events(ev_events);
        event.setSelectableChannel(channel);
        event.setArgs(args);
        event.setEventCallBackHandler(eventCallBackHandler);
        event.setEv_pri(eventBase.active_queues.length / 2);
        event.setEv_pncalls(null);
        return event;
    }

    @Override
    public void event_active(Event event, int ev_res, int ncalls) {
        if ((event.ev_flags & EventConfig.EVLIST_ACTIVE) > 0) {
            event.ev_flags |= ev_res;
            return;
        }
        event.ev_ncalls = ncalls;
        event.ev_pncalls = null;
        event.ev_res = ev_res;
        eventQueueInsert(event,EventConfig.EVLIST_ACTIVE);
    }

    public void eventQueueInsert(Event event,int queue) {
        if ((event.ev_flags & queue) > 0) {
            if ((event.ev_flags & EventConfig.EVLIST_ACTIVE) > 0) {
                return;
            }
        }
        event.ev_flags |= queue;
        switch (queue) {
            case EventConfig.EVLIST_INSERTED:
                eventBase.events_queue.add(event);
                break;
            case EventConfig.EVLIST_TIMEOUT:
                eventBase.timeheap.add(event);
                break;
            case EventConfig.EVLIST_ACTIVE:
                eventBase.active_queues[event.ev_pri].add(event);
                eventBase.event_count_active++;
                break;
            default:throw new IllegalArgumentException("unknow queue" + queue);
        }
    }

    public void eventQueueRemove(Event event,int queue) {
        if (!((event.ev_flags & queue) > 0)) {
            throw new IllegalArgumentException("event " + event + " is not in queue" + queue);
        }
        event.ev_flags &= ~queue;
        switch (queue) {
            case EventConfig.EVLIST_INSERTED:
                eventBase.events_queue.remove(event);
                break;
            case EventConfig.EVLIST_TIMEOUT:
                eventBase.timeheap.remove(event);
                break;
            case EventConfig.EVLIST_ACTIVE:
                eventBase.active_queues[event.ev_pri].remove(event);
                eventBase.event_count_active--;
                break;
            default:
                throw new IllegalArgumentException("unknow event queue " + queue);
        }

    }

    private boolean isValidEvent(Event event) {
        return (event.ev_events & (EventConfig.EV_ACCEPT|EventConfig.EV_READ|EventConfig.EV_WRITE|EventConfig.EV_CONNECT))>0;
    }

    private boolean isActiveEvent(Event event) {
        return (event.ev_flags & (Event.EVLIST_INSERTED|Event.EVLIST_ACTIVE)) > 0;
    }

    public long getTime(EventBase eventBase) {
        if (eventBase.timecache > 0) {
            return eventBase.timecache;
        }
        return System.currentTimeMillis();
    }
    private long getTimeoutNext() {
        long selectionTimeout = 1000L;
        if (this.eventBase.event_count_active > 0) {
            selectionTimeout = -1;
        } else {
            Event timeoutEvent = eventBase.timeheap.peek();
            if (timeoutEvent != null) {
                long now = getTime(this.eventBase);
                if (timeoutEvent.ev_timeout < now) {
                    selectionTimeout = -1L;
                } else
                    selectionTimeout = timeoutEvent.ev_timeout - now;
            }
        }
        return selectionTimeout;
    }

    public static void main(String[] args) {
        int a = EventConfig.EVLIST_INIT;
        System.out.println(EventConfig.EVLIST_INIT);
        System.out.println(EventConfig.EVLIST_ACTIVE);
        a |= EventConfig.EVLIST_INSERTED;
        System.out.println(a&EventConfig.EVLIST_TIMEOUT);
    }
}
