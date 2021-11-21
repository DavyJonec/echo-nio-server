import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Client extends Thread {
    private Selector selector;
    private SocketChannel socketChannel;
    private volatile boolean stop;
    SocketChannel channel;


    public static void main(String[] args) {
        Client echoClient = null;
        try {
            echoClient = new Client();
            echoClient.start();
            System.out.println("Client started");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Client() throws IOException {
        selector = Selector.open();
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
    }

    @Override
    public void run() {
        try {
            boolean connected = socketChannel.connect(new InetSocketAddress("127.0.0.1", 12563));
            if (connected) {
                socketChannel.register(selector, SelectionKey.OP_WRITE);
            } else {
                socketChannel.register(selector, SelectionKey.OP_CONNECT);
            }
            while (!stop) {
                selector.select(1000);
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    handler(key);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handler(SelectionKey key) throws IOException {
        if (key.isValid()) {
            channel = (SocketChannel) key.channel();
            if (key.isConnectable()) {
                if (channel.finishConnect()) {
                    channel.register(selector, SelectionKey.OP_WRITE);
                }
            }
            if (key.isReadable()) {
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                int readCount = channel.read(readBuffer);
                if (readCount > 0) {
                    readBuffer.flip();
                    byte[] bytes = new byte[readBuffer.remaining()];
                    readBuffer.get(bytes);
                    System.out.println(new String(bytes, "UTF-8"));
                } else if (readCount < 0) {
                    key.cancel();
                    channel.close();
                }
                channel.register(selector, SelectionKey.OP_WRITE);
            }
            if (key.isWritable()) {
                BufferedReader localReader = new BufferedReader(new InputStreamReader(System.in));
                String msg = null;
                if (((msg = localReader.readLine()) != null)) {
                    ByteBuffer writeBuffer = ByteBuffer.allocate(msg.getBytes().length);
                    writeBuffer.put(msg.getBytes());
                    writeBuffer.flip();
                    socketChannel.write(writeBuffer);
                    socketChannel.register(selector, SelectionKey.OP_READ);
                }
            }
        }
    }


    private void sendMsg(String msg) throws IOException {
        ByteBuffer responseBuffer = ByteBuffer.allocate(msg.getBytes().length);
        responseBuffer.put(msg.getBytes());
        responseBuffer.flip();
        socketChannel.write(responseBuffer);
        channel.register(selector, SelectionKey.OP_READ);
    }

}
