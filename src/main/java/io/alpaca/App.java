package io.alpaca;

import com.google.common.collect.Sets;
import io.alpaca.models.ManifestEntry;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
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

    private static String MANIFEST = "manifest";

    private static final String tmpDir = System.getProperty("java.io.tmpdir") + File.separator + "alpaca" + File.separator + ProcessHandle.current().pid() + File.separator;

    public static void main(String[] args) {
        String targetClass = null;
        String jarPath;
        if (args != null && args.length == 1) {
            // Usage: java -jar alpaca-1.0.0.Final.jar <JAR_FILE_PATH>
            jarPath = args[0];
        } else if (args != null && args.length == 2) {
            // Usage: java -jar alpaca-1.0.0.Final.jar manifest <JAR_FILE_PATH>
            targetClass = args[0];
            jarPath = args[1];
        } else {
            System.out.println("Usage: java -jar alpaca-1.0.0.Final.jar <JAR_FILE_PATH>\n       java -jar alpaca-1.0.0.Final.jar " + MANIFEST + " <JAR_FILE_PATH>");
            return;
        }

        final StringBuffer output = new StringBuffer();

        if (targetClass != null && MANIFEST.equals(targetClass)) {
            final Set<String> lineSet = Collections.synchronizedSortedSet(Sets.newTreeSet());

            final var manifestEntries = Alpaca.scanManifestEntry(Paths.get(jarPath), tmpDir);
            for (ManifestEntry manifestEntry : manifestEntries) {
                lineSet.add(manifestEntry.toDeptopiaManifest());
            }
            output.append(String.join("\n", lineSet));

            // Clean up decompressed dir
            FileUtils.deleteQuietly(Paths.get(tmpDir).toFile());
        } else {
            try (JarFile jarFile = new JarFile(Paths.get(jarPath).toFile())) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry jarEntry = entries.nextElement();
                    String jarEntryName = jarEntry.getName();
                    if (jarEntryName.matches("META-INF/maven/.*/pom.xml")) {
                        pomList.add(jarEntry);
                    }

                    // return only the jarFile file name
                    if (jarEntryName.endsWith(".jarFile")) {
                        jarList.add(jarEntry);
                    }
                }

                if (pomList.size() == 0) {
                    String jarName = new File(jarFile.getName()).getName();
                    output.append(jarName);
                } else {
                    for (int i = 0; i < pomList.size(); i++) {
                        final JarEntry jarEntry = pomList.get(i);
                        String jarEntryName = jarEntry.getName();
                        String jarVersion = "";
                        try {
                            if (jarEntryName.matches("META-INF/maven/.*/pom.xml")) {
                                Model model = new MavenXpp3Reader().read(jarFile.getInputStream(jarEntry));
                                jarVersion = model.getVersion();
                                if (jarVersion == null) {
                                    jarVersion = model.getParent().getVersion();
                                }
                            }
                            output.append(jarEntryName.replace("pom.xml", jarVersion));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (i != pomList.size() - 1) {
                            output.append(",");
                        }
                    }
                }

                if (jarList.size() > 0) {
                    output.append("\n");
                }

                for (int i = 0; i < jarList.size(); i++) {
                    final JarEntry jarEntry = jarList.get(i);
                    String jarEntryName = jarEntry.getName();

                    // return only the jarFile file name
                    final String[] split = jarEntryName.split("/");
                    if (split != null) {
                        jarEntryName = split[split.length - 1];
                    }
                    output.append(jarEntryName);

                    if (i != jarList.size() - 1) {
                        output.append(",");
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        System.out.println(output);
    }
}