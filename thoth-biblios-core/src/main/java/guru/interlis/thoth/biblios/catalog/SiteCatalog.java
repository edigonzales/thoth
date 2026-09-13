package guru.interlis.thoth.biblios.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Central catalog of all documentation components and their versions.
 */
public final class SiteCatalog {
    private final List<DocComponent> components;

    public SiteCatalog(List<DocComponent> components) {
        Objects.requireNonNull(components, "components are required");
        this.components = List.copyOf(components);
    }

    public List<DocComponent> components() {
        return components;
    }

    /**
     * Find a component by its ID.
     */
    public DocComponent findById(String componentId) {
        for (DocComponent c : components) {
            if (c.id().equals(componentId)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Get a specific component version.
     */
    public ComponentVersion getVersion(String componentId, String version) {
        DocComponent component = findById(componentId);
        if (component == null) {
            return null;
        }
        return component.getVersion(version);
    }

    /**
     * Return a new catalog with the given component replaced by ID.
     * If no component with the same ID exists yet, it is appended.
     */
    public SiteCatalog withReplacedComponent(DocComponent component) {
        Objects.requireNonNull(component, "component is required");
        List<DocComponent> updated = new ArrayList<>(components.size() + 1);
        boolean replaced = false;
        for (DocComponent existing : components) {
            if (existing.id().equals(component.id())) {
                updated.add(component);
                replaced = true;
            } else {
                updated.add(existing);
            }
        }
        if (!replaced) {
            updated.add(component);
        }
        return new SiteCatalog(updated);
    }

    @Override
    public String toString() {
        return "SiteCatalog{components=" + components.size() + "}";
    }
}
