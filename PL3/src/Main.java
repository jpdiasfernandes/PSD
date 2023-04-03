import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        try {
            ServerSocketChannel ssc = ServerSocketChannel.open().bind(new InetSocketAddress(6201));
            MainLoop loop = new MainLoop();
            Observable<SocketChannel> server = loop.accept(ssc);
            server.subscribe( sc -> {
                Observable<ByteBuffer> reader = loop.read(sc);
                reader.subscribe( bb -> {
                    var utf8 = StandardCharsets.UTF_8;
                    System.out.println("Recebi: " + utf8.decode(bb));

                });
            });
            loop.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
