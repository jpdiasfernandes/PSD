import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class Server {

    public static void main(String[] args) {
        try {
            ServerSocketChannel ss = ServerSocketChannel.open();
            ss.bind(new InetSocketAddress(6201));
            ss.configureBlocking(false);
            Selector sel = SelectorProvider.provider().openSelector();
            // Estou a avisar o selector que quero ele me teste se as operações de aceitação para este canal estão
            // prontas quando o select() for invocado
            ss.register(sel, SelectionKey.OP_ACCEPT);

            Set<SelectionKey> receivers =  new HashSet<>();
            List<ByteBuffer> history = new ArrayList<>();

            while(true) {
                sel.select();
                for(SelectionKey key : sel.selectedKeys().stream().toList()) {
                    // para canais de aceitação
                    if (key.isAcceptable()) {
                        System.out.println("A minha chave é " + key);
                        SocketChannel s = ss.accept();
                        s.configureBlocking(false);
                        SelectionKey newKey = s.register(sel, SelectionKey.OP_READ);
                        receivers.add(newKey);
                        newKey.attach(0);
                    }
                    // para canais de leitura
                    if (key.isReadable()) {
                        System.out.println("Estou a ler");
                        ByteBuffer buf = ByteBuffer.allocate(100);
                        SocketChannel s = (SocketChannel) key.channel();

                        if (s.read(buf) >= 0) {
                            buf.flip();
                            history.add(buf.duplicate());
                            buf.flip();
                            for(SelectionKey k : receivers)
                                k.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        } else {
                            key.cancel();
                            s.close();
                            receivers.remove(key);
                            continue;
                        }
                    }
                    if (key.isWritable()) {
                        System.out.println("Estou a escrever");
                        SocketChannel s = (SocketChannel) key.channel();
                        int next = (int) key.attachment();
                        ByteBuffer buf = history.get(next);
                        key.attach(++next);

                        if (history.size() == next) {
                            System.out.println("Vou passar só para read porque já escrevi a última mensagem");
                            key.interestOps(SelectionKey.OP_READ);
                        }

                        int r = s.write(buf);
                        buf.flip();
                        if (r <= 0) {
                            key.cancel();
                            receivers.remove(key);
                            s.close();
                        }
                    }
                }
                sel.selectedKeys().clear();
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
