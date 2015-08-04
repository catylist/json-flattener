/**
 *
 * @author Wei-Ming Wu
 *
 *
 * Copyright 2015 Wei-Ming Wu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package com.github.wnameless.json.flattener;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.eclipsesource.json.JsonValue;

/**
 * 
 * {@link JsonFlattener} flatten any JSON nested objects or arrays into a simple
 * Map{@literal <Stirng, Object>}. The String key will represents the
 * corresponding position of value in the original nested objects or arrays and
 * the Object value are either String, Boolean, Number or null. <br>
 * <br>
 * For example: <br>
 * { "a" : { "b" : 1, "c": null, "d": [false, true] }, "e": "f", "g":2.3 }<br>
 * will be turned into <br>
 * {<br>
 * a.b=1,<br>
 * a.c=null,<br>
 * a.d[0]=false,<br>
 * a.d[1]=true,<br>
 * e=f<br>
 * g=2.3<br>
 * }
 *
 */
public final class JsonFlattener {

  /**
   * Returns a flattened JSON string.
   * 
   * @param json
   *          the JSON string
   * @return a flattened JSON string.
   */
  public static String flatten(String json) {
    return new JsonFlattener(json).flatten();
  }

  /**
   * Returns a flattened JSON as Map.
   * 
   * @param json
   *          the JSON string
   * @return a flattened JSON as Map
   */
  public static Map<String, Object> flattenAsMap(String json) {
    return new JsonFlattener(json).flattenAsMap();
  }

  private final LinkedList<IndexedPeekIterator<?>> elementIters =
      new LinkedList<IndexedPeekIterator<?>>();
  private final Map<String, Object> flattenedJsonMap =
      new LinkedHashMap<String, Object>();
  private String flattenedJson = null;

  /**
   * Creates a JSON flattener.
   * 
   * @param json
   *          the JSON string
   */
  public JsonFlattener(String json) {
    JsonValue source = Json.parse(json);
    if (!source.isObject() && !source.isArray())
      throw new IllegalArgumentException(
          "Input must be a JSON object or array");

    reduce(source);
  }

  /**
   * Returns a flattened JSON as Map.
   * 
   * @return a flattened JSON as Map
   */
  public Map<String, Object> flattenAsMap() {
    while (!elementIters.isEmpty()) {
      if (!elementIters.getLast().hasNext()) {
        elementIters.removeLast();
      } else if (elementIters.getLast().peek() instanceof Member) {
        Member mem = (Member) elementIters.getLast().next();
        reduce(mem.getValue());
      } else if (elementIters.getLast().peek() instanceof JsonValue) {
        JsonValue val = (JsonValue) elementIters.getLast().next();
        reduce(val);
      }
    }

    return flattenedJsonMap;
  }

  /**
   * Returns a flattened JSON string.
   * 
   * @return a flattened JSON string
   */
  public String flatten() {
    if (flattenedJson != null) return flattenedJson;

    flattenAsMap();

    JsonObject jsonObj = Json.object();
    for (Entry<String, Object> mem : flattenedJsonMap.entrySet()) {
      if (mem.getValue() instanceof Boolean) {
        jsonObj.add(mem.getKey(), (Boolean) mem.getValue());
      } else if (mem.getValue() instanceof String) {
        jsonObj.add(mem.getKey(), (String) mem.getValue());
      } else if (mem.getValue() instanceof Number) {
        boolean isInteger = mem.getValue() instanceof Long;
        jsonObj.add(mem.getKey(),
            isInteger ? (Long) mem.getValue() : (Double) mem.getValue());
      } else {
        jsonObj.add(mem.getKey(), Json.NULL);
      }
    }

    return flattenedJson = jsonObj.toString();
  }

  private void reduce(JsonValue val) {
    if (val.isObject())
      elementIters
          .add(new IndexedPeekIterator<Member>(val.asObject().iterator()));
    else if (val.isArray())
      elementIters
          .add(new IndexedPeekIterator<JsonValue>(val.asArray().iterator()));
    else
      flattenedJsonMap.put(computeKey(), jsonVal2Obj(val));
  }

  private Object jsonVal2Obj(JsonValue jsonValue) {
    if (jsonValue.isBoolean()) return jsonValue.asBoolean();
    if (jsonValue.isString()) return jsonValue.asString();
    if (jsonValue.isNumber()) {
      double v = jsonValue.asDouble();
      if (!Double.isNaN(v) && !Double.isInfinite(v) && v == Math.rint(v))
        return jsonValue.asLong();
      else
        return jsonValue.asDouble();
    }

    return null;
  }

  private String computeKey() {
    String key = "";

    for (IndexedPeekIterator<?> iter : elementIters) {
      if (iter.getCurrent() instanceof Member) {
        if (!key.isEmpty()) key += ".";

        key += ((Member) iter.getCurrent()).getName();
      } else {
        key += "[" + iter.getIndex() + "]";
      }
    }

    return key;
  }

}
