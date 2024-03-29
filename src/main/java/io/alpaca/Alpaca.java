package io.alpaca;

import com.google.common.collect.Sets;
import io.alpaca.models.ManifestEntry;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static io.alpaca.Utils.decompressArchive;
import static io.alpaca.Utils.isArchive;
import static io.alpaca.Utils.isJavaArchive;

/**
 * Generate manifest for an archive file or directory.
 * <p/>
 * See: https://github.com/tedwon/alpaca
 */
public class Alpaca {

    private static final Logger LOG = Logger.getLogger(Alpaca.class);

    public static final String buildMetadata = "META-INF/build.metadata";

    public static final String UNKNOWN = "Unknown";

    public static final String COMMA_SEPARATE = ",";

    public static Set<ManifestEntry> scanManifestEntry(final Path jarFilePath) {
        return scanManifestEntry("", "", jarFilePath);
    }

    public static Set<ManifestEntry> scanManifestEntry(final String productName, final String productVersion, final Path jarFilePath) {
        final Set<ManifestEntry> manifests = Collections.synchronizedSet(Sets.newHashSet());

        final String tmpDir = System.getProperty("java.io.tmpdir") + File.separator + "alpaca" + File.separator + ProcessHandle.current().pid() + File.separator + UUID.randomUUID() + File.separator;

        // Check if the input path is a directory?
        if (Files.isDirectory(jarFilePath) && !Files.isRegularFile(jarFilePath)) {
            // input path is a directory
            // Get a file list in the directory
            try (Stream<Path> stream = Files.walk(jarFilePath)) {
                stream.parallel()
                        .filter(Files::isRegularFile)
                        .filter(file -> !Pattern.compile(Pattern.quote("/\\.git/"), Pattern.CASE_INSENSITIVE).matcher(file.toString()).find())
                        .forEach(file -> {
                            final var manifestEntries = Alpaca.scanManifestEntry(productName, productVersion, file);
                            manifests.addAll(manifestEntries);
                        });
            } catch (Exception e) {
                LOG.errorf(e, "Exception occurred while list up files in %s\n", jarFilePath);
            }
        } else {
            // Check if the file is an archive?
            if (isArchive(jarFilePath)) {
                // Decompress the archive
                final var targetUnzipDir = tmpDir + jarFilePath;
                // Call this method recursively for the archive
                decompressArchive(jarFilePath, targetUnzipDir).stream()
                        .parallel()
                        .forEach(archiveEntry -> {
                            manifests.addAll(Alpaca.scanManifestEntry(productName, productVersion, Paths.get(archiveEntry)));
                        });
            } else if (isJavaArchive(jarFilePath)) {
                boolean scanMainJarManifestFinished = false;
                ManifestEntry manifestEntry = null;

                final var jarPathToFile = jarFilePath.toFile();
                final var jarAbsolutePath = jarPathToFile.getAbsolutePath();
                final var jarFileName = jarPathToFile.getName();

                try (final JarFile jarFile = new JarFile(jarPathToFile)) {
                    // Decompress the archive && Manifest bundled jars inside the input jar file
                    final var targetUnzipDir = tmpDir + jarFilePath;
                    decompressArchive(jarFilePath, targetUnzipDir).stream()
                            .parallel()
                            .forEach(archiveEntry -> {
                                manifests.addAll(Alpaca.scanManifestEntry(productName, productVersion, Paths.get(archiveEntry)));
                            });

                    // check if the jar file has META-INF/MANIFEST
                    Manifest manifest = jarFile.getManifest();
                    if (manifest != null) {
                        // read MANIFEST
                        manifestEntry = getEntryFromJarManifest(productName, productVersion, jarFileName, manifest);
                        if (manifestEntry != null) {
                            manifestEntry.setPath(jarAbsolutePath);
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

                        // build.groupId -> commons-httpclient
                        final String groupId = prop.getProperty("build.groupId");

                        // build.artifactId=camel-archetype-activemq
                        final String artifactId = prop.getProperty("build.artifactId");

                        final String pomName = artifactId;

                        // build.version=2.23.2.fuse-780036-redhat-00001
                        // build.version.full -> 3.1.0.redhat-8
                        final String version = prop.getProperty("build.version");

                        if (artifactId != null && version != null) {
                            manifestEntry = new ManifestEntry(productName, productVersion, groupId, artifactId, version, jarFileName, pomName, jarAbsolutePath, null);
                            scanMainJarManifestFinished = checkManifestEntry(manifestEntry);
                        }
                    }

                    // check whether it is uberjar or not
                    int pomCountInJarFile = countPOMFiletoCheckUberJar(jarFile);
                    if (pomCountInJarFile >= 2) {
                        // to exclusive the main jar's pom.xml file from bundledjar entries
                        String jarsMainPOMFile = null;

                        // if it is uberjar
                        // find pom.xml of the jar file among all pom.xml files including bundled jars in the file
                        // read pom.xml
                        Enumeration<JarEntry> entries = jarFile.entries();
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
                                if (jarsMainPOMFile == null || jarsMainPOMFile.length() < artifactName.length()) {
                                    if (jarFileName.contains(artifactName)) {
                                        // to exclusive the main jar's pom.xml file from bundledjar entries
                                        jarsMainPOMFile = jarEntryNameForPOMFile;
                                        String groupId = model.getGroupId();
                                        if (groupId != null && model.getParent() != null) {
                                            groupId = model.getParent().getGroupId();
                                        }
                                        if (groupId == null) {
                                            groupId = manifestEntry.getGroupId();
                                        }
                                        String artifactId = model.getArtifactId();
                                        String pomName = model.getName();
                                        if (artifactId != null && version != null) {
                                            if (pomName == null || pomName.contains("${")) {
                                                pomName = artifactId;
                                            }
                                            manifestEntry = new ManifestEntry(productName, productVersion, groupId, artifactId, version, jarFileName, pomName, jarAbsolutePath, null);
                                        }
                                    }
                                }
                            }
                        }

                        // create manifest entry for the bundled jar inside of the jar file
                        List<JarEntry> pomList = new ArrayList<>();
                        entries = jarFile.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry jarEntryForPOMFile = entries.nextElement();
                            String jarEntryNameForPOMFile = jarEntryForPOMFile.getName();
                            if (jarEntryNameForPOMFile.matches("META-INF/maven/.+/pom.xml")) {
                                if (!jarEntryNameForPOMFile.equals(jarsMainPOMFile)) {
                                    // to exclusive the main jar's pom.xml file from bundledjar entries
                                    pomList.add(jarEntryForPOMFile);
                                }
                            }
                        }
                        StringBuffer output = new StringBuffer();
                        for (int i = 0; i < pomList.size(); i++) {
                            final JarEntry jarEntryForBundledJar = pomList.get(i);
                            String jarEntryName = jarEntryForBundledJar.getName();
                            String jarVersion = "";
                            try {
                                if (jarEntryName.matches("META-INF/maven/.*/pom.xml")) {
                                    Model model = new MavenXpp3Reader().read(jarFile.getInputStream(jarEntryForBundledJar));
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
                                output.append(COMMA_SEPARATE);
                            }
                        }
                        manifestEntry.setBundles(output.toString());
                        scanMainJarManifestFinished = checkManifestEntry(manifestEntry);
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

                                    String groupId = model.getGroupId();
                                    if (groupId == null && model.getParent() != null) {
                                        groupId = model.getParent().getGroupId();
                                    }
                                    if (groupId == null) {
                                        groupId = manifestEntry.getGroupId();
                                    }
                                    String artifactId = model.getArtifactId();
                                    String version = model.getVersion();
                                    if (version == null) {
                                        version = model.getParent().getVersion();
                                    }
                                    String pomName = model.getName();
                                    if (artifactId != null && version != null) {
                                        if (pomName == null || pomName.contains("${")) {
                                            pomName = artifactId;
                                        }
                                        manifestEntry = new ManifestEntry(productName, productVersion, groupId, artifactId, version, jarFileName, pomName, jarAbsolutePath, null);
                                        scanMainJarManifestFinished = checkManifestEntry(manifestEntry);
                                    }
                                }
                            }
                        }
                    } else {
                        // pom.xml not found in the jar file
                        if (manifestEntry == null & !scanMainJarManifestFinished) {
                            manifestEntry = new ManifestEntry(productName, productVersion, UNKNOWN, UNKNOWN, UNKNOWN, jarFileName, UNKNOWN, jarAbsolutePath, null);
                            scanMainJarManifestFinished = checkManifestEntry(manifestEntry);
                        } else {
                            scanMainJarManifestFinished = true;
                        }
                    }
                } catch (Exception e) {
                    // java.util.zip.ZipException: zip END header not found /tmp/koala/downloads/fuse/7.8.0/unzip/redhat-fuse-7.8.0-sources.zip/fuse-karaf-7.8.0.fuse-780038-redhat-00001/modules/fuse-patch/patch-management/src/test/resources/content/patch9/system/org/jboss/fuse/fuse-tranquility/1.2.4/fuse-tranquility-1.2.4.jar
                    if (!scanMainJarManifestFinished) {
                        manifestEntry = new ManifestEntry(productName, productVersion, UNKNOWN, UNKNOWN, UNKNOWN, jarFileName, UNKNOWN, jarAbsolutePath, null);
                        scanMainJarManifestFinished = checkManifestEntry(manifestEntry);
                    }
//                LOG.errorf(e, "Exception occurred while processing %s\n", jarPathToFile);
                }

                if (scanMainJarManifestFinished) {
                    if (manifestEntry.getPath() == null) {
                        System.out.println();
                    }
                    manifests.add(manifestEntry);
                } else {
                    LOG.warnf("Failed to generate manifest from %s", jarPathToFile);
                }
            }
        }
        // Clean up decompressed dir
        try {
            FileUtils.deleteQuietly(Paths.get(tmpDir).toFile());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return manifests;
    }

    private static boolean checkManifestEntry(ManifestEntry manifestEntry) {
        boolean validFlag = false;

        final var jarPomName = manifestEntry.getPomName();
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
                                                         final String jarFileName, final Manifest manifest) {
        if (manifest != null) {
            Attributes attributes = manifest.getMainAttributes();

            // groupId
            // org.apache.taglibs.taglibs-standard-spec;singleton=true
            String groupId = null;
            final var implementationVendorId = attributes.getValue("Implementation-Vendor-Id");
            if (implementationVendorId != null && !implementationVendorId.contains(";") && !implementationVendorId.contains("=") && !implementationVendorId.contains("http://")) {
                groupId = implementationVendorId;
            }
            if (groupId == null || "".equals(groupId) || groupId.contains("%")) {
                groupId = attributes.getValue("Bundle-SymbolicName");
                if (groupId == null || "".equals(groupId)) {
                    groupId = attributes.getValue("Automatic-Module-Name");
                    if (groupId == null || "".equals(groupId)) {
                        groupId = UNKNOWN;
                    }
                }
            }

            // artifactId
            String artifactId = attributes.getValue("Implementation-Title");
            if (artifactId == null || "".equals(artifactId) || artifactId.contains("%") || artifactId.contains(" ")) {
                artifactId = attributes.getValue("Bundle-Name");
                if (artifactId == null || "".equals(artifactId) || artifactId.contains(" ")) {
                    artifactId = attributes.getValue("Specification-Title");
                    if (artifactId == null || artifactId.contains(" ") || artifactId.contains("JBoss")) {
                        if (attributes.getValue("Bundle-SymbolicName") != null) {
                            artifactId = attributes.getValue("Bundle-SymbolicName");
                        } else {
                            artifactId = UNKNOWN;
                        }
                    }
                }
            }

            // pom name
            // TODO: e.g. "Bundle-Description" -> Jansi is a java library for generating and interpreting ANSI escape sequences.
            String pomName = attributes.getValue("Bundle-Name");
            if (pomName == null || "".equals(pomName) || pomName.contains("%")) {
                pomName = attributes.getValue("Implementation-Title");
                if (pomName == null || "".equals(pomName)) {
                    pomName = attributes.getValue("Bundle-SymbolicName");
                    if (pomName == null) {
                        pomName = UNKNOWN;
                    }
                }
            }
            String version = attributes.getValue("Implementation-Version");
            if (version == null || "null".equals(version) || "".equals(version)) {
                version = attributes.getValue("Bundle-Version");
                if (version == null) {
                    version = UNKNOWN;
                }
            }

            return new ManifestEntry(productName, productVersion, groupId, artifactId, version, jarFileName, pomName, null, null);
        }
        return null;
    }
}
