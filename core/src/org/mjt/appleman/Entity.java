package org.mjt.appleman;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;

public class Entity extends ModelInstance
{
	MyVars mgr;
	public String name = "";
	public final Vector3 center = new Vector3();
	public final Vector3 dimensions = new Vector3();
	public final float radius;
	public final BoundingBox bounds = new BoundingBox();
	public Environment environment = null;
	public boolean checkBB = true; // jos true, tsekataan bb frustumiin. jos false, tsekataan sphere frustumiin.

	public AnimationController animation = null;

	/**
	 * luodaan model (ei animoitu!)
	 *
	 * @param model
	 * @param name
	 * @param rootNode
	 * @param mergeTransform
	 * @param environment
	 * @param mgr
	 */
	public Entity(final Model model, final String name, String rootNode, final boolean mergeTransform,
				  final Environment environment, final MyVars mgr)
	{
		super(model, rootNode);
		this.mgr = mgr;
		this.name = name;
		calculateBoundingBox(bounds);
		bounds.getCenter(center);
		bounds.getDimensions(dimensions);
		radius = dimensions.len() / 2f;
		this.environment = environment;
	}

	/**
	 * luodaan animoitu model. jos animName==null, asetetaan eka animaatio
	 * k�ytt��n.
	 *
	 * @param model
	 * @param name
	 * @param environment
	 * @param animName
	 * @param mgr
	 */
	public Entity(final Model model, final String name, final Environment environment, final String animName,
				  final MyVars mgr)
	{
		super(model); // NOTE: animoidulle modelil ei anneta rootNodea
		this.mgr = mgr;
		this.name = name;
		calculateBoundingBox(bounds);
		bounds.getCenter(center);
		bounds.getDimensions(dimensions);
		radius = dimensions.len() / 2f;
		this.environment = environment;

		this.animation = new AnimationController(this);
		for (int q = 0; q < this.animations.size; q++)
			Gdx.app.log("DEBUG", "  Anim " + q + ": " + this.animations.get(q).id);
		if (animName != null)
			this.animation.animate(animName, -1, 1f, null, 0.2f);
		else
			this.animation.animate(this.animations.get(0).id, -1, 1f, null, 0.2f);
	}

	public static Array<Entity> load(final String fileName, final Environment environment, final MyVars mgr)
	{
		Gdx.app.log("DEBUG", "Entity.load " + fileName);

		Array<Entity> ent = new Array<Entity>();

		mgr.assets.load(fileName, Model.class);
		mgr.assets.finishLoading();
		Model model = mgr.assets.get(fileName, Model.class);

		for (int i = 0; i < model.nodes.size; i++)
		{
			String id = model.nodes.get(i).id;
			Entity instance = new Entity(model, id, id, false, environment, mgr);
			ent.add(instance);

			Gdx.app.log(" DEBUG", "  " + i + ": " + id);
		}
		return ent;
	}

	public static Entity loadAnimated(final String fileName, final String name, final Environment environment,
									  final String animName, final MyVars mgr)
	{
		Gdx.app.log("DEBUG", "Entity.loadAnimated " + fileName);
		mgr.assets.load(fileName, Model.class);
		mgr.assets.finishLoading();
		Model model = mgr.assets.get(fileName, Model.class);
		Entity ent = new Entity(model, name, environment, animName, mgr);

		return ent;
	}

	public void render(final Camera camera)
	{
		mgr.modelBatch.begin(camera);
		if (checkBB)
		{
			if (isVisibleBB(camera, this))
				mgr.modelBatch.render(this);
		}
		else
		{
			if (isVisibleSphere(camera, this))
				mgr.modelBatch.render(this);
		}
		mgr.modelBatch.end();
	}

	public boolean isVisibleBB(final Camera cam, final Entity instance)
	{
		instance.transform.getTranslation(MyVars.tmpVector3);
		MyVars.tmpVector3.add(instance.center);
		return cam.frustum.boundsInFrustum(MyVars.tmpVector3, instance.dimensions);
	}

	public boolean isVisibleSphere(final Camera cam, final Entity instance)
	{
		instance.transform.getTranslation(MyVars.tmpVector3);
		MyVars.tmpVector3.add(instance.center);
		return cam.frustum.sphereInFrustum(MyVars.tmpVector3, instance.radius);
	}

	public void setAnimation(final String name)
	{
		if (animation != null)
			animation.animate(name, -1, 1f, null, 0.2f);
	}

	public void updateAnimation(float delta)
	{
		if (animation != null)
			animation.update(delta);
	}

	public static Entity findEntity(String name, Array<Entity> ent)
	{
		for (Entity e : ent)
			if (e.name.equals(name))
				return e;
		return null;
	}

}
