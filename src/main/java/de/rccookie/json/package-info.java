/**
 * A library to work with JSON files. The parsing is handled
 * by the {@link de.rccookie.json.Json Json} class.
 *
 * <p>JSON objects and arrays are represented by the classes
 * {@link de.rccookie.json.JsonObject JsonObject} and
 * {@link de.rccookie.json.JsonArray JsonArray}, respectively.
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
 *     {@link de.rccookie.json.Parse} class)
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
 * {@link de.rccookie.json.JsonElement JsonElement} functions
 * as a wrapper class for values in JSON similar to
 * {@link java.util.Optional Optional} to simplify the process of
 * performing many steps to check weather a value is present by checking
 * each object / array for existence.
 */
package de.rccookie.json;
