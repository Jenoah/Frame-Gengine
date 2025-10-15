package nl.framegengine.editor;

import nl.framegengine.core.callbacks.EventCallback;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.utils.FileHelper;
import nl.framegengine.core.utils.JsonHelper;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ManifestHelper {
    private static final String manifestFileName = "/.fgmanifest";

    private static final List<HashMap<String, String>> textures = new ArrayList<>();
    private static final List<HashMap<String, String>> scripts = new ArrayList<>();
    private static final List<HashMap<String, String>> levels = new ArrayList<>();
    private static final List<HashMap<String, String>> materials = new ArrayList<>();
    private static final List<HashMap<String, String>> others = new ArrayList<>();

    private static final List<EventCallback> eventCallbacks = new ArrayList<>();
    private static FileAlterationMonitor monitor = null;

    public static void registerManifestListener() {
        try {
            // Get absolute paths for files to exclude
            File manifestFile = new File(EngineSettings.currentProjectDirectory + File.separator + manifestFileName).getCanonicalFile();
            File settingsFile = new File(EngineSettings.currentProjectDirectory + File.separator + EngineSettings.getSettingsFileName()).getCanonicalFile();

            Set<String> excludedPaths = Set.of(
                    manifestFile.getAbsolutePath(),
                    settingsFile.getAbsolutePath()
            );

            Set<String> excludedDirs = Set.of("build", ".compiled");

            IOFileFilter customFilter = new IOFileFilter() {
                @Override
                public boolean accept(File file) {
                    try {
                        // Skip directories in the excluded list
                        File parent = file;
                        while (parent != null) {
                            if (excludedDirs.contains(parent.getName())) {
                                return false;
                            }
                            parent = parent.getParentFile();
                        }

                        // Exclude by full path
                        String absolutePath = file.getCanonicalPath();
                        if (excludedPaths.contains(absolutePath)) return false;

                        // Exclude .app files
                        if (file.getName().endsWith(".app")) return false;

                        return true;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public boolean accept(File dir, String name) {
                    // Same as above, but for directory entries
                    if (excludedDirs.contains(name)) {
                        return false;
                    }
                    return true;
                }
            };

            FileAlterationObserver observer = FileAlterationObserver.builder()
                    .setFile(new File(EngineSettings.currentProjectDirectory))
                    .setFileFilter(customFilter)
                    .get();

            monitor = new FileAlterationMonitor(1000);
            FileAlterationListener listener = new ManifestHelper.ManifestFileListener();

            observer.addListener(listener);
            monitor.addObserver(observer);
            monitor.start();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadManifest(){
        File manifestFile = new File(getManifestPath());

        textures.clear();
        scripts.clear();
        levels.clear();
        materials.clear();
        others.clear();

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
                        case "materials" -> jsonValue.asJsonArray().forEach(jsonArrayValue -> {
                            if (jsonArrayValue.getValueType() == JsonValue.ValueType.OBJECT) {
                                materials.add(manifestJsonToHashmapItem(jsonArrayValue.asJsonObject()));
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
            Debug.log("Loaded in " + (textures.size() + scripts.size() + levels.size() + materials.size() + others.size()) + " data entries");
        }else{
            Debug.logError("Manifest file empty");
        }
    }

    public static void updateManifest() {
        loadManifest();

        JsonObjectBuilder jsonManifestContent = Json.createObjectBuilder();
        JsonArrayBuilder textureArray = Json.createArrayBuilder();
        JsonArrayBuilder scriptArray = Json.createArrayBuilder();
        JsonArrayBuilder levelArray = Json.createArrayBuilder();
        JsonArrayBuilder materialArray = Json.createArrayBuilder();
        JsonArrayBuilder otherArray = Json.createArrayBuilder();

        List<HashMap<String, String>> manifestTextures = new ArrayList<>();
        List<HashMap<String, String>> manifestScripts = new ArrayList<>();
        List<HashMap<String, String>> manifestLevels = new ArrayList<>();
        List<HashMap<String, String>> manifestMaterials = new ArrayList<>();
        List<HashMap<String, String>> manifestOthers = new ArrayList<>();

        Path[] filesInProject = FileHelper.findFilesInDirectory(Paths.get(EngineSettings.currentProjectDirectory), new HashSet<>(Arrays.asList(".app", ".tmp", ".bak"))).toArray(Path[]::new);

        try {
            for (Path filePath : filesInProject) {
                manifestFileType fileType = pathToManifestFileType(filePath);
                if (!Files.exists(filePath) || Files.isHidden(filePath)) continue;
                String fileGUID = FileHelper.getChecksum(filePath.toAbsolutePath().toString());
                String relativePath = Paths.get(EngineSettings.currentProjectDirectory).relativize(filePath).toString();

                switch (fileType) {
                    case TEXTURE -> addManifestRecord(textures, fileGUID, relativePath, manifestTextures, filePath);
                    case SCRIPT -> addManifestRecord(scripts, fileGUID, relativePath, manifestScripts, filePath);
                    case LEVEL -> addManifestRecord(levels, fileGUID, relativePath, manifestLevels, filePath);
                    case MATERIAL -> addManifestRecord(materials, fileGUID, relativePath, manifestMaterials, filePath);
                    case null, default -> addManifestRecord(others, fileGUID, relativePath, manifestOthers, filePath);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        manifestTextures.forEach(manifestValue -> addToManifestArray(manifestValue, textureArray));
        manifestScripts.forEach(manifestValue -> addToManifestArray(manifestValue, scriptArray));
        manifestLevels.forEach(manifestValue -> addToManifestArray(manifestValue, levelArray));
        manifestMaterials.forEach(manifestValue -> addToManifestArray(manifestValue, materialArray));
        manifestOthers.forEach(manifestValue -> addToManifestArray(manifestValue, otherArray));

        jsonManifestContent.add("textures", textureArray.build());
        jsonManifestContent.add("scripts", scriptArray.build());
        jsonManifestContent.add("levels", levelArray.build());
        jsonManifestContent.add("materials", materialArray.build());
        jsonManifestContent.add("others", otherArray.build());

        Map<String, Boolean> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(config);

        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = jsonWriterFactory.createWriter(stringWriter);
        jsonWriter.write(jsonManifestContent.build());

        FileHelper.writeToFile(stringWriter.toString(), getManifestPath());

        if(!eventCallbacks.isEmpty()) eventCallbacks.forEach(EventCallback::onTrigger);
        Debug.log("Manifest updated");
    }

    public static boolean setEventCallback(EventCallback callback){
        eventCallbacks.clear();
        return eventCallbacks.add(callback);
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

    private static void addToManifestArray(HashMap<String, String> value, JsonArrayBuilder jsonArrayBuilder){
        JsonObjectBuilder fileInfo = Json.createObjectBuilder();
        fileInfo.add("guid", value.get("guid"));
        fileInfo.add("path", value.get("path"));
        fileInfo.add("filename", value.get("filename"));
        jsonArrayBuilder.add(fileInfo.build());
    }

    private static void addManifestRecord(List<HashMap<String, String>> iterationList, String fileGuid, String relativePath, List<HashMap<String, String>> manifestArray, Path filePath){
        boolean hasAddedFile = false;
        for (HashMap<String, String> value : iterationList) {
            if(value.get("guid").equals(fileGuid) && !value.get("path").equals(relativePath)){
                value.replace("path", relativePath);
                value.replace("filename", FileHelper.getFileName(filePath.toString()));
                manifestArray.add(value);
                hasAddedFile = true;
                break;
            }else if(value.get("path").equals(relativePath) && !value.get("guid").equals(fileGuid)){
                manifestArray.add(value);
                hasAddedFile = true;
                break;
            }
        }
        if(!hasAddedFile){
            HashMap<String, String> fileHashmap = new HashMap<>();
            fileHashmap.put("guid", fileGuid);
            fileHashmap.put("path", relativePath);
            fileHashmap.put("filename", FileHelper.getFileName(filePath.toString()));
            manifestArray.add(fileHashmap);
        }
    }

    public enum manifestFileType{
        TEXTURE,
        SCRIPT,
        LEVEL,
        MATERIAL,
        NULL
    }

    private static manifestFileType fileToManifestFileType(File file){
        return pathToManifestFileType(file.toPath());
    }

    private static manifestFileType pathToManifestFileType(Path path){
        String extension = FileHelper.getExtension(path.toString());

        return switch (extension) {
            case "jpg", "jpeg", "JPG", "JPEG", "png", "PNG", "gif", "tiff" -> manifestFileType.TEXTURE;
            case "lvl" -> manifestFileType.LEVEL;
            case "java" -> manifestFileType.SCRIPT;
            case "mtrl" -> manifestFileType.MATERIAL;
            case null, default -> manifestFileType.NULL;
        };
    }

    public static List<HashMap<String, String>> getTextures(){ return textures; }
    public static List<HashMap<String, String>> getScripts(){ return scripts; }
    public static List<HashMap<String, String>> getLevels(){ return levels; }
    public static List<HashMap<String, String>> getMaterials(){ return materials; }
    public static List<HashMap<String, String>> getOthers(){ return others; }

    public static List<HashMap<String, String>> getOfType(manifestFileType fileType){
        switch (fileType){
            case TEXTURE -> { return getTextures(); }
            case SCRIPT -> { return getScripts(); }
            case LEVEL -> { return getLevels(); }
            case MATERIAL -> { return getMaterials(); }
            case null, default -> { return getOthers(); }
        }
    }

    public static String getGuidByPath(manifestFileType fileType, String path){
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

    public static String getPathByGuid(manifestFileType fileType, String guid){
        AtomicReference<String> path = new AtomicReference<>();

        List<HashMap<String, String>> typeArray = getOfType(fileType);
        for (HashMap<String, String> map : typeArray) {
            if(map.get("guid").equals(guid)){
                path.set(map.get("path"));
                break;
            }
        }

        return path.get();
    }

    public static boolean hasGuid(manifestFileType fileType, String guid){
        AtomicBoolean hasGuid = new AtomicBoolean(false);

        List<HashMap<String, String>> typeArray = getOfType(fileType);
        typeArray.forEach(map -> {
            if(map.get("guid").equals(guid)){
                hasGuid.set(true);
            }
        });
        return hasGuid.get();
    }


    private static class ManifestFileListener implements FileAlterationListener {
        @Override
        public void onFileCreate(File file) { updateManifest(); }

        @Override
        public void onDirectoryChange(File file) {}

        @Override
        public void onDirectoryCreate(File file) {}

        @Override
        public void onDirectoryDelete(File file) {}

        @Override
        public void onFileChange(File file) { updateManifest(); }

        @Override
        public void onFileDelete(File file) { updateManifest(); }

        @Override
        public void onStart(FileAlterationObserver fileAlterationObserver) {}

        @Override
        public void onStop(FileAlterationObserver observer) {}
    }

    public static boolean isRunning(){ return monitor != null; }
}
