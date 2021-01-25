package io.alpaca;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Alpaca is a Jar Analyzer project.
 * <p/>
 * You can get bundled jars info including their version info from an uberjar. Alpaca will display the output in one line with comma separate..
 */
public class App {

    private static List<JarEntry> pomList = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        String jarPath;
        if (args != null && args.length == 1) {
            jarPath = args[0];
        } else {
            System.out.println("Usage: java -jar alpaca-1.0.0.Final.jar <JAR_FILE_PATH>");
            return;
        }

        try (JarFile jar = new JarFile(Paths.get(jarPath).toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                if (jarEntry.getName().matches("META-INF/maven/.*/pom.xml")) {
                    pomList.add(jarEntry);
                }
            }

            StringBuffer output = new StringBuffer();
            for (int i = 0; i < pomList.size(); i++) {
                final JarEntry jarEntry = pomList.get(i);
                String jarVersion = "";
                try {
                    Model model = new MavenXpp3Reader().read(jar.getInputStream(jarEntry));
                    jarVersion = model.getVersion();
                    if (jarVersion == null) {
                        jarVersion = model.getParent().getVersion();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                output.append(jarEntry.getName().replace("pom.xml", jarVersion));
                if (i != pomList.size() - 1) {
                    output.append(",");
                }
            }

            System.out.println(output);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
