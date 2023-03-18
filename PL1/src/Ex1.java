import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

class ServeRunnable implements Runnable{
    private Set<SocketChannel> receivers;
    private SocketChannel s;
    public ServeRunnable(Set<SocketChannel> receivers, SocketChannel s) {
        this.receivers = receivers;
        this.s = s;
    }

    public void run() {
        ByteBuffer buf = ByteBuffer.allocate(100);
        Charset utf8 = StandardCharsets.UTF_8;
        try {
            int size = -1;
            while((size = s.read(buf)) > 0) {
                buf.flip();
                System.out.println(utf8.decode(buf));
                buf.flip();

                for(SocketChannel r: receivers) {
                    r.write(buf.duplicate());
                }
                buf.clear();
            }
            s.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }
}

public class Ex1 {
    public static void main(String[] args) {
        try {
            ServerSocketChannel ss = ServerSocketChannel.open();
            ss.bind(new InetSocketAddress(6201));

            Set<SocketChannel> receivers = new HashSet<>();
            while(true) {
                SocketChannel s = ss.accept();
                System.out.println("Aceitei um pedido");
                receivers.add(s);
                Thread serve = new Thread(new ServeRunnable(receivers, s));
                serve.start();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }
}