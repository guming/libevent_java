package org.jinn.libevent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Created by gumingcn on 16/3/22.
 */
public class ClientTest {
    private final static InetSocketAddress SERVER_ADDRESS = new InetSocketAddress(
            "localhost", 8080);
    private Selector selector;
    private String message="hello";

    public ClientTest init(String msg) throws IOException{
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        selector=Selector.open();
        message += msg;
        channel.connect(SERVER_ADDRESS);
        channel.register(selector, SelectionKey.OP_CONNECT);
        return this;
    }

    public void listen() throws IOException{
            System.out.println("client starting...");
            while(true){
                selector.select();
                Iterator<SelectionKey> ite = selector.selectedKeys().iterator();
                while(ite.hasNext()){
                    SelectionKey key = ite.next();
                    ite.remove();
                    if(key.isConnectable()){
                        SocketChannel channel=(SocketChannel)key.channel();
                        if(channel.isConnectionPending()){
                            channel.finishConnect();
                        }

                        channel.configureBlocking(false);
                        channel.write(ByteBuffer.wrap(message.getBytes()));
                        channel.register(selector, SelectionKey.OP_READ);
                        System.out.println("connected success.");
                    }else if(key.isReadable()){
                        SocketChannel channel = (SocketChannel)key.channel();

                        ByteBuffer buffer = ByteBuffer.allocate(10);
                        channel.read(buffer);
                        byte[] data = buffer.array();
                        String message = new String(data);
                        System.out.println("recv: "+message);
                        channel.close();
//                        selector.close();
//                        break;
                    }
                }
            }
        }

    public static void main(String[] args) throws Exception {
            for (int i = 0; i <1000; i++) {
                final int t=i;
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            new ClientTest().init(""+t).listen();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
                System.out.println("i:"+i);
            }

            Thread.sleep(1000*60);
        }

}
