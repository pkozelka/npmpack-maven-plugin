package net.kozelka.npmpack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Petr Kozelka
 */
public class JsonUtils {

    /**
     * Sorts properties of a json object, recursively
     * @param jo
     */
    public static void sortJsonDeep(JsonElement jo) {
        final LinkedList<JsonObject> unsorted = new LinkedList<JsonObject>();
        if (jo.isJsonObject()) {
            unsorted.add(jo.getAsJsonObject());
        } else if (jo.isJsonArray()) {
            for (JsonElement item : jo.getAsJsonArray()) {
                if (item.isJsonObject()) {
                    unsorted.add(item.getAsJsonObject());
                }
            }
        }
        while (!unsorted.isEmpty()) {
            sortJsonObject(unsorted.removeFirst(), unsorted);
        }
    }

    private static void sortJsonObject(JsonObject jo, Collection<JsonObject> unsorted) {
        final List<Map.Entry<String, JsonElement>> entries = new ArrayList<Map.Entry<String, JsonElement>>(jo.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, JsonElement>>() {
            @Override
            public int compare(Map.Entry<String, JsonElement> o1, Map.Entry<String, JsonElement> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        // remove unsorted entries, and process those that are themselves objects
        for (Map.Entry<String, JsonElement> entry : entries) {
            final JsonElement value = jo.remove(entry.getKey());
            if (value.isJsonObject()) {
                unsorted.add(value.getAsJsonObject());
            } else if (value.isJsonArray()) {
                final JsonArray ja = value.getAsJsonArray();
                for (JsonElement jsonElement : ja) {
                    if (jsonElement.isJsonObject()) {
                        unsorted.add(jsonElement.getAsJsonObject());
                    }
                }
            }
        }
        // add back in sorted order
        for (Map.Entry<String, JsonElement> entry : entries) {
            jo.add(entry.getKey(), entry.getValue());
        }
    }
}
