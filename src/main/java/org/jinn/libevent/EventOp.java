package org.jinn.libevent;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by gumingcn on 2016/3/14.
 * wrap the selector
 */
public class EventOp {

    private Selector selector;

    private LibEvent libEvent;

    private Map<SelectableChannel, Event> acceptEventByChannel = new HashMap<SelectableChannel, Event>();

    private Map<SelectableChannel,Event> readEventByChannel = new HashMap<SelectableChannel, Event>();

    private Map<SelectableChannel,Event> writeEventByChannel = new HashMap<SelectableChannel, Event>();

    public EventOp(LibEvent libEvent) {
        this.libEvent = libEvent;
    }

    public void init() throws IOException {
        selector = Selector.open();
    }

    public void add(Event event) throws IOException {
        if (event != null) {
            if((event.ev_events&EventConfig.EV_ACCEPT)>0){
                SelectionKey key = event.selectableChannel.keyFor(selector);
                if (key == null) {
                    key = event.selectableChannel.register(selector, SelectionKey.OP_ACCEPT);
                } else {
                    key.interestOps(key.interestOps() | SelectionKey.OP_ACCEPT);
                }
                acceptEventByChannel.put(event.selectableChannel,event);
            }else if((event.ev_events&EventConfig.EV_READ)>0){
                SelectionKey key = event.selectableChannel.keyFor(selector);
                if (key == null) {
                    key = event.selectableChannel.register(selector, SelectionKey.OP_READ);
                } else {
                    key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                }
                readEventByChannel.put(event.selectableChannel,event);
            }else if((event.ev_events&EventConfig.EV_WRITE)>0){
                SelectionKey key = event.selectableChannel.keyFor(selector);
                if (key == null) {
                    key = event.selectableChannel.register(selector, SelectionKey.OP_WRITE);
                } else {
                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                }
                writeEventByChannel.put(event.selectableChannel,event);
            }
        }

    }

    public void delete(Event event) {
        if((event.ev_events&EventConfig.EV_READ)==Event.EV_READ){
            SelectionKey key = event.selectableChannel.keyFor(selector);
            this.readEventByChannel.remove(event.selectableChannel);
            if(key!=null){
                key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
            }
        }
        if((event.ev_events&EventConfig.EV_WRITE)==Event.EV_WRITE){
            SelectionKey key = event.selectableChannel.keyFor(selector);
            this.writeEventByChannel.remove(event.selectableChannel);
            if(key!=null){
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            }
        }
    }

    public void destory() throws IOException {
        for (SelectableChannel channel : readEventByChannel.keySet()) {
            SelectionKey key = channel.keyFor(selector);
            if (key != null && key.isValid()) {
                key.cancel();
            }
        }
        for (SelectableChannel channel : writeEventByChannel.keySet()) {
            SelectionKey key = channel.keyFor(selector);
            if (key != null && key.isValid()) {
                key.cancel();
            }
        }
        selector.close();

    }

    public int dispatch(EventBase eventBase, long timeout) throws IOException {
        if (timeout > 0) {
            int n = selector.select(timeout);
            if (n > 0) {
                Set<SelectionKey> set=selector.selectedKeys();
                Iterator<SelectionKey> it = set.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    Event accept=null;
                    Event read = null;
                    Event write = null;
                    int ev_res=0;
                    if(!key.isValid()){
                        continue;
                    }
                    if ((key.interestOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                        ev_res |= EventConfig.EV_ACCEPT;
                        accept = acceptEventByChannel.get(key.channel());
                    }
                    if ((key.interestOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                        ev_res |= EventConfig.EV_READ;
                        read = readEventByChannel.get(key.channel());
                    }
                    if ((key.interestOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                        ev_res |= EventConfig.EV_WRITE;
                        write = writeEventByChannel.get(key.channel());
                    }
                    if (accept != null && (accept.ev_events & EventConfig.EV_ACCEPT)>0) {
                        libEvent.event_active(accept,ev_res,1);
                    }
                    if (read != null && (read.ev_events & EventConfig.EV_READ)>0) {
                        libEvent.event_active(read,ev_res,1);
                    }
                    if (write != null && (write.ev_events & EventConfig.EV_WRITE)>0) {
                        libEvent.event_active(write,ev_res,1);
                    }
                    it.remove();
                }
            }
            return n;
        }else{
            return 0;
        }
    }



}
