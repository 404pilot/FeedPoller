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

        // TODO why commented
        //long timeout = System.currentTimeMillis() + 5000;
        //
        //while (System.currentTimeMillis() < timeout) {
        //    if (server.isStarted())
        //        break;
        //
        //    TimeUnit.MILLISECONDS.sleep(100);
        //}

        System.out.println(server.isStarted() ? "Jetty server started!" : "**** Server failed to start! *****");
    }

    public void stop() throws Exception {
        server.stop();
    }
}