package org.jinn.libevent;

import java.io.IOException;
import java.nio.channels.SelectableChannel;

/**
 * Created by gumingcn on 2016/3/14.
 */
public  interface LibEvent {

    public void init() throws IOException;

    public void event_add(Event event,long timeout) throws IOException;

    public void event_del(Event event);

    public void event_loop();

    public Event event_set(SelectableChannel channel,int ev_events,EventCallBackHandler eventCallBackHandler,Object... args);

    public void event_active(Event event,int ev_res,int ncalls);

}
