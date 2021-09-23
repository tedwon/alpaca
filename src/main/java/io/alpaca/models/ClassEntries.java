package io.alpaca.models;

import com.google.common.collect.Sets;

import java.io.Serializable;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class ClassEntries implements Serializable {

    private static final long serialVersionUID = 7429702703421340836L;

    private String fqcn;

    private Set<ManifestEntry> manifests = Collections.synchronizedSet(Sets.newHashSet());

    public Set<ManifestEntry> getManifests() {
        return manifests;
    }

    public ClassEntries() {
    }

    public ClassEntries(String fqcn, Set<ManifestEntry> manifests) {
        this.fqcn = fqcn;
        this.manifests = manifests;
    }

    public void setManifests(Set<ManifestEntry> manifests) {
        this.manifests = manifests;
    }

    public String getFqcn() {
        return fqcn;
    }

    public void setFqcn(String fqcn) {
        this.fqcn = fqcn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassEntries that = (ClassEntries) o;
        return Objects.equals(fqcn, that.fqcn) && Objects.equals(manifests, that.manifests);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fqcn, manifests);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ClassEntries{");
        sb.append("fullyQualifiedClassName='").append(fqcn).append('\'');
        sb.append(", manifests=").append(manifests);
        sb.append('}');
        return sb.toString();
    }
}
