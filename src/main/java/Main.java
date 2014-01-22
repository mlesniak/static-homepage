import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

import java.io.File;
import java.io.IOException;

public class Main {
    String[] args;

    public static void main(String[] args) {
        Main main = new Main(args);
        main.handleCommandLine();
    }

    public Main(String[] args) {
        this.args = args;
    }

    private void handleCommandLine() {
        switch (args[0]) {
            case "serve":
                serve();
                break;
            case "build":
            default:
                build();
        }
    }

    private void serve() {
        String contentRoot = Config.get("target");
        Server server = new Server(8080);

        System.out.println("Serving content from " + contentRoot);
        ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setDirectoriesListed(true);
        resource_handler.setWelcomeFiles(new String[]{"index.html"});
        resource_handler.setResourceBase(contentRoot);

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{resource_handler, new DefaultHandler()});
        server.setHandler(handlers);

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private void build() {
        File sourceDirectory = Config.getFile("source");
        File targetDirectory = Config.getFile("target");
        copyStaticFiles(sourceDirectory, targetDirectory);
        createDynamicFiles(sourceDirectory, targetDirectory);
    }

    private void copyStaticFiles(File sourceDirectory, File targetDirectory) {
        IOFileFilter filter = new IOFileFilter() {
            @Override
            public boolean accept(File file) {
                return !file.getName().startsWith("_");
            }

            @Override
            public boolean accept(File dir, String name) {
                return !dir.getName().startsWith("_");
            }
        };

        try {
            FileUtils.copyDirectory(sourceDirectory, targetDirectory, filter);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private void createDynamicFiles(File sourceDirectory, File targetDirectory) {

    }
}
