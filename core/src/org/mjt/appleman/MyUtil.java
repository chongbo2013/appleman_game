package org.mjt.appleman;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class MyUtil
{
	public static Mesh createFullScreenQuad()
	{
		float[] verts = new float[20];
		int i = 0;
		verts[i++] = -1; // x1
		verts[i++] = -1; // y1
		verts[i++] = 0;
		verts[i++] = 0f; // u1
		verts[i++] = 0f; // v1

		verts[i++] = 1f; // x2
		verts[i++] = -1; // y2
		verts[i++] = 0;
		verts[i++] = 1f; // u2
		verts[i++] = 0f; // v2

		verts[i++] = 1f; // x3
		verts[i++] = 1f; // y2
		verts[i++] = 0;
		verts[i++] = 1f; // u3
		verts[i++] = 1f; // v3

		verts[i++] = -1; // x4
		verts[i++] = 1f; // y4
		verts[i++] = 0;
		verts[i++] = 0f; // u4
		verts[i++] = 1f; // v4
		Mesh mesh = new Mesh(true, 4, 0, // static mesh with 4 vertices and no indices
				new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
				new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

		mesh.setVertices(verts);
		return mesh;
	}
	// original code by kalle_h
}
