package io.alpaca.models;

import java.io.Serializable;
import java.util.Objects;

public class ManifestEntry implements Serializable {

    private static final long serialVersionUID = 2138385057811617052L;

    private String productName;

    private String productVersion;

    private String jarName;

    private String jarArtifactId;

    private String jarVersion;

    private String jarPomName;

    private String jarFilePath;

    private String bundledJarName;

    public ManifestEntry() {
    }

    public ManifestEntry(String productName, String productVersion, String jarName, String jarArtifactId, String jarVersion, String jarPomName) {
        this.productName = productName;
        this.productVersion = productVersion;
        this.jarName = jarName;
        this.jarArtifactId = jarArtifactId;
        this.jarVersion = jarVersion;
        this.jarPomName = jarPomName;
    }

    public ManifestEntry(String productName, String productVersion, String jarName, String jarArtifactId, String jarVersion, String jarPomName, String jarFilePath) {
        this.productName = productName;
        this.productVersion = productVersion;
        this.jarName = jarName;
        this.jarArtifactId = jarArtifactId;
        this.jarVersion = jarVersion;
        this.jarPomName = jarPomName;
        this.jarFilePath = jarFilePath;
    }

    public ManifestEntry(String productName, String productVersion, String jarName, String jarArtifactId, String jarVersion, String jarPomName, String jarFilePath, String bundledJarName) {
        this.productName = productName;
        this.productVersion = productVersion;
        this.jarArtifactId = jarArtifactId;
        this.jarName = jarName;
        this.jarVersion = jarVersion;
        this.jarPomName = jarPomName;
        this.jarFilePath = jarFilePath;
        this.bundledJarName = bundledJarName;
    }

    public ManifestEntry(ManifestEntry manifestEntry) {
        this.productName = manifestEntry.getProductName();
        this.productVersion = manifestEntry.getProductVersion();
        this.jarArtifactId = manifestEntry.getJarArtifactId();
        this.jarName = manifestEntry.getJarName();
        this.jarVersion = manifestEntry.getJarVersion();
        this.jarPomName = manifestEntry.getJarPomName();
        this.jarFilePath = manifestEntry.getJarFilePath();
        this.bundledJarName = manifestEntry.getBundledJarName();
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

    public String getJarName() {
        return jarName;
    }

    public void setJarName(String jarName) {
        this.jarName = jarName;
    }

    public String getJarArtifactId() {
        return jarArtifactId;
    }

    public void setJarArtifactId(String jarArtifactId) {
        this.jarArtifactId = jarArtifactId;
    }

    public String getJarVersion() {
        return jarVersion;
    }

    public void setJarVersion(String jarVersion) {
        this.jarVersion = jarVersion;
    }

    public String getJarPomName() {
        return jarPomName;
    }

    public void setJarPomName(String jarPomName) {
        this.jarPomName = jarPomName;
    }

    public String getJarFilePath() {
        return jarFilePath;
    }

    public void setJarFilePath(String jarFilePath) {
        this.jarFilePath = jarFilePath;
    }

    public String getBundledJarName() {
        return bundledJarName;
    }

    public void setBundledJarName(String bundledJarName) {
        this.bundledJarName = bundledJarName;
    }

    public String toManifest() {
        final StringBuffer sb = new StringBuffer();
        sb.append(productName).append(":").append(productVersion)
                .append("/").append(jarPomName).append("/").append(jarVersion)
                .append("/").append(jarName);
        return sb.toString();
    }

    public String toManifestForUberJar() {
        final StringBuffer sb = new StringBuffer();
        sb.append(productName).append(":").append(productVersion)
                .append("/").append(jarPomName).append("/").append(jarVersion)
                .append("/").append(jarName);
        if (bundledJarName != null && !"".equals(bundledJarName)) {
            sb.append("/").append(bundledJarName);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(productName).append(":").append(productVersion)
                .append("/").append(jarPomName).append("/").append(jarVersion)
                .append("/").append(jarName).append("/").append(jarFilePath);
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ManifestEntry that = (ManifestEntry) o;
        return Objects.equals(productVersion, that.productVersion) && Objects.equals(jarName, that.jarName) && Objects.equals(jarArtifactId, that.jarArtifactId) && Objects.equals(jarVersion, that.jarVersion) && Objects.equals(jarPomName, that.jarPomName) && Objects.equals(jarFilePath, that.jarFilePath) && Objects.equals(bundledJarName, that.bundledJarName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productVersion, jarName, jarArtifactId, jarVersion, jarPomName, jarFilePath, bundledJarName);
    }
}
