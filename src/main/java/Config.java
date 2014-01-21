import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
    private static final String FILENAME = "_config.properties";
    private static Config INSTANCE;
    private final Properties properties;

    private Config() {
        properties = new Properties();
        try {
            properties.load(new FileInputStream(FILENAME));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public static Config get() {
        if (INSTANCE == null) {
            INSTANCE = new Config();
        }

        return INSTANCE;
    }

    public static String get(String property) {
        return get().properties.getProperty(property);
    }

    public static File getFile(String property) {
        return new File(get(property));
    }
}
