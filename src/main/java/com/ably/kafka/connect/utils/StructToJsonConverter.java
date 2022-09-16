package com.ably.kafka.connect.utils;

import com.google.gson.Gson;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.ConnectException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to convert a Struct to a JSON string with eliminating the schema information.
 * This class uses the Gson library to convert the Struct to a JSON string.
 */
public class StructToJsonConverter {
    /**
     * Convert a Struct to a JSON string eliminating the schema information.
     *
     * @param struct the Struct to convert.
     * @return the JSON string.
     * <p>
     * throws a ConnectException if the Struct contains a BYTES field.
     */
    public static String toJsonString(final Struct struct, final Gson gson) {
        final Map<String, Object> structJsonMap = structJsonMap(struct);
        return gson.toJson(structJsonMap);
    }

    /**
     * Convert a Struct to a LinkedHashMap by eliminating schematic information and
     * mapping field names to corresponding values. This function is recursive and the built map will reflect
     * the shape of data in the Struct without schema information.
     *
     * @param struct Struct to convert
     * @return Map that represents shape of data in the Struct without schema information
     */
    private static Map<String, Object> structJsonMap(final Struct struct) {
        if (struct == null) {
            return null;
        }

        final Map<String, Object> structMap = new LinkedHashMap<>(struct.schema().fields().size());

        for (final Field field : struct.schema().fields()) {
            switch (field.schema().type()) {
                case STRUCT:
                    final Struct fieldStruct = struct.getStruct(field.name());
                    structMap.put(field.name(), structJsonMap(fieldStruct));
                    break;
                case ARRAY:
                    final List<Object> fieldArray = struct.getArray(field.name());
                    final List<Object> fieldJsonArray = jsonArrayFromStructArray(fieldArray);
                    structMap.put(field.name(), fieldJsonArray);
                    break;
                case MAP:
                    final Map<String, Object> jsonMap = jsonMapFromStructMap(struct.getMap(field.name()));
                    structMap.put(field.name(), jsonMap);
                    break;
                case INT8:
                    structMap.put(field.name(), struct.getInt8(field.name()));
                    break;
                case INT16:
                    structMap.put(field.name(), struct.getInt16(field.name()));
                    break;
                case INT32:
                    structMap.put(field.name(), struct.getInt32(field.name()));
                    break;
                case INT64:
                    structMap.put(field.name(), struct.getInt64(field.name()));
                    break;
                case FLOAT32:
                    structMap.put(field.name(), struct.getFloat32(field.name()));
                    break;
                case FLOAT64:
                    structMap.put(field.name(), struct.getFloat64(field.name()));
                    break;
                case BOOLEAN:
                    structMap.put(field.name(), struct.getBoolean(field.name()));
                    break;
                case STRING:
                    structMap.put(field.name(), struct.getString(field.name()));
                    break;
                case BYTES:
                    throw new ConnectException("Bytes are currently not supported for conversion to JSON.");
                default:
                    throw new ConnectException("Unexpected and unsupported type encountered." + field.schema().type());
            }
        }
        return structMap;
    }

    /**
     * Convert a map from a map from struct (Struct.getMap()) to a
     * LinkedHashMap by eliminating schematic information and evaluating the value of each key.
     * Like [structJsonMap] this function will build a map that reflects the shape of data in the
     * original map without schema information.
     *
     * @param map Original map that is retrieved from Struct.getMap()
     * @return Map that represents shape of data in the original map without schema information
     */
    private static Map<String, Object> jsonMapFromStructMap(Map<Object, Object> map) {
        if (map == null) {
            return null;
        }
        final Map<String, Object> jsonMap = new LinkedHashMap<>(map.size());
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            jsonMap.put(entry.getKey().toString(), jsonValue(entry.getValue()));
        }
        return jsonMap;
    }

    /**
     * Convert a list of struct array (Struct.getArray()) to a plain list that can be JSON serialized. with
     * enumarating on the original array and making a list of json serializable objects.
     *
     * @param fieldArray Original list of struct array
     * @return List of JSOn serializable objects
     */
    private static List<Object> jsonArrayFromStructArray(List<Object> fieldArray) {
        if (fieldArray == null) {
            return null;
        }
        final List<Object> fieldJsonArray = new ArrayList<>(fieldArray.size());
        for (Object fieldArrayItem : fieldArray) {
            fieldJsonArray.add(jsonValue(fieldArrayItem));
        }
        return fieldJsonArray;
    }

    /**
     * Get a Json serializable value from a value based on the type of original value.
     * Depending on the type of the original value, this function can return a map from a struct,
     * a list from a list of values, a map from a struct map or the value itself.
     *
     * @param originalValue Original value
     * @return JSON serializable object
     */
    private static Object jsonValue(final Object originalValue) {
        if (originalValue instanceof Struct) {
            return structJsonMap((Struct) originalValue);
        } else if (originalValue instanceof List) {
            return jsonArrayFromStructArray((List<Object>) originalValue);
        } else if (originalValue instanceof Map) {
            return jsonMapFromStructMap((Map<Object, Object>) originalValue);
        } else if (originalValue instanceof byte[]) {
            throw new ConnectException("Bytes are currently not supported for conversion to JSON.");
        }
        return originalValue;
    }
}
