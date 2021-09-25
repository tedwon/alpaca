package io.alpaca;

import com.google.common.collect.Sets;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class Utils {

    private static final Logger LOG = Logger.getLogger(Utils.class);

    public static final String UNKNOWN = "Unknown";
    public static final String ZIP = ".zip";
    public static final String ADM = ".adm";
    public static final String JAR = ".jar";
    public static final String POM = ".pom";
    public static final String WAR = ".war";
    public static final String EAR = ".ear";
    public static final String RAR = ".rar";
    public static final String HPI = ".hpi";
    public static final String TAR = ".tar";
    public static final String TAR_GZ = ".tar.gz";
    public static final String TGZ = ".tgz";

    public static final String TXT = ".txt";
    public static final String XML = ".xml";
    public static final String WIN6 = ".win6";

    public static final String JAR_ARCHIVE = "application/x-java-archive";
    public static final String RAR_ARCHIVE = "application/vnd.rar"; // rar
    public static final String ZIP_ARCHIVE = "application/zip";
    public static final String GZ_ARCHIVE = "application/gzip"; // gz
    public static final String GZIP_ARCHIVE = " => application/gzip"; // tar.gz tgz
    public static final String TAR_ARCHIVE = "application/x-tar"; // tar

    private static final Set<String> javaArchiveFormats = Sets.newHashSet();
    private static final Set<String> archiveFormats = Sets.newHashSet();
    private static final Set<String> blackListSet = Sets.newHashSet();

    static {
        javaArchiveFormats.add(JAR_ARCHIVE);

        archiveFormats.add(RAR_ARCHIVE);
        archiveFormats.add(ZIP_ARCHIVE);
        archiveFormats.add(GZ_ARCHIVE);
        archiveFormats.add(GZIP_ARCHIVE);
        archiveFormats.add(TAR_ARCHIVE);

        blackListSet.add("java");
    }

    public static boolean isValidManifestTargetFormat(Path path) {
        final var fileExtension = com.google.common.io.Files.getFileExtension(path.toString());
        if (!blackListSet.contains(fileExtension)) {
            return true;
        }
        return false;
    }

    public static boolean isJavaArchive(Path path) {
        try {
            final var contentType = Files.probeContentType(path);
            if (javaArchiveFormats.contains(contentType)) {
                return true;
            }
        } catch (IOException e) {
        }
        return false;
    }
    public static boolean isArchive(Path path) {
        try {
            final var contentType = Files.probeContentType(path);
            if (archiveFormats.contains(contentType)) {
                return true;
            }
        } catch (IOException e) {
        }
        return false;
    }

    public static Set<String> decompressArchive(final Path zipFile, final String targetUnzipDir) {
        final Set<String> unzippedFileEntrySet = Sets.newConcurrentHashSet();
        // Unzip .zip or .adm files
        final String zipFileStr = zipFile.getFileName().toString();
        if (zipFileStr.endsWith(ZIP) || zipFileStr.endsWith(JAR) || zipFileStr.endsWith(ADM)) {
            unzippedFileEntrySet.addAll(unzip(targetUnzipDir, zipFile));
        } else if (zipFileStr.endsWith(TAR)) {
            unzippedFileEntrySet.addAll(decompressTarFile(targetUnzipDir, zipFile));
        } else if (zipFileStr.endsWith(TAR_GZ) || zipFileStr.endsWith(TGZ)) {
            unzippedFileEntrySet.addAll(decompressTarGzFile(targetUnzipDir, zipFile));
        } else {
            LOG.infof("Found unknown compressed file: %s", zipFile);
        }

        // delete zipFile after decompression to save disk usage
//        try {
//            FileUtils.forceDelete(zipFile.toFile());
////            LOG.infof("Deleted zip file: %s", zipFile);
//        } catch (IOException e) {
//            LOG.errorf(e, "Exception occurred while deleting %s\n", zipFile);
//        }

        return unzippedFileEntrySet;
    }

    public static Set<String> unzip(final String targetUnZipDir, final Path zipFile) {
//        LOG.infof("Unzipping %s to %s", zipFile, targetUnZipDir);

        final Set<String> unzippedFileEntrySet = Sets.newConcurrentHashSet();

        // Unzip
        try (InputStream fi = Files.newInputStream(zipFile);
             InputStream bi = new BufferedInputStream(fi);
             ArchiveInputStream i = new ZipArchiveInputStream(bi)) {
            final String zipFileName = zipFile.toFile().getName();
            ArchiveEntry entry;
            while ((entry = i.getNextEntry()) != null) {
                if (!i.canReadEntryData(entry)) {
                    // log something?
                    continue;
                }

                Path unzippedFileEntry = fileName(targetUnZipDir, zipFileName, entry);
                File f = unzippedFileEntry.toFile();
                if (entry.isDirectory()) {
                    final var isDirectory = !f.isDirectory();
                    final var mkdirs = !f.mkdirs();
                    if (isDirectory && mkdirs) {
//                        throw new IOException("failed to create directory " + f);
                    }
                } else {
                    File parent = f.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
//                        throw new IOException("failed to create directory " + parent);
                    }
                    try (OutputStream o = Files.newOutputStream(f.toPath())) {
                        IOUtils.copy(i, o);
                    }
                }

                // Add only file, not directory
                if (Files.isRegularFile(unzippedFileEntry)) {
                    unzippedFileEntrySet.add(unzippedFileEntry.toString());
                }
            }
        } catch (Exception e) {
            LOG.errorf(e, "Exception occurred while unzip %s\n", zipFile);
        }
//        LOG.infof("Unzipped %s entries from %s to %s", unzippedFileEntrySet.size(), zipFile, targetUnZipDir);
        return unzippedFileEntrySet;
    }

    public static Set<String> decompressTarGzFile(final String targetUnZipDir, final Path zipFile) {
        LOG.infof("Decompressing tar.gz file %s to %s", zipFile, targetUnZipDir);

        final Set<String> unzippedFileEntrySet = Sets.newConcurrentHashSet();

        // Unzip
        try (InputStream fi = Files.newInputStream(zipFile);
             BufferedInputStream bi = new BufferedInputStream(fi);
             GzipCompressorInputStream gzi = new GzipCompressorInputStream(bi);
             TarArchiveInputStream i = new TarArchiveInputStream(gzi)) {
            final String zipFileName = zipFile.toFile().getName();
            ArchiveEntry entry;
            while ((entry = i.getNextEntry()) != null) {
                if (!i.canReadEntryData(entry)) {
                    // log something?
                    continue;
                }

                Path unzippedFileEntry = fileName(targetUnZipDir, zipFileName, entry);
                File f = unzippedFileEntry.toFile();
                if (entry.isDirectory()) {
                    if (!f.isDirectory() && !f.mkdirs()) {
//                        throw new IOException("failed to create directory " + f);
                    }
                } else {
                    File parent = f.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
//                        throw new IOException("failed to create directory " + parent);
                    }
                    try (OutputStream o = Files.newOutputStream(f.toPath())) {
                        IOUtils.copy(i, o);
                    }
                }

                // Add only file, not directory
                if (Files.isRegularFile(unzippedFileEntry)) {
                    unzippedFileEntrySet.add(unzippedFileEntry.toString());
                }
            }
        } catch (Exception e) {
            LOG.errorf(e, "Exception occurred while decompressing tar.gz file %s\n", zipFile);
        }
        LOG.infof("Decompressed tar.gz %s entries from %s to %s", unzippedFileEntrySet.size(), zipFile, targetUnZipDir);
        return unzippedFileEntrySet;
    }

    public static Set<String> decompressTarFile(final String targetUnZipDir, final Path zipFile) {
        LOG.infof("Decompress tar file %s to %s", zipFile, targetUnZipDir);

        final Set<String> unzippedFileEntrySet = Sets.newConcurrentHashSet();

        // Unzip
        try (InputStream fi = Files.newInputStream(zipFile);
             BufferedInputStream bi = new BufferedInputStream(fi);
             TarArchiveInputStream i = new TarArchiveInputStream(bi)) {
            final String zipFileName = zipFile.toFile().getName();
            ArchiveEntry entry;
            while ((entry = i.getNextEntry()) != null) {
                if (!i.canReadEntryData(entry)) {
                    // log something?
                    continue;
                }

                Path unzippedFileEntry = fileName(targetUnZipDir, zipFileName, entry);
                File f = unzippedFileEntry.toFile();
                if (entry.isDirectory()) {
                    if (!f.isDirectory() && !f.mkdirs()) {
//                        throw new IOException("failed to create directory " + f);
                    }
                } else {
                    File parent = f.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
//                        throw new IOException("failed to create directory " + parent);
                    }
                    try (OutputStream o = Files.newOutputStream(f.toPath())) {
                        IOUtils.copy(i, o);
                    }
                }

                // Add only file, not directory
                if (Files.isRegularFile(unzippedFileEntry)) {
                    unzippedFileEntrySet.add(unzippedFileEntry.toString());
                }
            }
        } catch (Exception e) {
            LOG.errorf(e, "Exception occurred while decompressing tar file %s\n", zipFile);
        }
        LOG.infof("Decompressed tar %s entries from %s to %s", unzippedFileEntrySet.size(), zipFile, targetUnZipDir);
        return unzippedFileEntrySet;
    }

    public static Path fileName(final String targetDir, final String zipFileName, final ArchiveEntry entry) {
        return Path.of(targetDir + File.separator + zipFileName + File.separator + entry.getName());
    }
}
