package jme3maze;

import com.jme3.app.SimpleApplication;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.shadow.PointLightShadowRenderer;

public class MazeGame extends SimpleApplication {

    public static void main(String[] arguments) {
        MazeGame application = new MazeGame();
        application.setShowSettings(false);
        application.start();
    }

    @Override
    public void simpleInitApp() {
        cam.setLocation(new Vector3f(-35f, 5f, 30f));
        cam.lookAtDirection(Vector3f.UNIT_X, Vector3f.UNIT_Y);
        flyCam.setEnabled(false);

        viewPort.setBackgroundColor(new ColorRGBA(0f, 0f, 1f, 0f));
        rootNode.setShadowMode(ShadowMode.CastAndReceive);

        PointLight torch = new PointLight();
        rootNode.addLight(torch);
        torch.setColor(new ColorRGBA(1f, 1f, 1f, 1f));
        torch.setPosition(new Vector3f(3f, 4.65f, 33f));
        torch.setRadius(1000f);

        Material material = new Material(assetManager, Materials.LIGHTING);
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Diffuse", new ColorRGBA(1f, 1f, 1f, 1f));
        material.setColor("Specular", new ColorRGBA(1f, 1f, 1f, 1f));
        material.setFloat("Shininess", 1f);

        Quad mesh = new Quad(10f, 10f);
        Geometry geometry = new Geometry("", mesh);
        rootNode.attachChild(geometry);
        geometry.move(-5f, 10f, 25f);
        geometry.rotate(FastMath.HALF_PI, 0f, 0f);
        geometry.setMaterial(material);

        PointLightShadowRenderer plsr
                = new PointLightShadowRenderer(assetManager, 256);
        plsr.setLight(torch);
        plsr.setShadowIntensity(1f);
        viewPort.addProcessor(plsr);
    }
}
