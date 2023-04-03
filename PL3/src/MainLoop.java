import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.Observer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Map;

public class MainLoop {

    private Selector sel;
    private Map<ObservableEmitter<SocketChannel>, ServerSocketChannel> acceptors;
    private Map<ObservableEmitter<ByteBuffer>, SocketChannel> readers;
    public MainLoop() throws IOException {
        this.sel = SelectorProvider.provider().openSelector();
        this.acceptors = new HashMap<>();
        this.readers = new HashMap<>();
    }
    public Observable<SocketChannel> accept(ServerSocketChannel ssc) {
        return Observable.create(sub -> {
            ssc.configureBlocking(false);
            ssc.register(this.sel, SelectionKey.OP_ACCEPT, sub);
            this.acceptors.put(sub, ssc);
        });
    }

    public Observable<ByteBuffer> read(SocketChannel sc) {
        return Observable.create(sub -> {
            sc.configureBlocking(false);
            sc.register(this.sel, SelectionKey.OP_READ, sub);
            this.readers.put(sub, sc);
        });
    }

    public void run() throws IOException {
        while(true) {
            this.sel.select();
            for(SelectionKey key : sel.selectedKeys().stream().toList()) {
                if (key.isAcceptable()) {
                    ObservableEmitter<SocketChannel> sub = (ObservableEmitter<SocketChannel>) key.attachment();
                    try {
                        ServerSocketChannel ssc = this.acceptors.get(sub);
                        SocketChannel sc = ssc.accept();
                        sub.onNext(sc);
                    } catch (Exception e) {
                        sub.onError(e);
                    }
                }
                if (key.isReadable()) {
                    ObservableEmitter<ByteBuffer> sub = (ObservableEmitter<ByteBuffer>) key.attachment();
                    try {
                        ByteBuffer buf = ByteBuffer.allocate(100);
                        SocketChannel sc = this.readers.get(sub);
                        if (sc.read(buf) >= 0) {
                            buf.flip();
                            sub.onNext(buf);
                        } else {
                            key.cancel();
                            sc.close();
                            this.readers.remove(sub);
                            sub.onComplete();
                        }
                    } catch (Exception e) {
                        sub.onError(e);
                    }
                }
            }
            sel.selectedKeys().clear();
        }
    }


}
