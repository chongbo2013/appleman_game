/*
 *      PICKING koodia...:
 * 
@Override
 public void render() {
 if (loading && assets.update())
 doneLoading();
    camController.update();
    Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight());
    Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

    if ((shader != null) && (loading == false)) {
        if (Gdx.input.justTouched()) {
            this.pickRay = cam.getPickRay(Gdx.input.getX(),
                    Gdx.input.getY(), 0, 0, Gdx.graphics.getWidth(),
                    Gdx.graphics.getHeight());
        }

        modelBatch.begin(cam);
        renderContext.begin();
        shader.begin(cam, renderContext);
        for (int i = 0; i < instances.size; i++) {
            this.currentInstance = instances.get(i);

            for (Node node : currentInstance.nodes) {
                checkCurrentInstance(node);
            }

            modelBatch.render(currentInstance, shader);
        }
        modelBatch.end();
        renderContext.end();
    }
}

private void checkCurrentInstance(Node node) {
    if (node.children.size > 0) {
        for (Node current : node.children) {
            checkCurrentInstance(current);
        }
    } else if (node.parts.size > 0) {
        for (NodePart part : node.parts) {
            isMeshIntersected(pickRay, part.meshPart.mesh);
        }
    }
}

private void isMeshIntersected(Ray ray, Mesh mesh) {
    Vector3 intersection = new Vector3();
    float[] vertices = new float[mesh.getNumVertices() * 6];
    short[] indices = new short[mesh.getNumIndices()];
    mesh.getVertices(vertices);
    mesh.getIndices(indices);
    if ((ray != null)
            && (Intersector.intersectRayTriangles(ray, vertices, indices,
                    5, intersection))) {
        log("intersection");
        if (currentInstance != null) {
            currentInstance.setSelected(true);
        }
    }
}
 */
package org.mjt.appleman;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;

public class Scene
{
	public final MyVars vars;
	public PerspectiveCamera camera;
	/**
	 * jos t�m� asetettu, modelit renderoidaan k�ytt�m�ll� t�t� envi� eik�
	 * modelin omaa envi�.
	 */
	public Environment environment = null;
	private Array<Entity> entities = new Array<Entity>();

	public int visibleEntities = 0;

	public Scene(final MyVars mgr)
	{
		this.vars = mgr;
	}

	public void createCamera()
	{
		camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.position.set(0, 150, 0);
		camera.lookAt(Vector3.Zero);
		camera.near = 1f;
		camera.far = 1000;
		camera.update();
	}

	public void addEntity(final Entity ent)
	{
		entities.add(ent);
	}

	public void addEntity(final Array<Entity> ent)
	{
		for (Entity e : ent)
			entities.add(e);
	}

	public Entity getEntity(final int index)
	{
		return entities.get(index);
	}

	// TODO TEST
	public Entity getEntity(final String name)
	{
		for (int q = 0; q < entities.size; q++)
			if (entities.get(q).name.equals(name))
				return entities.get(q);
		return null;
	}

	public void removeEntity(final String name)
	{
		for (int q = 0; q < entities.size; q++)
		{
			if (entities.get(q).name.equals(name))
			{
				entities.removeIndex(q);
				break;
			}
		}
	}

	/**
	 * renderoi frustumissa olevat entityt.
	 */
	public void render()
	{
		visibleEntities = 0;

		vars.modelBatch.begin(camera);

		for (int i = 0; i < entities.size; i++)
		{
			if ((entities.get(i).checkBB && entities.get(i).isVisibleBB(camera, entities.get(i))) // jos bb tai sphere
					// frustumissa, rendaa
					|| entities.get(i).isVisibleSphere(camera, entities.get(i)))
			{
				if (environment != null)
					vars.modelBatch.render(entities.get(i), environment);
				else
					vars.modelBatch.render(entities.get(i), entities.get(i).environment);
				visibleEntities++;
			}
		}
		vars.modelBatch.end();
	}

	/**
	 * palauttaa l�himm�n entityn indexin joka screenX,screenY kohdassa.
	 * <p>
	 * jos checkBB==true, tsekataan onko boundingbox frustumissa, muuten onko
	 * sphere frustumissa.
	 *
	 * @param screenX
	 * @param screenY
	 * @param checkBB
	 * @return
	 */
	public int getEntityIndex(int screenX, int screenY, boolean checkBB)
	{
		Ray ray = camera.getPickRay(screenX, screenY);
		int result = -1;
		float len = 1000000;
		for (int i = 0; i < entities.size; i++)
		{
			if ((checkBB && Intersector.intersectRayBounds(ray, entities.get(i).bounds, MyVars.tmpVector3))
					|| (!checkBB && Intersector.intersectRaySphere(ray, entities.get(i).center, entities.get(i).radius,
					MyVars.tmpVector3)))
			{
				float len2 = MyVars.tmpVector3.len2();
				if (len2 < len)
				{
					len = len2;
					result = i;
				}
			}
		}
		return result;
	}

	public void dispose()
	{
		//TODO for loop ja joka entitys dispose		
		entities.clear();
	}

}
