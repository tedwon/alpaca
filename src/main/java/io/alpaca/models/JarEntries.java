package io.alpaca.models;

import com.google.common.collect.Sets;

import java.io.Serializable;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class JarEntries implements Serializable {

    private static final long serialVersionUID = 7079022923389793004L;

    private String jar;

    private Set<ManifestEntry> manifests = Collections.synchronizedSet(Sets.newHashSet());

    public JarEntries() {
    }

    public JarEntries(String jar, Set<ManifestEntry> manifests) {
        this.jar = jar;
        this.manifests = manifests;
    }

    public Set<ManifestEntry> getManifests() {
        return manifests;
    }

    public void setManifests(Set<ManifestEntry> manifests) {
        this.manifests = manifests;
    }

    public String getJar() {
        return jar;
    }

    public void setJar(String jar) {
        this.jar = jar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JarEntries that = (JarEntries) o;
        return Objects.equals(jar, that.jar) && Objects.equals(manifests, that.manifests);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jar, manifests);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("JarEntries{");
        sb.append("jarFileName='").append(jar).append('\'');
        sb.append(", manifestEntrySet=").append(manifests);
        sb.append('}');
        return sb.toString();
    }
}
