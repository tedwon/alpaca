package io.alpaca.models;

import java.io.Serializable;
import java.util.Objects;

public class ManifestEntry implements Serializable {

    private static final long serialVersionUID = 3167639000206178577L;

    private String productName;

    private String productVersion;

    private String groupId;

    private String artifactId;

    private String version;

    private transient String pomName;

    private String jarFileName;

    private String path;

    private String bundles;

    public ManifestEntry() {
    }

    public ManifestEntry(String productName, String productVersion, String groupId, String artifactId, String version, String jarFileName, String pomName, String path, String bundles) {
        this.productName = productName;
        this.productVersion = productVersion;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.jarFileName = jarFileName;
        this.pomName = pomName;
        this.path = path;
        this.bundles = bundles;
    }

    public ManifestEntry(ManifestEntry manifestEntry) {
        this.productName = manifestEntry.getProductName();
        this.productVersion = manifestEntry.getProductVersion();
        this.groupId = manifestEntry.getGroupId();
        this.artifactId = manifestEntry.getArtifactId();
        this.version = manifestEntry.getVersion();
        this.pomName = manifestEntry.getPomName();
        this.jarFileName = manifestEntry.getJarFileName();
        this.path = manifestEntry.getPath();
        this.bundles = manifestEntry.getBundles();
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductVersion() {
        return productVersion;
    }

    public void setProductVersion(String productVersion) {
        this.productVersion = productVersion;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPomName() {
        return pomName;
    }

    public void setPomName(String pomName) {
        this.pomName = pomName;
    }

    public String getJarFileName() {
        return jarFileName;
    }

    public void setJarFileName(String jarFileName) {
        this.jarFileName = jarFileName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBundles() {
        return bundles;
    }

    public void setBundles(String bundles) {
        this.bundles = bundles;
    }

    public String toManifest() {
        final StringBuffer sb = new StringBuffer();
        sb.append(productName).append(":").append(productVersion)
                .append("/").append(pomName).append("/").append(version)
                .append("/").append(jarFileName);
        return sb.toString();
    }

    public String toManifestForUberJar() {
        final StringBuffer sb = new StringBuffer();
        sb.append(productName).append(":").append(productVersion)
                .append("/").append(pomName).append("/").append(version)
                .append("/").append(jarFileName);
        if (bundles != null && !"".equals(bundles)) {
            sb.append("/").append(bundles);
        }
        return sb.toString();
    }


    /**
     * GAV(Group:Artifact:Version)
     * <p/>
     * e.g. pkg:mvn/org.apache.camel/camel-barcode@2.23.2.fuse-780036-redhat-00001
     */
    public String toDeptopiaManifest() {
        final StringBuffer sb = new StringBuffer();
        sb.append("pkg:mvn/").append(groupId)
                .append("/").append(artifactId)
                .append("@").append(version)
                .append("/").append(jarFileName)
//                .append("/").append(path)
        ;
        return sb.toString();
    }

    public String toDeptopiaManifestForUberJar() {
        final StringBuffer sb = new StringBuffer(toDeptopiaManifest());
        if (bundles != null && !"".equals(bundles)) {
            sb.append("/").append(bundles);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(productName).append(":").append(productVersion)
                .append("/").append(pomName).append("/").append(version)
                .append("/").append(jarFileName).append("/").append(path);
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ManifestEntry that = (ManifestEntry) o;
        return Objects.equals(productName, that.productName) && Objects.equals(productVersion, that.productVersion) && Objects.equals(groupId, that.groupId) && Objects.equals(artifactId, that.artifactId) && Objects.equals(version, that.version) && Objects.equals(jarFileName, that.jarFileName) && Objects.equals(path, that.path) && Objects.equals(bundles, that.bundles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productName, productVersion, groupId, artifactId, version, jarFileName, path, bundles);
    }
}