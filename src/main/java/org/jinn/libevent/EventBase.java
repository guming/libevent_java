package org.jinn.libevent;

import java.util.LinkedList;
import java.util.PriorityQueue;

/**
 * Created by gumingcn on 2016/3/14.
 */
public class EventBase {

    public LinkedList<Event> events_queue;
    public LinkedList<Event>[] active_queues;
    public PriorityQueue<Event> timeheap;
    public int event_count_active=0;
    public int event_count=0;
    public EventOp eventOp;
    public boolean event_break;
    public long timecache=0;

    public EventBase() {
        this.events_queue = new LinkedList<Event>();
        this.timeheap = new PriorityQueue<Event>();
        this.active_queues = new LinkedList[5];
        for (int i = 0; i <active_queues.length ; i++) {
            this.active_queues[i] = new LinkedList<Event>();
        }
    }
}
