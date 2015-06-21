package feedpoller.stub;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;

public class SimpleServer {
    private Server server;

    public SimpleServer(Handler handler, int port) {
        server = new Server(port);
        server.setHandler(handler);
    }

    public void start() throws Exception {
        server.start();
        
        long start = System.currentTimeMillis();

        while (!server.isStarted()) {
            long current = System.currentTimeMillis();
            if (current - start > 2000) {
                throw new Error("Jetty is not started in 2 seconds");
            }
        }

        System.out.println("Jetty server started!");
    }

    public void stop() throws Exception {
        server.stop();
    }
}