/*
 Copyright (c) 2014, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3maze.items;

import com.jme3.app.SimpleApplication;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.influencers.ParticleInfluencer;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import java.util.logging.Logger;
import jme3utilities.MyAsset;

/**
 * Torch item in the Maze Game. Provides illumination.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class Torch
        extends Item {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(Torch.class.getName());
    /**
     * asset path to the "torch" icon asset
     */
    final private static String iconAssetPath = "Textures/map-icons/torch.png";
    /**
     * asset path to the "torch" 3-D model asset
     */
    final private static String modelAssetPath = "Models/items/torch/torch.j3o";
    /**
     * offset of light-source from vertex when free
     */
    final private static Vector3f lightOffset = new Vector3f(3f, 4.65f, 3f);
    /**
     * offset of model from vertex when free
     */
    final private static Vector3f modelOffset = new Vector3f(3f, 2.65f, 3f);
    // *************************************************************************
    // constructors

    /**
     * Instantiate a torch.
     *
     * @param application (not null)
     */
    public Torch(SimpleApplication application) {
        super("torch", application);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the offset of the light-source from the vertex when free.
     *
     * @return new vector in world coordinates
     */
    public Vector3f getLightOffset() {
        return lightOffset.clone();
    }
    // *************************************************************************
    // Item methods

    /**
     * Shift from the left hand to the right hand.
     */
    @Override
    public void shiftLeft() {
        super.shiftLeft();
        playerState.updateTorchLocation();
    }

    /**
     * Shift from the right hand to the left hand.
     */
    @Override
    public void shiftRight() {
        super.shiftRight();
        playerState.updateTorchLocation();
    }

    /**
     * Add to the player's inventory, favoring the left hand.
     */
    @Override
    public void take() {
        boolean success = playerState.takeFavorLeftHand(this);
        if (!success) {
            return;
        }

        success = freeItemsState.remove(this);
        assert success : this;

        playerState.updateTorchLocation();

        System.out.printf("You picked up a %s.%n", getTypeName());
    }

    /**
     * Visualize in inventory.
     *
     * @return asset path of a texture
     */
    @Override
    public String visualizeInventory() {
        return iconAssetPath;
    }

    /**
     * Visualize in the main view.
     *
     * @return new unparented instance
     */
    @Override
    public Spatial visualizeMain() {
        Node node = new Node("torch node");
        node.setLocalTranslation(modelOffset);
        /*
         * Attach the torch model to the node.
         */
        Spatial spatial = assetManager.loadModel(modelAssetPath);
        node.attachChild(spatial);
        ColorRGBA color = new ColorRGBA(0.9f, 0.8f, 0.5f, 1f);
        Material material = MyAsset.createShinyMaterial(assetManager, color);
        spatial.setMaterial(material);
        /*
         * Attach an emitter to the node.
         */
        ParticleEmitter emitter = createEmitter();
        emitter.setLocalTranslation(0f, 1f, 0f);
        node.attachChild(emitter);

        return node;
    }

    /**
     * Visualize in the map view.
     *
     * @return new unparented instance
     */
    @Override
    public Spatial visualizeMap() {
        Spatial icon = mapViewState.loadIcon(iconAssetPath, true);
        return icon;
    }
    // *************************************************************************
    // private methods

    /**
     * Create the emitter for the flame of this torch.
     */
    private ParticleEmitter createEmitter() {
        /*
         * Create material for particles.
         */
        String texturePath = "Effects/Explosion/flame.png";
        Texture texture = MyAsset.loadTexture(assetManager, texturePath);
        Material material =
                MyAsset.createParticleMaterial(assetManager, texture);
        /*
         * Create the emitter.
         */
        ParticleMesh.Type meshType = ParticleMesh.Type.Triangle;
        int maxParticleCount = 25;
        ParticleEmitter emitter = new ParticleEmitter("torch emitter",
                meshType, maxParticleCount);
        /*
         * Configure the emitter.
         */
        ColorRGBA particleEndColor = ColorRGBA.Red;
        emitter.setEndColor(particleEndColor);
        float particleMaxLife = 1f; // seconds
        emitter.setHighLife(particleMaxLife);
        emitter.setImagesX(2);
        emitter.setImagesY(2); // 2x2 texture animation
        emitter.setInWorldSpace(true);
        float particleMinLife = 0.5f; // seconds
        emitter.setLowLife(particleMinLife);
        emitter.setMaterial(material);
        emitter.setSelectRandomImage(true);
        emitter.setShadowMode(RenderQueue.ShadowMode.Off);
        ColorRGBA particleStartColor = new ColorRGBA(1f, 0.9f, 0.3f, 0.3f);
        emitter.setStartColor(particleStartColor);
        /*
         * Configure the influencer.
         */
        ParticleInfluencer influencer = emitter.getParticleInfluencer();
        Vector3f initialVelocity = new Vector3f(0f, 3f, 0f);
        influencer.setInitialVelocity(initialVelocity);
        float percentRandom = 40f;
        influencer.setVelocityVariation(percentRandom / 100f);

        return emitter;
    }
}