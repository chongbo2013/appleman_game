package org.mjt.appleman;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.Logger;

public class Appleman extends Game
{
	public static final boolean DESKTOP = true;
	public static final boolean HTML = true;

	MyVars myVars;

	@Override
	public void create()
	{
		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
		Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1);

		myVars = new MyVars();
		myVars.assets.getLogger().setLevel(Logger.DEBUG);

		MyVars.SCREENWIDTH = Gdx.graphics.getWidth();
		MyVars.SCREENHEIGHT = Gdx.graphics.getHeight();

		MenuScreen menuScreen = new MenuScreen(this);
		setScreen(menuScreen);
	}

	@Override
	public void render()
	{
		super.render();
	}

}
