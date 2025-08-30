package nl.framegengine.editor;

import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.entity.SceneManager;
import nl.framegengine.core.utils.FileHelper;
import nl.framegengine.core.utils.JsonHelper;

import javax.json.*;
import java.io.*;
import java.util.concurrent.CompletableFuture;

public class EngineSettings {
    public static String currentProjectDirectory = "";
    public static String currentLevelPath = "";
    public static String currentProjectName = "Unknown project";
    public static String currentProjectIconGuid = "";

    public static boolean isCompiled = false;
    private static final String settingsFileName = "/.fgsettings";

    public static void saveSettings(){
        JsonObjectBuilder jsonSaveContent = Json.createObjectBuilder();
        jsonSaveContent.add("currentLevelPath", currentLevelPath);
        jsonSaveContent.add("currentProjectIconGuid", currentProjectIconGuid);
        jsonSaveContent.add("currentProjectName", currentProjectName);

        JsonObject jsonSaveContentObject = jsonSaveContent.build();
        FileHelper.writeToFile(jsonSaveContentObject.toString(), currentProjectDirectory + settingsFileName);
        if(SceneManager.getInstance() != null && SceneManager.getInstance().getCurrentScene() != null)
            FileHelper.writeToFile(SceneManager.sceneToJson(SceneManager.getInstance().getCurrentScene()), currentProjectDirectory + File.separator + currentLevelPath);
    }

    public static void loadSettings() {
        if (!isCompiled && (currentProjectDirectory.isEmpty() || !new File(currentProjectDirectory).exists())) return;
        String saveFileContent = FileHelper.readFile(currentProjectDirectory + settingsFileName);

        if(saveFileContent == null) {
            Debug.logError("No settings file has been found. Creating...");
            saveSettings();
            return;
        }

        JsonObject projectInfo = Json.createReader(new StringReader(saveFileContent)).readObject();

        if (JsonHelper.hasJsonKey(projectInfo, "currentLevelPath")) currentLevelPath = projectInfo.getString("currentLevelPath");
        if (JsonHelper.hasJsonKey(projectInfo, "currentProjectIconGuid")) currentProjectIconGuid = projectInfo.getString("currentProjectIconGuid");
        if (JsonHelper.hasJsonKey(projectInfo, "currentProjectName")) currentProjectName = projectInfo.getString("currentProjectName");
        if(currentProjectName.isBlank()) currentProjectName = FileHelper.getDirectoryName(currentProjectDirectory);

        if(!isCompiled) {
            saveEngineConfig();
            ManifestHelper.updateManifest();
            ManifestHelper.registerManifestListener();
        }

        Debug.log("Project settings successfully loaded in");
    }

    public static void createNewProject(){
        String projectDirectory = FileHelper.selectDirectory();
        if(projectDirectory == null){
            Debug.logError("Project directory is not a valid path");
            return;
        }
        EngineSettings.currentProjectDirectory = projectDirectory;
        EngineSettings.saveSettings();

        try {
            FileHelper.copyResourceToDirectory("default project/", projectDirectory);
        } catch (Exception e) {
            Debug.log("Something went wrong trying to create the project: " + e.getMessage());
        }
        Debug.log("Creating new project at " + projectDirectory);
    }

    public static void loadProject(){
        String projectDirectory = FileHelper.selectDirectory();
        if(projectDirectory == null){
            Debug.logError("Project directory is not a valid path");
            return;
        }
        EngineSettings.currentProjectDirectory = projectDirectory;
        EngineSettings.loadSettings();
    }

    private static void saveEngineConfig(){
        String userHome = System.getProperty("user.home");
        File configDir = new File(userHome, ".framegengine");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        File settingsFile = new File(configDir, "editorconfig.json");

        JsonObjectBuilder jsonSaveContent = Json.createObjectBuilder();
        jsonSaveContent.add("currentProjectDirectory", currentProjectDirectory);

        JsonObject jsonSaveContentObject = jsonSaveContent.build();
        FileHelper.writeToFile(jsonSaveContentObject.toString(), settingsFile.getAbsolutePath());
    }

    public static void loadEngineConfig(){
        String userHome = System.getProperty("user.home");
        File configDir = new File(userHome, ".framegengine");
        File settingsFile = new File(configDir, "editorconfig.json");

        String appMode = System.getProperty("app.mode", "dev"); // default to dev if not set

        isCompiled = ("compiled".equalsIgnoreCase(appMode));

        if(isCompiled) {
            currentProjectDirectory = "/userresource";
            Debug.log("Current directory set to " + currentProjectDirectory);
            return;
        }


        String saveFileContent = FileHelper.readFile(settingsFile.getAbsolutePath());
        if(saveFileContent == null) {
            Debug.logError("No editor config found. Creating...");
            saveEngineConfig();
            return;
        }

        JsonObject projectInfo = Json.createReader(new StringReader(saveFileContent)).readObject();

        if (!JsonHelper.hasJsonKey(projectInfo, "currentProjectDirectory")) return;

        currentProjectDirectory = projectInfo.getString("currentProjectDirectory");
        currentProjectName = FileHelper.getDirectoryName(currentProjectDirectory);
    }

    public static void buildProjectMac(){
        ImGuiHelper.showProgressBar("Building");
        Debug.log("Starting build for Mac");

        CompletableFuture.runAsync(() -> {
            try {
                File outputDirectory = new File(currentProjectDirectory + File.separator + "build");
                String iconPath = ManifestHelper.getPathByGuid(ManifestHelper.manifestFileType.TEXTURE, currentProjectIconGuid);
                File iconFile = (iconPath == null || iconPath.isBlank()) ? null : new File(iconPath);
                if(iconFile == null || !iconFile.exists() || (iconFile.exists() && !FileHelper.getExtension(iconFile.getPath()).equalsIgnoreCase("icns"))) iconFile = new File("textures/FrameGengine_icon.icns");
                if(outputDirectory.exists()) FileHelper.deleteDirectory(outputDirectory.toPath());
                File currentProjectDirectoryFile = new File(currentProjectDirectory);

                String[] command = {"./gradlew",
                        "buildGame",
                        "--warn",
                        "-PcustomAppName=" + currentProjectName,
                        "-PcustomDest=" + outputDirectory.getPath(),
                        "-PcustomIcon=" + iconFile.getPath(),
                        "-PcustomProjectPath=" + currentProjectDirectoryFile.getAbsolutePath(),
                        "-PcustomFileType=app-image",
                };

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(new File(System.getProperty("user.dir"))); // Set working directory
                pb.redirectErrorStream(true);
                Process process = pb.start();

                Thread outputReader = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println(line); // Print to Java console or your logger
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                outputReader.start();

                int exitCode = process.waitFor();
                outputReader.join(); // Wait until stream reading is done

                if (exitCode == 0) {
                    Debug.log("Build completed successfully.");
                    FileHelper.openDirectory(outputDirectory);
                } else {
                    Debug.logError("Build failed. Exit code: " + exitCode);
                }
            } catch (
                    Exception e) {
                Debug.logError("Error while building: " + e.getMessage());
                e.printStackTrace();
            }
        }).thenRun(() -> {
            ImGuiHelper.hideProgressBar();
        });
    }

    public static void buildProjectWindows(){
        ImGuiHelper.showProgressBar("Building");
        Debug.log("Starting build for Windows");

        CompletableFuture.runAsync(() -> {
            try {
                File outputDirectory = new File(currentProjectDirectory + File.separator + "build");
                File iconPath = new File(ManifestHelper.getPathByGuid(ManifestHelper.manifestFileType.TEXTURE, currentProjectIconGuid));
                if(iconPath == null || !iconPath.exists() || (iconPath.exists() && !FileHelper.getExtension(iconPath.getPath()).equalsIgnoreCase("ico"))) iconPath = new File("textures/FrameGengine_icon.ico");
                if(outputDirectory.exists()) FileHelper.deleteDirectory(outputDirectory.toPath());

                String[] command = {"./gradlew",
                        "buildGame",
                        "-PcustomAppName=" + currentProjectName,
                        "-PcustomDest=" + outputDirectory.getPath(),
                        "-PcustomIcon=" + iconPath.getAbsolutePath(),
                        "-PcustomFileType=exe",
                };

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(new File(System.getProperty("user.dir"))); // Set working directory
                pb.redirectErrorStream(true);
                Process process = pb.start();

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    Debug.log("Build completed successfully.");
                } else {
                    Debug.logError("Build failed. Exit code: " + exitCode);
                }
            } catch (Exception e) {
                Debug.logError("Error while building: " + e.getMessage());
                e.printStackTrace();
            }
        }).thenRun(ImGuiHelper::hideProgressBar);
    }

    public static String getSettingsFileName(){
        return settingsFileName;
    }
}