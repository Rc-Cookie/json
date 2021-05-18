/**
 * A library used to work with JSON files. The parsing is handled
 * by the {@link com.github.rccookie.json.Json Json} class, it can
 * handle json strings with proper JSON formatting and additionally
 * single line and multiline comments and trailing commas.
 *
 * <p>JSON objects and arrays are represented by the classes
 * {@link com.github.rccookie.json.JsonObject JsonObject} and
 * {@link com.github.rccookie.json.JsonArray JsonArray}, respectively.
 * Each of these classes has utilities to easily read and write JSON
 * from and to files.
 *
 * <p>Additionally, the class
 * {@link com.github.rccookie.json.JsonElement JsonElement} functions
 * as a wrapper class for values in JSON similar to
 * {@link java.util.Optional Optional} to simplify the process of
 * performing many steps to check weather a value is present by checking
 * each outer object / array for existence.
 */
package com.github.rccookie.json;
