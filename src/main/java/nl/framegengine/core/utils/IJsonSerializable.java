package nl.framegengine.core.utils;

import javax.json.JsonObject;

public interface IJsonSerializable {

    String guid = "";

    String getGuid();

    IJsonSerializable setGuid(String guid);

    JsonObject serializeToJson();

    IJsonSerializable deserializeFromJson(String json);

    static Object deserializeFromJsonToObject(String json, Class<?> classType) throws Exception {
        Object instance = classType.getDeclaredConstructor().newInstance();
        ((IJsonSerializable)instance).deserializeFromJson(json);
        return instance;
    }
}
