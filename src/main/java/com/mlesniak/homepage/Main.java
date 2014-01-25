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
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;

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

        if (args.length > 1 && args[1].equals("-w")) {
            startWatch();
        }
    }

    /** Quick and dirty watch service w/o error handling. */
    private void startWatch() {
        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            Path source = Paths.get(Config.get("source"));
            WatchKey key = source.register(watcher,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);

            while (true) {
                WatchKey watchKey;
                watchKey = watcher.take();
                for (WatchEvent<?> event: key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    WatchEvent<Path> ev = (WatchEvent<Path>)event;
                    Path filename = ev.context();
                    if (filename.toString().equals("_site")) {
                        break;
                    }
                    System.out.println("Changed: " + filename);
                    build();
                }

                watchKey.reset();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
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
        System.out.println("Starting build. " + new Date());
        File sourceDirectory = Config.getFile("source");
        File targetDirectory = Config.getFile("target");

        try {
            FileUtils.deleteDirectory(targetDirectory);
            FileUtils.forceMkdir(targetDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }

        copyStaticFiles(sourceDirectory, targetDirectory);
        createDynamicFiles(sourceDirectory, targetDirectory);
    }

    private void copyStaticFiles(File sourceDirectory, File targetDirectory) {
        IOFileFilter filter = new IOFileFilter() {
            @Override
            public boolean accept(File file) {
                return accept(file.getName());
            }

            @Override
            public boolean accept(File dir, String name) {
                return accept(dir.getName());
            }

            private boolean accept(String name) {
                return !(name.startsWith("_") || name.startsWith("."));
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
            if (file.getName().startsWith("_")) {
                continue;
            }
            System.out.println("  Processing " + file);
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
                    new File(sourceDirectory.getPath() + "/_layout/" + config.getProperty("layout") + ".html"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Replace variables.
        for (String key : config.stringPropertyNames()) {
            layout = layout.replaceAll("\\$\\{" + key + "\\}", Matcher.quoteReplacement(config.getProperty(key)));
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
