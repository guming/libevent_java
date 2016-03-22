package org.jinn.libevent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

/**
 * Created by gumingcn on 16/3/22.
 */
public class EchoServer {
    private static final long now = System.currentTimeMillis();
    static final class ReadHandler implements EventCallBackHandler{
        private LibEvent libEvent;
        @Override
        public void callback(SelectableChannel channel, int interestEvent, Object... args) {
            ByteBuffer dist = ByteBuffer.allocate(1024);
            try {
                ((ReadableByteChannel) channel).read(dist);
                dist.flip();
                Event event = libEvent.event_set(channel, EventConfig.EV_WRITE, new WriteHandler(dist,libEvent));
                libEvent.event_add(event,-1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public ReadHandler(LibEvent libEvent) {
            this.libEvent = libEvent;
        }
    }
    static final class WriteHandler implements EventCallBackHandler{
        private final ByteBuffer src;
        private final LibEvent libEvent;
        WriteHandler(ByteBuffer byteBuffer,LibEvent libEvent) {
            this.src = byteBuffer;
            this.libEvent = libEvent;
        }

        @Override
        public void callback(SelectableChannel channel, int interestEvent, Object... args) {
            try {
                ((WritableByteChannel) channel).write(src);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static final class AcceptHandler implements EventCallBackHandler{
        private final LibEvent libEvent;
        @Override
        public void callback(SelectableChannel channel, int interestEvent, Object... args) {
            if ((interestEvent & EventConfig.EV_ACCEPT) == EventConfig.EV_ACCEPT) {
                ServerSocketChannel server = (ServerSocketChannel) channel;
                try {
                    SocketChannel socket = server.accept();
                    System.out.println(socket.socket().getRemoteSocketAddress()+" connect "+(System.currentTimeMillis()-now));
                    socket.configureBlocking(false);
                    Event event = libEvent.event_set(socket, EventConfig.EV_READ | EventConfig.EV_PERSIST, new ReadHandler(libEvent));
                    libEvent.event_add(event,-1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public AcceptHandler(LibEvent libEvent) {
            this.libEvent = libEvent;
        }
    }

    public static void main(String[] args) {

        try {
            final LibEvent libEvent = new LibEventHandler();
            libEvent.init();
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(8080));
            Event event = libEvent.event_set(serverSocketChannel, EventConfig.EV_ACCEPT|EventConfig.EV_PERSIST, new AcceptHandler(libEvent));
            libEvent.event_add(event,2000);
            System.out.println("server starting...");
            libEvent.event_loop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
