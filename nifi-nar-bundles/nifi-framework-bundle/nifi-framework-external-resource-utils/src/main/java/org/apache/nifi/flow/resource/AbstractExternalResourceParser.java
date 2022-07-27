package org.apache.nifi.flow.resource;

public abstract class AbstractExternalResourceParser<T> implements ExternalResourceParser {
    protected abstract boolean isDirectory(final T node);
    protected abstract ImmutableExternalResourceDescriptor createDescriptor(final T node, final String url, final boolean isDirectory);
    protected abstract String parseLocation(final T node);
    protected abstract long parseLastModified(final T node);
}
