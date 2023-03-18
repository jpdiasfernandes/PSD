import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ServeRunnable implements Runnable{
    private List<SocketChannel> receivers;
    private SocketChannel s;
    public ServeRunnable(List<SocketChannel> receivers, SocketChannel s) {
        this.receivers = receivers;
        this.s = s;
    }

    public void run() {
        ByteBuffer buf = ByteBuffer.allocate(1000);
        Charset utf8 = StandardCharsets.UTF_8;
        try {
            int size = -1;
            while((size = s.read(buf)) > 0) {
                //System.out.println(size);
                buf.flip();
                String out = String.valueOf(utf8.decode(buf));
                System.out.println(out);
                buf.flip();

                for(SocketChannel r: receivers) {
                    r.write(buf.duplicate());
                    System.out.println("Enviei " + out + " para " + r.toString());
                }
                buf.clear();
            }
            s.close();
            receivers.remove(s);
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

            List<SocketChannel> receivers = new ArrayList<>();
            while(true) {
                SocketChannel s = ss.accept();
                s.configureBlocking(true);
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