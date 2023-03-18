import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.TimeUnit;

class SleepyClientSender implements Runnable {

    private int amount;
    private int max;
    private SocketChannel client;
    public SleepyClientSender(int min_amount, int max_amount, SocketChannel client) {
        this.amount = min_amount;
        this.max = max_amount;
        this.client = client;
    }

    private int getRandomAmount() {
        return (int)(Math.random() * (this.max - this.amount + 1) + this.amount);
    }
    public void run() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(100);
            ReadableByteChannel in = Channels.newChannel(System.in);
            while(in.read(buffer) > 0) {
                buffer.flip();
                client.write(buffer);
                buffer.clear();
            }
            client.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}


class SleepyClientReceiver implements Runnable {

    private int amount;
    private int max;
    private SocketChannel client;
    public SleepyClientReceiver(int min_amount, int max_amount, SocketChannel client) {
        this.amount = min_amount;
        this.max = max_amount;
        this.client = client;
    }

    private int getRandomAmount() {
        return (int)(Math.random() * (this.max - this.amount + 1) + this.amount);
    }
    public void run() {
        Charset utf8 = StandardCharsets.UTF_8;
        try {

            ByteBuffer buffer = ByteBuffer.allocate(100);
            while(client.read(buffer) > 0) {
                buffer.flip();
                System.out.println(utf8.decode(buffer));
                buffer.clear();
                int timeout = this.getRandomAmount();
                System.out.println("Sleeping " + timeout);
                TimeUnit.SECONDS.sleep(timeout);
            }
            client.close();
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
}

public class Ex2 {
    public static void main(String[] args) {
        int clients = 5;
        int min_amount = Integer.parseInt(args[0]);
        int max_amount = Integer.parseInt(args[1]);
        //System.out.println(min_amount);
        //System.out.println(max_amount);

        try {
            SocketChannel client = SocketChannel.open();
            client.connect(new InetSocketAddress("localhost", 6201));
            Thread sender = new Thread(new SleepyClientSender(min_amount, max_amount, client));
            Thread receiver = new Thread(new SleepyClientReceiver(min_amount, max_amount, client));

            sender.start();
            receiver.start();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }
}
