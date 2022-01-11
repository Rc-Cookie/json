/**
 * A library to work with JSON files. The parsing is handled
 * by the {@link com.github.rccookie.json.Json Json} class.
 *
 * <p>JSON objects and arrays are represented by the classes
 * {@link com.github.rccookie.json.JsonObject JsonObject} and
 * {@link com.github.rccookie.json.JsonArray JsonArray}, respectively.
 * Each of these classes has utilities to easily read and write JSON
 * from and to files.
 *
 * <p><p><h2>Supported syntax</h2>
 * <li>Objects, arrays, numbers, strings, booleans, null
 * <li>Any whitespace/newline formatting
 * <li>Single and multiline comments
 * <li>Character escape sequences - parsing and writing
 * <li>Any top level type
 * <li>Single quotes as double quotes (only in
 *     {@link com.github.rccookie.json.Parse} class)
 *
 * <p><p><h2>Path syntax</h2>
 * <p>Paths describe the path to an element in a json structure.
 * They have two ways of describing the required element:
 * <li>{@code [<key or index>]}
 * <li>{@code .<key or index>}
 * <p>It is recommended to use the brackets only for array indices and
 * the dot only for keys, but it is valid to do it any way. The first
 * dot must be omitted.
 *
 * <p>Additionally, the class
 * {@link com.github.rccookie.json.JsonElement JsonElement} functions
 * as a wrapper class for values in JSON similar to
 * {@link java.util.Optional Optional} to simplify the process of
 * performing many steps to check weather a value is present by checking
 * each object / array for existence.
 */
package com.github.rccookie.json;
