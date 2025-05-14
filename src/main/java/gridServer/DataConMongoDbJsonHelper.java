package gridServer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;


public class DataConMongoDbJsonHelper {


    String mergeFlatJson(List<Map<String, String>> flatJsons) {
        Set<String> cols = new HashSet<>();
        int i = 0;
        // collect all columns
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> flatJson : flatJsons) {
            cols.addAll(flatJson.keySet());
        }
        ArrayList<String> colSorted = new ArrayList<>(cols);
        Collections.sort(colSorted); // have "_id" at first position
        for (String col : colSorted) {
            sb.append(col).append(";");
        }
        sb.append(System.lineSeparator());
        // merge all data
        for (Map<String, String> flatJson : flatJsons) {
            for (String col : colSorted) {
                sb.append(flatJson.getOrDefault(col, "")).append(";");
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }


    private Map<String, String> flattenJson(Map<String,String> values, JsonElement jsonElement, String path) {
        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            Set<String> keySet = jsonObject.keySet();
            for (String s : keySet) {
                jsonElement = jsonObject.get(s);
                flattenJson(values, jsonElement, path.isEmpty() ? s : path + "_" + s);
            }
        } else if (jsonElement.isJsonArray()) {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            int i = 0;
            for (JsonElement element : jsonArray) {
                i++;
                flattenJson(values, jsonElement, path.isEmpty() ? "" + i : path + "_" + i);
            }
        } else {
            values.put(path, jsonElement.toString());
        }
        return values;

    }

    Map<String, String> flattenJson(JsonElement jsonElement) {
        return flattenJson( new HashMap<>(), jsonElement, "");
    }



    private JsonObject adjust(JsonObject jsonObjectParent, String prop, JsonElement jsonElement, String path) {
        int i = -1;
        if (jsonElement.isJsonArray()) {
            i = 0;
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            for (JsonElement element : jsonArray) {
                i++;
                String property = prop + "_" + i ;
                jsonObjectParent.add(property, adjust(element, path + "_" + property));
            }
            jsonObjectParent.remove(prop);
            jsonElement = null;
        } else if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            // {"enabledForClient":{"6513d6d0b6536f24b0e45de4":true}}
            Set<String> childProp = jsonObject.keySet();
            if( childProp.contains("_class") && childProp.contains("lookupValue") ) {
                jsonObject.remove(prop);
                jsonObjectParent.add(prop, jsonObject.get("lookupValue"));
                //jsonElement = null;
            } else if( childProp.contains("_class") ){
                // {"_id": "6513d6d3b6536f24b0e45e21","_class": "supplier"}
                // -> {"supplier": {"_id": "6513d6d3b6536f24b0e45e21"}}
                jsonObject.remove(prop);
                String className = jsonObject.get("_class").getAsString();
                jsonObject.remove("_class");
                JsonObject jsonObjectClass = new JsonObject();
                jsonObjectClass.add(className, jsonObject);
                jsonObjectParent = jsonObjectClass; // !!!! Mutating the parent !!! --> scan modified object
                //jsonElement = null;
            } else if( 1 == childProp.size() && childProp.contains("$oid") ){
                // {"_id":{"$oid":"6513d6d3b6536f24b0e45e21"}}
                // ->  {"_id":"6513d6d3b6536f24b0e45e21"}
                jsonObject.remove(prop);
                jsonObjectParent.add(prop, jsonObject.get("$oid"));
                //jsonElement = null;
            } else if (1 == childProp.size()) { // only 1 property
                String childProp_ = childProp.iterator().next();
                if (childProp_.matches("[0-9a-f]{24}")) { // {"6513d6d0b6536f24b0e45de4":true}
                    // --> {"enabledForClient":true}
                    jsonObject.remove(prop);
                    jsonObjectParent.add(prop, jsonObject.get(childProp_));
                    //jsonElement = null; // check child
                } // artificial key
            } // only 1 property
        }
        if (null != jsonElement && !prop.isEmpty() /* exclude self-recursion on Object-Level*/)  {
            adjust(jsonElement, path + "_" + prop);
        }
        return jsonObjectParent;
    }

    private JsonElement adjust(JsonElement jsonElement, String path) {
        if (jsonElement.isJsonObject()){
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            jsonObject = adjust(jsonObject, "", jsonObject, path); // allow self-Modification
            jsonElement = jsonObject;
            Set<String> keySet = new HashSet<>();
            keySet.addAll(jsonObject.keySet()); // enforce clone to prevent concurrentModification
            for (String s : keySet) {
                adjust(jsonObject, s, jsonObject.get(s), path);
            }
        } else if (jsonElement.isJsonPrimitive() ) {
        }
        return jsonElement;
    }

    JsonObject simplifyJson(JsonObject jsonObject){

        return (JsonObject) adjust(jsonObject,"");
    }

    String simplifyMongoDBJso(String jsonString) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
        JsonObject jsonObject1 = simplifyJson(jsonObject);
        return gson.toJson(jsonObject1);
    }


    JsonObject simplifyMongoDBJso2JsonObject(String jsonString) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
        JsonObject jsonObject1 = simplifyJson(jsonObject);
        return jsonObject1;
    }

}
