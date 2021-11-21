import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Server {

    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private volatile boolean stop;

    public static void main(String[] args) {
        try {
            new Server();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Server() throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(12563),1024);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started");

        while(!stop){
            selector.select(1000);
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> it = selectedKeys.iterator();
            SelectionKey key = null;
            while(it.hasNext()){
                key = it.next();
                it.remove();;
                handler(key);
            }
        }
    }

    private void handler(SelectionKey key) throws IOException {
        if(key.isValid()){
            if(key.isAcceptable()){
                ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                SocketChannel socketChannel = serverSocketChannel.accept();
                socketChannel.configureBlocking(false);
                socketChannel.register(selector,SelectionKey.OP_READ);
            }
            if(key.isReadable()){
                SocketChannel socketChannel = (SocketChannel) key.channel();
                ByteBuffer readBytes = ByteBuffer.allocate(1024);
                int readCount = socketChannel.read(readBytes);
                if(readCount > 0){
                    readBytes.flip();
                    byte[] bytes = new byte[readBytes.remaining()];
                    readBytes.get(bytes);
                    String receiveMsg = new String(bytes,"UTF-8");
                    if("bye".equals(receiveMsg)){
                        stop();
                        return;
                    }
                    String responseString ="Echo in the darkness: " + receiveMsg;
                    System.out.println(responseString);
                    responseString = "Echo: " + receiveMsg;
                    ByteBuffer responseBuffer = ByteBuffer.allocate(responseString.getBytes().length);
                    responseBuffer.put(responseString.getBytes());
                    responseBuffer.flip();
                    socketChannel.write(responseBuffer);
                }
            }
        }
    }

    public void stop(){
        this.stop = true;
    }

}
