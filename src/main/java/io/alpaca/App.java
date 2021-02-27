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
    private static List<JarEntry> jarList = new ArrayList<>();

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
                String jarEntryName = jarEntry.getName();
                if (jarEntryName.matches("META-INF/maven/.*/pom.xml")) {
                    pomList.add(jarEntry);
                }

                // return only the jar file name
                if (jarEntryName.endsWith(".jar")) {
                    jarList.add(jarEntry);
                }
            }

            StringBuffer output = new StringBuffer();
            for (int i = 0; i < pomList.size(); i++) {
                final JarEntry jarEntry = pomList.get(i);
                String jarEntryName = jarEntry.getName();
                String jarVersion = "";
                try {
                    if (jarEntryName.matches("META-INF/maven/.*/pom.xml")) {
                        Model model = new MavenXpp3Reader().read(jar.getInputStream(jarEntry));
                        jarVersion = model.getVersion();
                        if (jarVersion == null) {
                            jarVersion = model.getParent().getVersion();
                        }
                    }
                    output.append(jarEntry.getName().replace("pom.xml", jarVersion));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (i != pomList.size() - 1) {
                    output.append(",");
                }
            }

            if (jarList.size() > 0) {
                output.append("\n");
            }

            for (int i = 0; i < jarList.size(); i++) {
                final JarEntry jarEntry = jarList.get(i);
                String jarEntryName = jarEntry.getName();

                // return only the jar file name
                final String[] split = jarEntryName.split("/");
                if (split != null) {
                    jarEntryName = split[split.length - 1];
                }
                output.append(jarEntryName);

                if (i != jarList.size() - 1) {
                    output.append(",");
                }
            }

            System.out.println(output);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
