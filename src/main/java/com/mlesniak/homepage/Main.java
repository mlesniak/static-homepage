package com.mlesniak.homepage;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

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
        File pages = new File(sourceDirectory.toPath() + "/_pages");
        for (File file : FileUtils.listFiles(pages, null, false)) {
            handleDynamicFile(sourceDirectory, targetDirectory, file);
        }
    }

    private void handleDynamicFile(File sourceDirectory, File targetDirectory, File file) {
        // Read configuration.
        List<String> lines = null;
        lines = readLines(file, lines);
        Properties config = loadConfiguration(lines);

        String layout = null;
        try {
            layout = FileUtils.readFileToString(
                    new File(sourceDirectory.getPath() + "/_layout/"+ config.getProperty("layout") + ".html"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Replace variables.
        for (String key : config.stringPropertyNames()) {
            layout = layout.replaceAll("\\$\\{" + key + "\\}", config.getProperty(key));
        }

        // Replace content.
        layout = layout.replaceAll("\\$\\{content\\}", StringUtils.join(lines, "\n"));

        try {
            FileUtils.write(new File(targetDirectory.getPath() + "/" + file.getName()), layout);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private List<String> readLines(File file, List<String> lines) {
        try {
            lines = IOUtils.readLines(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return lines;
    }

    private Properties loadConfiguration(List<String> lines) {
        List<String> configuration = new LinkedList<>();
        Iterator<String> iterator = lines.iterator();
        while (iterator.hasNext()) {
            String line = iterator.next();
            configuration.add(line);
            iterator.remove();
            if (StringUtils.isBlank(line)) {
                break;
            }
        }

        Properties config = new Properties();
        try {
            config.load(new StringReader(StringUtils.join(configuration, "\n")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }
}
