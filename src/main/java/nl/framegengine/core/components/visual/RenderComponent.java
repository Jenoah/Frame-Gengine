package nl.framegengine.core.components.visual;

import nl.framegengine.core.components.Component;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.entity.SceneManager;
import nl.framegengine.core.modelLoaders.StaticMeshLoader;
import nl.framegengine.core.rendering.RenderManager;
import nl.framegengine.core.shaders.ShaderManager;
import nl.framegengine.core.utils.AABB;
import nl.framegengine.core.utils.Constants;
import nl.framegengine.core.utils.IJsonSerializable;
import nl.framegengine.core.utils.JsonHelper;
import nl.framegengine.core.visual.Material;
import nl.framegengine.core.visual.MaterialManager;
import nl.framegengine.core.visual.Mesh;
import nl.framegengine.core.visual.MeshMaterialSet;
import nl.framegengine.editor.ManifestHelper;
import nl.framegengine.editor.panels.ICustomEditorPanel;
import nl.framegengine.editor.panels.InfoPanel;
import org.joml.Vector3f;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RenderComponent extends Component implements ICustomEditorPanel {

    protected List<String> meshPaths = new ArrayList<>();

    protected final Set<MeshMaterialSet> meshMaterialSets = new HashSet<>();
    private Set<String> fieldsToIgnore = Set.of("fieldsToIgnore", "runInEditor", "guid", "root", "hasCleanedUp", "addedDuringPlaymode", "hasInitiated", "meshPaths");


    public RenderComponent(){ }

    public RenderComponent(Mesh mesh) {
        addMesh(mesh);
    }

    public RenderComponent(Mesh mesh, Material material) {
        addMesh(mesh, material);
    }

    public RenderComponent(MeshMaterialSet meshMaterialSet) {
        addMesh(meshMaterialSet);
    }

    public RenderComponent(Set<MeshMaterialSet> meshMaterialSets) {
        addMeshes(meshMaterialSets);
    }

    public void addMeshes(Set<MeshMaterialSet> meshMaterialSets) {
        meshMaterialSets.forEach(this::addMesh);
    }

    public void addMesh(MeshMaterialSet meshMaterialSet) {
        addMesh(meshMaterialSet.getMesh(), meshMaterialSet.material);
    }

    public void addMesh(Mesh mesh) {
        addMesh(mesh, new Material(ShaderManager.getDefaultShader()));

    }

    public void addMesh(Mesh mesh, Material material) {
        meshMaterialSets.add(new MeshMaterialSet(mesh, material).setRoot(this.getRoot()));
        if(!meshPaths.contains(mesh.getMeshPath())) meshPaths.add(mesh.getMeshPath());
        if(hasInitiated){
            dequeueRender();
            queueRender();
        }
    }

    public Set<MeshMaterialSet> getMeshMaterialSets() {
        return meshMaterialSets;
    }

    @Override
    public void initiate() {
        if (hasInitiated) return;
        super.initiate();

        if(meshMaterialSets.isEmpty()){
            root.removeComponent(this);
            return;
        }
        calculateRadius();
        calculateAABB();
        queueRender();
    }

    @Override
    public Component setRoot(GameObject root) {
        super.setRoot(root);

        meshMaterialSets.forEach((meshMaterialSet) -> {
            meshMaterialSet.setRoot(root);
            SceneManager.currentScene.addVaoId(meshMaterialSet.getMesh().getVaoID());
        });
        return this;
    }

    private void queueRender() {
        RenderManager.queueRender(this);
    }

    private void dequeueRender() {
        RenderManager.dequeueRender(this);
    }

    private void calculateRadius() {
        float maxRadius = 0;
        for (MeshMaterialSet meshMaterialSet : meshMaterialSets) {
            if(meshMaterialSet.getMesh() == null){
                Debug.logError("Mesh is null");
                return;
            }
            for (Vector3f vertex : meshMaterialSet.getMesh().getVertices()) {
                float distance = vertex.distance(Constants.VECTOR3_ZERO);
                if (distance > maxRadius) {
                    maxRadius = distance;
                }
            }
        }

        getRoot().setRadius(maxRadius);
    }

    public void calculateAABB() {
        Vector3f min = new Vector3f(Float.MAX_VALUE);
        Vector3f max = new Vector3f(-Float.MAX_VALUE);

        for (MeshMaterialSet meshMaterialSet : meshMaterialSets) {
            for (Vector3f vertex : meshMaterialSet.getMesh().getVertices()) {
                min.x = Math.min(min.x, vertex.x);
                min.y = Math.min(min.y, vertex.y);
                min.z = Math.min(min.z, vertex.z);
                max.x = Math.max(max.x, vertex.x);
                max.y = Math.max(max.y, vertex.y);
                max.z = Math.max(max.z, vertex.z);
            }
        }

        GameObject root = getRoot();
        if(root == null) return;

        if (min.length() == Float.MAX_VALUE || max.length() == -Float.MAX_VALUE) {
            root.setAabb(new AABB(new Vector3f(Constants.VECTOR3_ZERO), new Vector3f()));
            return;
        }

        getRoot().setCenter(new Vector3f(min).lerp(max, 0.5f));

        root.setAabb(new AABB(min, max));
    }

    @Override
    public void enable() {
        super.enable();
        queueRender();
    }

    @Override
    public void disable() {
        super.disable();
        dequeueRender();
    }

    @Override
    public void cleanUp() {
        super.cleanUp();
        meshMaterialSets.forEach(mms -> mms.getMesh().cleanUp());
        disable();
    }

    @Override
    public JsonObject serializeToJson() {
        List<String> ignoredKeys = new ArrayList<>();
        ignoredKeys.add("hasInitiated");
        boolean hasMeshPaths = !meshPaths.isEmpty();
        if(hasMeshPaths){
            for (String meshPath : meshPaths) {
                if(meshPath.isBlank()){
                    hasMeshPaths = false;
                    break;
                }
            }
        }
        if(!hasMeshPaths){
            ignoredKeys.add("meshPaths");
            ignoredKeys.add("meshMaterialSets");
        } else {
            List<String> originalPaths = new ArrayList<>(meshPaths);
            meshPaths.clear();
            for (String path : originalPaths) {
                meshPaths.add(toStableReference(path));
            }

            // Also update the meshPath on each MeshMaterialSet's Mesh
            for (MeshMaterialSet mms : meshMaterialSets) {
                String mp = mms.getMesh().getMeshPath();
                if (mp != null && !mp.isBlank()) {
                    mms.getMesh().setMeshPath(toStableReference(mp));
                }
            }
        }

        JsonObject result = JsonHelper.objectToJson(this, ignoredKeys.toArray(new String[0]));

        return result;
    }

    private String toStableReference(String path) {
        if (path == null || path.isBlank()) return path;
        if (path.startsWith(Mesh.BUILTIN_PREFIX)) return path;

        if (ManifestHelper.hasGuid(ManifestHelper.manifestFileType.MODEL, path)) {
            return path;
        }

        String guid = ManifestHelper.getGuidByPath(ManifestHelper.manifestFileType.MODEL, path);
        if (guid != null) return guid;

        return path;
    }

    @Override
    public IJsonSerializable deserializeFromJson(String json) {
        JsonReader jsonReader = Json.createReader(new StringReader(json));
        JsonObject jsonInfo = jsonReader.readObject();
        try {
            JsonHelper.loadVariableIntoObject(this, jsonInfo);
        } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        if(!meshMaterialSets.isEmpty()){
            Set<MeshMaterialSet> tempMms = new HashSet<>(meshMaterialSets);
            meshMaterialSets.clear();

            tempMms.forEach(meshMaterialSetA -> {
                Mesh mesh = meshMaterialSetA.getMesh();
                float uvScale = mesh.getUvScale();
                String meshPath = mesh.getMeshPath();
                Material mat = meshMaterialSetA.material;

                if(meshPath.isBlank()) return;

                Set<MeshMaterialSet> mms = StaticMeshLoader.load(meshPath, mesh.getMeshId(), "");
                if(mms == null || mms.isEmpty()) return;
                mms.forEach(meshMaterialSetB -> {
                    if(uvScale != 1f) meshMaterialSetB.getMesh().setUVScale(uvScale);
                    meshMaterialSetB.setRoot(getRoot());
                    if(mat != MaterialManager.defaultMaterial) meshMaterialSetB.material = mat;
                });
                meshMaterialSets.addAll(mms);
            });
        }


        return this;
    }

    @Override
    public void renderPanel() {
        for (Field field : RenderComponent.class.getDeclaredFields()) {
            if (Modifier.isPrivate(field.getModifiers()) || fieldsToIgnore.contains(field.getName())) continue;

            try {
                field.setAccessible(true);
                Object value = field.get(this);

                if (value == null) continue;

                InfoPanel.drawOption(field, this);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
