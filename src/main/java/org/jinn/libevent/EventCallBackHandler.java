package org.jinn.libevent;

import java.nio.channels.SelectableChannel;

/**
 * Created by gumingcn on 2016/3/14.
 */
public interface EventCallBackHandler {
	public void callback(SelectableChannel channel, int interestEvent,
						 Object... args);
}
