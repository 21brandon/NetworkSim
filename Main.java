import javax.swing.*;
import java.io.IOException;


public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        RouterManager routerManager = new RouterManager();
        routerManager.addRouter(null);
        final NetworkVisualizer gui = new NetworkVisualizer(routerManager);
        Timer timer = new Timer(25, (e) -> gui.draw());
        timer.start();
    }
}
