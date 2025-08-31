package nl.framegengine.core.utils;

import javax.json.JsonObject;

public interface IJsonSerializable {
    JsonObject serializeToJson();

    IJsonSerializable deserializeFromJson(String json);

    static Object deserializeFromJsonToObject(String json, Class<?> classType) throws Exception {
        Object instance = classType.getDeclaredConstructor().newInstance();
        ((IJsonSerializable)instance).deserializeFromJson(json);
        return instance;
    }
}
