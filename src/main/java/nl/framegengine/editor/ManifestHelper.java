package nl.framegengine.editor;

import nl.framegengine.core.callbacks.EventCallback;
import nl.framegengine.core.utils.FileHelper;
import nl.framegengine.core.utils.JsonHelper;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ManifestHelper {
    private static final String manifestFileName = "/.fgmanifest";

    private static final List<HashMap<String, String>> textures = new ArrayList<>();
    private static final List<HashMap<String, String>> scripts = new ArrayList<>();
    private static final List<HashMap<String, String>> levels = new ArrayList<>();
    private static final List<HashMap<String, String>> others = new ArrayList<>();

    private static final List<EventCallback> eventCallbacks = new ArrayList<>();

    public static void registerManifestListener(){
        String[] excludedFileNames = {
                FileHelper.getFileName(manifestFileName) + "." + FileHelper.getExtension(manifestFileName),
                FileHelper.getFileName(EngineSettings.getSettingsFileName()) + "." + FileHelper.getExtension(EngineSettings.getSettingsFileName()),
                "*.app"
        };

        try {
// Exclude specific directories
            IOFileFilter excludeDirsFilter = new IOFileFilter() {
                private final Set<String> excludedDirNames = Set.of("build", ".compiled");

                @Override
                public boolean accept(File file) {
                    // Accept files only if not in excluded directories
                    File parent = file.getParentFile();
                    while (parent != null) {
                        if (excludedDirNames.contains(parent.getName())) {
                            return false;
                        }
                        parent = parent.getParentFile();
                    }
                    return true;
                }

                @Override
                public boolean accept(File dir, String name) {
                    // Directory accept only if not in excluded directories
                    if (excludedDirNames.contains(name)) {
                        return false;
                    }
                    return true;
                }
            };

            IOFileFilter excludeFilesFilter = FileFilterUtils.or(
                    Arrays.stream(excludedFileNames)
                            .map(FileFilterUtils::nameFileFilter)
                            .toArray(IOFileFilter[]::new)
            );

            IOFileFilter combinedFilter = FileFilterUtils.and(
                    FileFilterUtils.notFileFilter(excludeFilesFilter),
                    excludeDirsFilter
            );

            FileAlterationObserver observer = FileAlterationObserver.builder()
                    .setFile(new File(EngineSettings.currentProjectDirectory))
                    .setFileFilter(combinedFilter)
                    .get();

            FileAlterationMonitor monitor = new FileAlterationMonitor(1000);
            FileAlterationListener listener = new ManifestHelper.ManifestFileListener();

            observer.addListener(listener);
            monitor.addObserver(observer);
            monitor.start();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateManifest(){
        File manifestFile = new File(getManifestPath());

        textures.clear();
        scripts.clear();
        levels.clear();
        others.clear();

        if(manifestFile.exists()){
            String manifestFileContent = FileHelper.readFile(manifestFile.getAbsolutePath());
            if(manifestFileContent != null && !manifestFileContent.isBlank()) {
                JsonObject manifestInfo = Json.createReader(new StringReader(manifestFileContent)).readObject();
                manifestInfo.forEach((s, jsonValue) -> {
                    if(jsonValue.getValueType() == JsonValue.ValueType.ARRAY){
                        switch (s) {
                            case "textures" -> jsonValue.asJsonArray().forEach(jsonArrayValue -> {
                                if (jsonArrayValue.getValueType() == JsonValue.ValueType.OBJECT) {
                                    textures.add(manifestJsonToHashmapItem(jsonArrayValue.asJsonObject()));
                                }
                            });
                            case "scripts" -> jsonValue.asJsonArray().forEach(jsonArrayValue -> {
                                if (jsonArrayValue.getValueType() == JsonValue.ValueType.OBJECT) {
                                    scripts.add(manifestJsonToHashmapItem(jsonArrayValue.asJsonObject()));
                                }
                            });
                            case "levels" -> jsonValue.asJsonArray().forEach(jsonArrayValue -> {
                                if (jsonArrayValue.getValueType() == JsonValue.ValueType.OBJECT) {
                                    levels.add(manifestJsonToHashmapItem(jsonArrayValue.asJsonObject()));
                                }
                            });
                            case null, default -> jsonValue.asJsonArray().forEach(jsonArrayValue -> {
                                if (jsonArrayValue.getValueType() == JsonValue.ValueType.OBJECT) {
                                    others.add(manifestJsonToHashmapItem(jsonArrayValue.asJsonObject()));
                                }
                            });
                        }
                    }
                });
            }
        }

        JsonObjectBuilder jsonManifestContent = Json.createObjectBuilder();
        JsonArrayBuilder textureArray = Json.createArrayBuilder();
        JsonArrayBuilder scriptArray = Json.createArrayBuilder();
        JsonArrayBuilder levelArray = Json.createArrayBuilder();
        JsonArrayBuilder otherArray = Json.createArrayBuilder();

        List<HashMap<String, String>> manifestTextures = new ArrayList<>();
        List<HashMap<String, String>> manifestScripts = new ArrayList<>();
        List<HashMap<String, String>> manifestLevels = new ArrayList<>();
        List<HashMap<String, String>> manifestOthers = new ArrayList<>();

        File[] filesInProject = FileHelper.findFilesInDirectory(new File(EngineSettings.currentProjectDirectory), new String[]{".app", ".tmp", ".bak"}).toArray(File[]::new);

        for (File file : filesInProject) {
            manifestFileType fileType = fileToManifestFileType(file);
            if(!file.exists() || file.isHidden()) continue;
            AtomicBoolean hasAddedFile = new AtomicBoolean(false);
            String fileGUID = FileHelper.getChecksum(file.getAbsolutePath());
            String relativePath = Paths.get(EngineSettings.currentProjectDirectory).relativize(file.toPath()).toString();

            if(fileType == manifestFileType.TEXTURE){
                for (HashMap<String, String> textures : textures) {
                    if(textures.get("guid").equals(fileGUID) && !textures.get("path").equals(relativePath)){
                        textures.replace("path", relativePath);
                        textures.replace("filename", FileHelper.getFileName(file.getPath()));
                        manifestTextures.add(textures);
                        hasAddedFile.set(true);
                        break;
                    }else if(textures.get("path").equals(relativePath) && !textures.get("guid").equals(fileGUID)){
                        manifestTextures.add(textures);
                        hasAddedFile.set(true);
                        break;
                    }
                }
                if(!hasAddedFile.get()){
                    HashMap<String, String> fileHashmap = new HashMap<>();
                    fileHashmap.put("guid", fileGUID);
                    fileHashmap.put("path", relativePath);
                    fileHashmap.put("filename", FileHelper.getFileName(file.getPath()));
                    manifestTextures.add(fileHashmap);
                }
            } else if (fileType == manifestFileType.SCRIPT) {
                for (HashMap<String, String> script : scripts) {
                    if(script.get("guid").equals(fileGUID) && !script.get("path").equals(relativePath)){
                        script.replace("path", relativePath);
                        script.replace("filename", FileHelper.getFileName(file.getPath()));
                        manifestScripts.add(script);
                        hasAddedFile.set(true);
                        break;
                    }else if(script.get("path").equals(relativePath) && !script.get("guid").equals(fileGUID)){
                        manifestTextures.add(script);
                        hasAddedFile.set(true);
                        break;
                    }
                }
                if(!hasAddedFile.get()){
                    HashMap<String, String> fileHashmap = new HashMap<>();
                    fileHashmap.put("guid", fileGUID);
                    fileHashmap.put("path", relativePath);
                    fileHashmap.put("filename", FileHelper.getFileName(file.getPath()));
                    manifestScripts.add(fileHashmap);
                }
            } else if (fileType == manifestFileType.LEVEL) {
                for (HashMap<String, String> level : levels) {
                    if(level.get("guid").equals(fileGUID) && !level.get("path").equals(relativePath)){
                        level.replace("path", relativePath);
                        level.replace("filename", FileHelper.getFileName(file.getPath()));
                        manifestLevels.add(level);
                        hasAddedFile.set(true);
                        break;
                    }else if(level.get("path").equals(relativePath) && !level.get("guid").equals(fileGUID)){
                        manifestLevels.add(level);
                        hasAddedFile.set(true);
                        break;
                    }
                }
                if(!hasAddedFile.get()){
                    HashMap<String, String> fileHashmap = new HashMap<>();
                    fileHashmap.put("guid", fileGUID);
                    fileHashmap.put("path", relativePath);
                    fileHashmap.put("filename", FileHelper.getFileName(file.getPath()));
                    manifestLevels.add(fileHashmap);
                }
            } else {
                for (HashMap<String, String> other : others) {
                    if (other.get("guid").equals(fileGUID) && !other.get("path").equals(relativePath)) {
                        other.replace("path", relativePath);
                        other.replace("filename", FileHelper.getFileName(file.getPath()));
                        manifestScripts.add(other);
                        hasAddedFile.set(true);
                        break;
                    } else if (other.get("path").equals(relativePath) && !other.get("guid").equals(fileGUID)) {
                        manifestTextures.add(other);
                        hasAddedFile.set(true);
                        break;
                    }
                }
                if (!hasAddedFile.get()) {
                    HashMap<String, String> fileHashmap = new HashMap<>();
                    fileHashmap.put("guid", fileGUID);
                    fileHashmap.put("path", relativePath);
                    fileHashmap.put("filename", FileHelper.getFileName(file.getPath()));
                    manifestOthers.add(fileHashmap);
                }
            }
        }

        manifestTextures.forEach(manifestTexture -> {
            JsonObjectBuilder fileInfo = Json.createObjectBuilder();
            fileInfo.add("guid", manifestTexture.get("guid"));
            fileInfo.add("path", manifestTexture.get("path"));
            fileInfo.add("filename", manifestTexture.get("filename"));
            textureArray.add(fileInfo.build());
        });
        manifestScripts.forEach(manifestScript -> {
            JsonObjectBuilder fileInfo = Json.createObjectBuilder();
            fileInfo.add("guid", manifestScript.get("guid"));
            fileInfo.add("path", manifestScript.get("path"));
            fileInfo.add("filename", manifestScript.get("filename"));
            scriptArray.add(fileInfo.build());
        });
        manifestLevels.forEach(manifestLevel -> {
            JsonObjectBuilder fileInfo = Json.createObjectBuilder();
            fileInfo.add("guid", manifestLevel.get("guid"));
            fileInfo.add("path", manifestLevel.get("path"));
            fileInfo.add("filename", manifestLevel.get("filename"));
            levelArray.add(fileInfo.build());
        });
        manifestOthers.forEach(manifestOther -> {
            JsonObjectBuilder fileInfo = Json.createObjectBuilder();
            fileInfo.add("guid", manifestOther.get("guid"));
            fileInfo.add("path", manifestOther.get("path"));
            fileInfo.add("filename", manifestOther.get("filename"));
            otherArray.add(fileInfo.build());
        });

        jsonManifestContent.add("textures", textureArray.build());
        jsonManifestContent.add("scripts", scriptArray.build());
        jsonManifestContent.add("levels", levelArray.build());
        jsonManifestContent.add("others", otherArray.build());

        Map<String, Boolean> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(config);

        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = jsonWriterFactory.createWriter(stringWriter);
        jsonWriter.write(jsonManifestContent.build());

        FileHelper.writeToFile(stringWriter.toString(), getManifestPath());

        if(!eventCallbacks.isEmpty()) eventCallbacks.forEach(callback -> callback.onTrigger());
    }

    public static boolean setEventCallback(EventCallback callback){
        eventCallbacks.clear();
        if(!eventCallbacks.contains(callback)) return eventCallbacks.add(callback);
        return false;
    }

    public static boolean addEventCallback(EventCallback callback){
        if(!eventCallbacks.contains(callback)) return eventCallbacks.add(callback);
        return false;
    }

    public static boolean removeEventCallback(EventCallback callback){
        return eventCallbacks.remove(callback);
    }

    private static HashMap<String, String> manifestJsonToHashmapItem(JsonObject jsonObject){
        HashMap<String, String> itemInfo = new HashMap<>();
        if(JsonHelper.hasJsonKey(jsonObject, "guid")) itemInfo.put("guid", jsonObject.getString("guid"));
        if(JsonHelper.hasJsonKey(jsonObject, "path")) itemInfo.put("path", jsonObject.getString("path"));
        if(JsonHelper.hasJsonKey(jsonObject, "filename")) itemInfo.put("filename", jsonObject.getString("filename"));

        return itemInfo;
    }

    public static String getManifestPath(){
        return Paths.get(EngineSettings.currentProjectDirectory, manifestFileName).toString();
    }

    public enum manifestFileType{
        TEXTURE,
        SCRIPT,
        LEVEL,
        NULL
    }

    private static manifestFileType fileToManifestFileType(File file){
        String extension = FileHelper.getExtension(file.getPath());

        return switch (extension) {
            case "jpg", "jpeg", "JPG", "JPEG", "png", "PNG", "gif", "tiff" -> manifestFileType.TEXTURE;
            case "lvl" -> manifestFileType.LEVEL;
            case "java" -> manifestFileType.SCRIPT;
            case null, default -> manifestFileType.NULL;
        };
    }

    public static final List<HashMap<String, String>> getTextures(){
        return textures;
    }

    public static final List<HashMap<String, String>> getScripts(){
        return scripts;
    }

    public static final List<HashMap<String, String>> getLevels(){
        return levels;
    }

    public static final List<HashMap<String, String>> getOthers(){
        return others;
    }

    public static final List<HashMap<String, String>> getOfType(manifestFileType fileType){
        switch (fileType){
            case TEXTURE -> { return getTextures(); }
            case SCRIPT -> { return getScripts(); }
            case LEVEL -> { return getLevels(); }
            case null, default -> { return getOthers(); }
        }
    }

    public static final String getGuidbyPath(manifestFileType fileType, String path){
        AtomicReference<String> guid = new AtomicReference<>();
        File file = new File(path);
        if(!file.exists()) return null;
        if(file.isAbsolute()){
            path = Paths.get(EngineSettings.currentProjectDirectory).relativize(Paths.get(path)).toString();
        }

        List<HashMap<String, String>> typeArray = getOfType(fileType);
        String finalPath = path;
        typeArray.forEach(map -> {
            if(map.get("path").equals(finalPath)){
                guid.set(map.get("guid"));
            }
        });
        return guid.get();
    }

    public static final String getPathByGuid(manifestFileType fileType, String guid){
        AtomicReference<String> path = new AtomicReference<>();

        List<HashMap<String, String>> typeArray = getOfType(fileType);
        typeArray.forEach(map -> {
            if(map.get("guid").equals(guid)){
                path.set(map.get("path"));
            }
        });
        return path.get();
    }


    private static class ManifestFileListener implements FileAlterationListener {
        @Override
        public void onFileCreate(File file) {
            updateManifest();
        }

        @Override
        public void onDirectoryChange(File file) {

        }

        @Override
        public void onDirectoryCreate(File file) {

        }

        @Override
        public void onDirectoryDelete(File file) {

        }

        @Override
        public void onFileChange(File file) {
            updateManifest();
        }

        @Override
        public void onFileDelete(File file) {
            updateManifest();
        }

        @Override
        public void onStart(FileAlterationObserver fileAlterationObserver) {}

        @Override
        public void onStop(FileAlterationObserver observer) {}
    }
}
