package jme3maze;

import com.jme3.app.SimpleApplication;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.shadow.PointLightShadowRenderer;
import com.jme3.system.AppSettings;
import jme3utilities.Heart;
import jme3utilities.MyAsset;
import jme3utilities.debug.Dumper;
import jme3utilities.mesh.RectangleMesh;

public class MazeGame extends SimpleApplication {

    final private static Dumper dumper = new Dumper();

    public void dump() {
        dumper.setDumpBucket(true);
        dumper.setDumpCull(true);
        dumper.setDumpMatParam(true);
        dumper.setDumpShadow(true);
        dumper.setDumpTransform(true);
        dumper.setDumpVertex(true);

        dumper.dump(renderManager);
    }

    public static void main(String[] arguments) {
        MazeGame application = new MazeGame();
        Heart.parseAppArgs(application, arguments);

        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL32);
        application.setSettings(settings);

        application.start();
    }

    @Override
    public void simpleInitApp() {
        renderer.setDefaultAnisotropicFilter(8);
        cam.setLocation(new Vector3f(-35f, 5f, 30f));
        cam.lookAtDirection(Vector3f.UNIT_X, Vector3f.UNIT_Y);
        flyCam.setEnabled(false);
        viewPort.setBackgroundColor(ColorRGBA.Blue);
        rootNode.setShadowMode(ShadowMode.CastAndReceive);

        PointLight torch = new PointLight();
        rootNode.addLight(torch);
        torch.setPosition(new Vector3f(3f, 4.65f, 33f));

        float lightIntensity = 1f;
        ColorRGBA pointColor = ColorRGBA.White.mult(lightIntensity);
        torch.setColor(pointColor);
        torch.setName("torch");
        float attenuationRadius = 1000f; // world units
        torch.setRadius(attenuationRadius);

        Material material = MyAsset.createShinyMaterial(assetManager,
                ColorRGBA.White.mult(2f));

        float h = FastMath.sqrt(0.5f);
        Quaternion orientation = new Quaternion(h, 0f, 0f, h);

        RectangleMesh mesh = new RectangleMesh(0f, 1f, 0f, 1f,
                0f, 10f, 0f, 10f, 1f);
        Geometry geometry = new Geometry("", mesh);
        rootNode.attachChild(geometry);
        geometry.setMaterial(material);
        geometry.setLocalTranslation(new Vector3f(-5f, 10f, 25f));
        geometry.setLocalRotation(orientation);

        PointLightShadowRenderer plsr
                = new PointLightShadowRenderer(assetManager, 256);
        plsr.setLight(torch);
        plsr.setShadowIntensity(1f);
        viewPort.addProcessor(plsr);
    }

    static int frameCount = 0;

    public void simpleUpdate(float tpf) {
        ++frameCount;
        if (frameCount == 2) {
            guiNode.detachAllChildren();
            dump();
        }
    }
}
