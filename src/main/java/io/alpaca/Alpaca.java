package io.alpaca;

import com.google.common.collect.Sets;
import io.alpaca.models.ManifestEntry;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Generate manifest entries for a given jar file.
 * <p/>
 * See: https://github.com/tedwon/alpaca
 */
public class Alpaca {

    public static final String buildMetadata = "META-INF/build.metadata";

    public static final String UNKNOWN = "Unknown";

    public static Set<ManifestEntry> scanManifestEntry(Path jarFilePath) {
        return scanManifestEntry("", "", jarFilePath);
    }

    public static Set<ManifestEntry> scanManifestEntry(final String productName, final String productVersion, final Path jarFilePath) {
        Set<ManifestEntry> manifests = Collections.synchronizedSet(Sets.newHashSet());

        ManifestEntry manifestEntry = null;

        boolean scanMainJarManifestFinished = false;
        boolean scanUberJarsFinished = false;

        final var jarPathToFile = jarFilePath.toFile();
        try (final JarFile jarFile = new JarFile(jarPathToFile)) {
            final var jarName = jarPathToFile.getName();
            final var jarAbsolutePath = jarFile.getName();

            // check if the jar file has META-INF/MANIFEST
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                // read MANIFEST
                manifestEntry = getEntryFromJarManifest(productName, productVersion, jarName, manifest);
                if (manifestEntry.getJarArtifactId() != null && manifestEntry.getJarVersion() != null && manifestEntry.getJarPomName() != null) {
                    manifestEntry.setJarFilePath(jarAbsolutePath);
                    scanMainJarManifestFinished = checkManifestEntry(manifestEntry);
                    if (scanMainJarManifestFinished) {
                        manifests.add(manifestEntry);
                    }
                }
            }

            // check if the jar file has META-INF/build.metadata
            final var jarEntry = getBuildMetadataFile(jarFile);
            if (!scanMainJarManifestFinished && jarEntry != null) {
                // read build.metadata
                InputStream input = jarFile.getInputStream(jarEntry);
                Properties prop = new Properties();
                // load a properties file
                prop.load(input);
                // build.artifactId=camel-archetype-activemq
                final String artifactId = prop.getProperty("build.artifactId");
                // build.version=2.23.2.fuse-780036-redhat-00001
                final String version = prop.getProperty("build.version");
                if (artifactId != null && version != null) {
                    manifestEntry = new ManifestEntry(productName, productVersion, jarName, artifactId, version, artifactId, jarAbsolutePath);
                    scanMainJarManifestFinished = checkManifestEntry(manifestEntry);
                    if (scanMainJarManifestFinished) {
                        manifests.add(manifestEntry);
                    }
                }
            }

            // check whether it is uberjar or not
            int pomCountInJarFile = countPOMFiletoCheckUberJar(jarFile);
            if (pomCountInJarFile >= 2) {
                // to exclusive the main jar's pom.xml file from bundledjar entries
                String jarsMainPOMFile = null;

                // if it is uberjar
                if (!scanMainJarManifestFinished) {
                    // find pom.xml of the jar file among all pom.xml files including bundled jars in the file
                    // read pom.xml
                    final Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry jarEntryForPOMFile = entries.nextElement();
                        String jarEntryNameForPOMFile = jarEntryForPOMFile.getName();
                        if (jarEntryNameForPOMFile.matches("META-INF/maven/.+/pom.xml")) {
                            // jarEntryNameForPOMFile == META-INF/maven/org.slf4j/jcl-over-slf4j/pom.xml
                            Model model = new MavenXpp3Reader().read(jarFile.getInputStream(jarEntryForPOMFile));
                            String version = model.getVersion();
                            if (version == null) {
                                version = model.getParent().getVersion();
                            }

                            // if this pom.xml is for the jarFile
                            Path path = Paths.get(jarEntryNameForPOMFile);
                            String artifactName = path.getName(3).toString();
                            if (jarName.contains(artifactName)) {
                                // to exclusive the main jar's pom.xml file from bundledjar entries
                                jarsMainPOMFile = jarEntryNameForPOMFile;
                                String jarPomName = model.getName();
                                String artifactId = model.getArtifactId();
                                if (artifactId != null && version != null) {
                                    if (jarPomName == null || jarPomName.contains("${")) {
                                        jarPomName = artifactId;
                                    }
                                    manifestEntry = new ManifestEntry(productName, productVersion, jarName, artifactId, version, jarPomName, jarAbsolutePath);
                                    scanMainJarManifestFinished = checkManifestEntry(manifestEntry);
                                    manifests.add(manifestEntry);
                                }
                            }
                        }
                    }
                }

                // create manifest entry for the bundled jar inside of the jar file
                final Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry jarEntryForPOMFile = entries.nextElement();
                    String jarEntryNameForPOMFile = jarEntryForPOMFile.getName();
                    if (jarEntryNameForPOMFile.matches("META-INF/maven/.+/pom.xml")) {
                        if (jarEntryNameForPOMFile.equals(jarsMainPOMFile)) {
                            // to exclusive the main jar's pom.xml file from bundledjar entries
                            continue;
                        }

                        // jarEntryName: META-INF/maven/org.slf4j/jcl-over-slf4j/pom.xml
                        Model model = new MavenXpp3Reader().read(jarFile.getInputStream(jarEntryForPOMFile));
                        String version = model.getVersion();
                        if (version == null) {
                            version = model.getParent().getVersion();
                        }

                        String bundledJarNameAndVersion = jarEntryNameForPOMFile.replace("pom.xml", version);
                        ManifestEntry manifestEntryForBundledJar = new ManifestEntry(manifestEntry);
                        manifestEntryForBundledJar.setBundledJarName(bundledJarNameAndVersion);
                        manifests.add(manifestEntryForBundledJar);
                    }
                }
            } else if (pomCountInJarFile == 1) {
                // if it is not uberjar
                if (!scanMainJarManifestFinished) {
                    // create manifest entry for the jar file
                    final Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry jarEntryForPOMFile = entries.nextElement();
                        String jarEntryNameForPOMFile = jarEntryForPOMFile.getName();
                        if (jarEntryNameForPOMFile.matches("META-INF/maven/.+/pom.xml")) {
                            Model model = new MavenXpp3Reader().read(jarFile.getInputStream(jarEntryForPOMFile));
                            String version = model.getVersion();
                            if (version == null) {
                                version = model.getParent().getVersion();
                            }
                            String jarPomName = model.getName();
                            String artifactId = model.getArtifactId();
                            if (artifactId != null && version != null) {
                                if (jarPomName == null || jarPomName.contains("${")) {
                                    jarPomName = artifactId;
                                }
                                manifestEntry = new ManifestEntry(productName, productVersion, jarName, artifactId, version, jarPomName, jarAbsolutePath);
                                scanMainJarManifestFinished = checkManifestEntry(manifestEntry);
                                manifests.add(manifestEntry);

                            }
                        }
                    }
                }
            } else {
                // pom.xml not found in the jar file
                if (!scanMainJarManifestFinished) {
                    manifestEntry = new ManifestEntry(productName, productVersion, jarName, jarName.replaceAll(".jar", ""), UNKNOWN, UNKNOWN, jarAbsolutePath);
                    scanMainJarManifestFinished = checkManifestEntry(manifestEntry);
                    manifests.add(manifestEntry);
                }
            }

            // check whether there is any other jar file inside the jar file
            // if found a jar
            // recursively call this method for the jar
            final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntryForJarFile = entries.nextElement();
                String jarEntryNameForJarFile = jarEntryForJarFile.getName();
                if (jarEntryNameForJarFile.matches(".+/*.jar")) {
                    try {
                        InputStream input = jarFile.getInputStream(jarEntryForJarFile);
                        String tempDir = System.getProperty("java.io.tmpdir");
                        File file = new File(tempDir + File.separator + jarName + File.separator + jarEntryNameForJarFile);
                        FileUtils.copyInputStreamToFile(input, file);
                        final var manifestEntries = Alpaca.scanManifestEntry(productName, productVersion, file.toPath());
                        manifests.addAll(manifestEntries);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            String jarName = jarPathToFile.getName();
            if (!scanMainJarManifestFinished) {
                manifestEntry = new ManifestEntry(productName, productVersion, jarName, jarName.replaceAll(".jar", ""), UNKNOWN, UNKNOWN, jarPathToFile.getAbsolutePath());
                manifests.add(manifestEntry);
            }
            e.printStackTrace();
        }
        return manifests;
    }

    private static boolean checkManifestEntry(ManifestEntry manifestEntry) {
        boolean validFlag = false;

        final var jarArtifactId = manifestEntry.getJarArtifactId();
        if (jarArtifactId != null && !jarArtifactId.contains("$")) {
            validFlag = true;
        }

        final var jarPomName = manifestEntry.getJarPomName();
        if (jarPomName != null && !jarPomName.contains("$")) {
            validFlag = true;
        }
        return validFlag;
    }

    private static int countPOMFiletoCheckUberJar(JarFile jarFile) {
        // Get pom.xml files in META-INF/maven/
        int pomCountInJarFile = 0;
        final Enumeration<JarEntry> entriesForPomCount = jarFile.entries();
        while (entriesForPomCount.hasMoreElements()) {
            final JarEntry jarEntry = entriesForPomCount.nextElement();
            final String jarEntryName = jarEntry.getName();
            if (jarEntryName.matches("META-INF/maven/.+/pom.xml")) {
                pomCountInJarFile++;
            }
        }
        return pomCountInJarFile;
    }

    private static JarEntry getBuildMetadataFile(JarFile jarFile) {
        Enumeration<JarEntry> entriesForBuildMetadataCount = jarFile.entries();
        while (entriesForBuildMetadataCount.hasMoreElements()) {
            JarEntry jarEntry = entriesForBuildMetadataCount.nextElement();
            String jarEntryName = jarEntry.getName();
            if (jarEntryName.matches(buildMetadata)) {
                return jarEntry;
            }
        }
        return null;
    }

    public static Set<String> scanClasses(Path jarFilePath) {
        Set<String> classes = Collections.synchronizedSet(Sets.newHashSet());
        return classes;
    }

    /**
     * Make an entry by looking up META-INF/MANIFEST.MF
     */
    private static ManifestEntry getEntryFromJarManifest(final String productName, final String productVersion,
                                                         final String jarName, final Manifest manifest) {
        if (manifest != null) {
            Attributes attributes = manifest.getMainAttributes();
            String jarArtifactId = attributes.getValue("Bundle-Name");
            if (jarArtifactId == null || "".equals(jarArtifactId) || jarArtifactId.contains("%")) {
                jarArtifactId = attributes.getValue("Implementation-Title");
                if (jarArtifactId == null || "".equals(jarArtifactId)) {
                    jarArtifactId = attributes.getValue("Bundle-SymbolicName");
                }
            }
            String jarVersion = attributes.getValue("Implementation-Version");
            if (jarVersion == null || "".equals(jarVersion)) {
                jarVersion = attributes.getValue("Bundle-Version");
            }

            return new ManifestEntry(productName, productVersion, jarName, jarArtifactId, jarVersion, jarArtifactId);
        }
        return null;
    }
}
