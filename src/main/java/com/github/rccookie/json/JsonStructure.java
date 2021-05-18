package com.github.rccookie.json;

import java.io.File;

/**
 * Superclass of {@link JsonObject} and {@link JsonArray}.
 */
public interface JsonStructure {

    boolean load(File file);

    boolean store(File file);
}
