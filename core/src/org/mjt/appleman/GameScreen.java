/*
Appleman (c) mjt, 2016
 -test game


 ukko liikkumaan								#OK
 luodaan maze   								#OK
 ukko random paikkaan kartalla					#OK
 kamera liikkumaan että ukko keskellä ruutua	#OK
 collision detection							#OK
 energiat										#OK
 score text										#OK
 omenoita randomisti							#OK
   omenat skaalautuu pienestä isoks				#OK
 tietyn ajan päästä omena mätänee				#OK
   sitten häviää, skaalautuu pieneks			#OK
 toon shader									#OK  https://kbalentertainment.wordpress
 .com/2013/11/27/tutorial-cel-shading-with-libgdx-and-opengl-es-2-0-using-post-processing/
 kun ottaa omenan, particle effect				#OK
 viholliset kiertelemään mazessa				#OK
 äänet											#OK 


BUGS:
 particles 				(looks different than in particle editor)
 particles + decals  	(depth problems)


 */

package org.mjt.appleman;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g3d.ModelCache;
import org.mjt.path.GridLocation;
import org.mjt.path.GridMap;
import org.mjt.path.GridPath;
import org.mjt.path.GridPathfinding;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class GameScreen implements Screen
{
	final float SIZE = 200;
	final int MAXAPPLES = 150, MAXENEMIES = 50;
	final int MAPWIDTH = 30, MAPHEIGHT = 30;
	final int TEXTURES = 3, SOUNDS = 5;
	final String GETAPPLE = "particles/getapple.pfx";
	final String GETROTAPPLE = "particles/getrotapple.pfx";
	final String ATTACK = "particles/blood.pfx";

	private Appleman main;
	private Scene scene;
	private Entity player;

	Environment environment;
	Mesh fullScreenQuad;

	TileMap map = new TileMap();

	// applet ja rotten applet tänne
	Array<Apple> apples = new Array<Apple>();
	TextureRegion[] textures = new TextureRegion[TEXTURES]; // decal kuvat
	Decal shadowDecal;

	// viholliset
	Array<Enemy> enemies = new Array<Enemy>();

	BitmapFont font;
	FrameBuffer frameBuffer;
	ShaderProgram toonShader;

	ParticleEffect currentEffect;

	Sound sounds[] = new Sound[SOUNDS];

	// ei-desktopilla näytä tatti
	Sprite control1, control2;

	int score = 0, energy = 5;

	public GameScreen(Appleman main)
	{
		this.main = main;
	}

	ModelCache mapModelCache;

	@Override
	public void show()
	{
		if (Appleman.DESKTOP == false)
		{
			control1 = new Sprite(new Texture(Gdx.files.internal("control1.png")));
			control2 = new Sprite(new Texture(Gdx.files.internal("control2.png")));
		}

		scene = new Scene(main.myVars);

		scene.createCamera();
		scene.camera.position.set(0, 100, 100);
		scene.camera.lookAt(0, 100, -100);
		scene.camera.near = 10;
		scene.camera.far = 5000;
		scene.camera.update(true);

		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f));
		environment.add(new DirectionalLight().set(0.5f, 0.5f, 0.5f, 0.0f, -0.9f, 0.4f));
		scene.environment = environment;

		frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
		toonShader = new ShaderProgram(Gdx.files.internal("shaders/toonshader.vertex.glsl"),
				Gdx.files.internal("shaders/toonshader.fragment.glsl"));
		fullScreenQuad = MyUtil.createFullScreenQuad();

		font = new BitmapFont(Gdx.files.internal("fonts/arial-32.fnt"));

		textures[0] = new TextureRegion(new Texture(Gdx.files.internal("apple.png")));
		textures[1] = new TextureRegion(new Texture(Gdx.files.internal("applerot.png")));
		textures[2] = new TextureRegion(new Texture(Gdx.files.internal("shadow.png")));

		// DEBUG: UkkoArmature|Idle
		// DEBUG: UkkoArmature|Walk
		player = Entity.loadAnimated("player.g3db", "player", null, "UkkoArmature|Idle", main.myVars);
		scene.addEntity(player);

		// luodaan kartta
		mapModelCache = new ModelCache();
		mapModelCache.begin();
		map.map = Maze.makeMaze(MAPWIDTH, MAPHEIGHT, false, false);
		Array<Entity> ent = Entity.load("objects.g3db", null, main.myVars);
		for (int x = 0; x < MAPWIDTH; x++)
			for (int y = 0; y < MAPHEIGHT; y++)
			{
				if (map.map[x][y] == ' ') // floor
				{
					Entity e = Entity.findEntity("Floor", ent);
					Entity ee = new Entity(e.model, e.name + x + y, e.name, false, null, main.myVars);
					ee.transform.setTranslation(x * SIZE, 0, y * SIZE);
					mapModelCache.add(ee);
					//scene.addEntity(ee);
				}
				if (map.map[x][y] == '#') // wall
				{
					Entity e = Entity.findEntity("Wall", ent);
					Entity ee = new Entity(e.model, e.name + x + y, e.name, false, null, main.myVars);
					ee.transform.setTranslation(x * SIZE, 0, y * SIZE);
					mapModelCache.add(ee);
					//scene.addEntity(ee);
				}
			}
		mapModelCache.end();

		// laita roinaa karttaan
		int enemiesCount = 0, pl = 0;
		while (true)
		{
			// random paikka kartalla
			int x = Math.abs(MyVars.random.nextInt(MAPWIDTH));
			int y = Math.abs(MyVars.random.nextInt(MAPHEIGHT));

			// jos tyhjä paikka
			if (map.map[x][y] == ' ')
			{
				int rnd = Math.abs(MyVars.random.nextInt(2));

				// ensin aseta pelaaja xy kohtaan
				if (rnd == 0 && pl == 0)
				{
					pl = 1;
					player.transform.setTranslation(x * SIZE, 0, y * SIZE);

					// aseta kamera
					player.transform.getTranslation(MyVars.tmpVector3);
					scene.camera.position.set(MyVars.tmpVector3.x, 1200, MyVars.tmpVector3.z + 500);
					scene.camera.lookAt(MyVars.tmpVector3.x, 0, MyVars.tmpVector3.z);
					scene.camera.update();
				}

				// kun pelaaja on asetettu, aseta vihollinen xy kohtaan (jos ei liian lähellä pelaajaa)
				if (pl == 1 && rnd == 1 && enemiesCount < MAXENEMIES)
				{
					player.transform.getTranslation(MyVars.tmpVector3);
					if (MyVars.tmpVector3.dst(x * SIZE, 0, y * SIZE) < 500)
						continue;

					Enemy e = new Enemy();
					e.ent = Entity.loadAnimated("enemy.g3db", "enemy" + enemiesCount, null, "UkkoArmature|Idle",
							main.myVars);
					e.ent.transform.setTranslation(x * SIZE, 0, y * SIZE);
					e.shadowDecal = Decal.newDecal(100, 100, textures[2], true);
					e.shadowDecal.setRotation(Vector3.Y, Vector3.Z);
					e.shadowDecal.setScale(1, 0.5f);

					enemies.add(e);
					scene.addEntity(e.ent);
					enemiesCount++;
				}

				// kun pelaajan ja vihollisten paikat on asetettu, poistu loopista
				if (pl == 1 && enemiesCount == MAXENEMIES)
					break;
			}
		}
		map.createTileMap(map.map);

		for (int q = 0; q < MAXAPPLES; q++)
			createApple();

		shadowDecal = Decal.newDecal(100, 100, textures[2], true);
		shadowDecal.setRotation(Vector3.Y, Vector3.Z);
		shadowDecal.setScale(1, 0.5f);

		// lataa partikkeliefektit  /// https://github.com/libgdx/libgdx/wiki/3D-Particle-Effects
		main.myVars.pointSpriteBatch.setCamera(scene.camera);
		main.myVars.billboardParticleBatch.setCamera(scene.camera);
		main.myVars.particleSystem.add(main.myVars.pointSpriteBatch);
		main.myVars.particleSystem.add(main.myVars.billboardParticleBatch);
		ParticleEffectLoader.ParticleEffectLoadParameter loadParam = new ParticleEffectLoader
				.ParticleEffectLoadParameter(
				main.myVars.particleSystem.getBatches());
		ParticleEffectLoader loader = new ParticleEffectLoader(new InternalFileHandleResolver());
		main.myVars.assets.setLoader(ParticleEffect.class, loader);
		main.myVars.assets.load(GETAPPLE, ParticleEffect.class, loadParam);
		main.myVars.assets.load(GETROTAPPLE, ParticleEffect.class, loadParam);
		main.myVars.assets.load(ATTACK, ParticleEffect.class, loadParam);
		main.myVars.assets.finishLoading();
		//----

		Gdx.app.log("DEBUG", "Load sounds.");
		sounds[0] = Gdx.audio.newSound(Gdx.files.internal("sounds/start.wav"));
		sounds[1] = Gdx.audio.newSound(Gdx.files.internal("sounds/gameover.wav"));
		sounds[2] = Gdx.audio.newSound(Gdx.files.internal("sounds/getapple.wav"));
		sounds[3] = Gdx.audio.newSound(Gdx.files.internal("sounds/getrotapple.wav"));
		sounds[4] = Gdx.audio.newSound(Gdx.files.internal("sounds/enemy.wav"));
		for (int q = 0; q < 5; q++)
			if (sounds[q] == null)
				Gdx.app.log("DEBUG:", "Error loading sound: " + q);
		Gdx.app.log("DEBUG", "Sounds loaded.");

		sounds[0].play();

		main.myVars.createDecalBatch(scene.camera);
		score = 0;
	}

	void renderParticleEffects(Camera camera)
	{
		main.myVars.modelBatch.begin(camera);
		main.myVars.particleSystem.update(); // technically not necessary for rendering
		main.myVars.particleSystem.begin();
		main.myVars.particleSystem.draw();
		main.myVars.particleSystem.end();
		main.myVars.modelBatch.render(main.myVars.particleSystem);
		main.myVars.modelBatch.end();
	}

	// luo 1 omena
	void createApple()
	{
		while (true)
		{
			// random paikka kartalla
			int x = Math.abs(MyVars.random.nextInt(MAPWIDTH));
			int y = Math.abs(MyVars.random.nextInt(MAPHEIGHT));

			// jos tyhjä paikka
			if (map.map[x][y] == ' ')
			{
				// vähän paikkarandomia
				float xp = MyVars.random.nextFloat() * 80 - 40;
				float yp = MyVars.random.nextFloat() * 80 - 40;

				// vähän kokorandomia
				float scl = MyVars.random.nextFloat() * 40 + 90;

				// aseta omena xy kohtaan
				Decal decal = Decal.newDecal(scl, scl, textures[0], true);
				Apple apple = new Apple();
				apple.name = "Apple";
				apple.decal = decal;

				apple.rotTime = MyVars.random.nextFloat() * 10 + 5;
				apple.decal.setPosition(x * SIZE + xp, 50, y * SIZE + yp);
				apple.scale = 0;
				apple.scaleAdder = 0.5f;

				apples.add(apple);
				break;
			}
		}
	}

	void checkTouch()
	{
		// tatti keskelle
		int AREASIZE = (int) control1.getWidth();
		int joystickX = AREASIZE / 2;
		int joystickY = AREASIZE / 2;

		for (int touch = 0; touch < 2; touch++)
		{
			if (Gdx.input.isTouched(touch))
			{
				int xx = Gdx.input.getX(touch);
				int yy = MyVars.SCREENHEIGHT - Gdx.input.getY(touch);

				if (xx < AREASIZE + 50 && yy < AREASIZE + 50) // joystick
				{
					if (xx < 0)
						xx = 0;
					if (yy < 0)
						yy = 0;
					if (xx > AREASIZE)
						xx = AREASIZE;
					if (yy > AREASIZE)
						yy = AREASIZE;

					joystickX = xx;
					joystickY = yy;
				}
			}
		}
		joystick.x = joystickX - AREASIZE / 2;
		joystick.y = joystickY - AREASIZE / 2;
		control2.setPosition(joystickX - control2.getWidth() / 2, joystickY - control2.getHeight() / 2);
	}

	class Joystick
	{
		public int x = 0, y = 0;
	}

	Joystick joystick = new Joystick();

	boolean update(float delta)
	{
		float x = 0, z = 0, rot = 0;
		boolean walk = false;
		float checkV = 3000 * delta, moveV = 300 * delta;

		boolean up = false, down = false, left = false, right = false;

		// ei-desktopilla tsekataan touch eventit
		if (Appleman.DESKTOP == false)
		{
			checkTouch();
			if (joystick.x < 0)
				left = true;
			if (joystick.x > 0)
				right = true;
			if (joystick.y > 0)
				up = true;
			if (joystick.y < 0)
				down = true;
		}

		if (Gdx.input.isKeyPressed(Input.Keys.UP))
			up = true;
		if (Gdx.input.isKeyPressed(Input.Keys.DOWN))
			down = true;
		if (Gdx.input.isKeyPressed(Input.Keys.LEFT))
			left = true;
		if (Gdx.input.isKeyPressed(Input.Keys.RIGHT))
			right = true;

		if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE))
		{
			dispose();
			MenuScreen menuScreen = new MenuScreen(main);
			main.setScreen(menuScreen);
			return false;
		}

		if (up)
		{
			if (checkMap(0, -checkV))
			{
				walk = true;
				z = -moveV;
				rot = 0;
			}

		}
		if (down)
		{
			if (checkMap(0, checkV))
			{
				walk = true;
				z = moveV;
				rot = 180;
			}
		}
		if (left)
		{
			if (checkMap(-checkV, 0))
			{
				walk = true;
				x = -moveV;
				rot = 90;
			}
		}
		if (right)
		{
			if (checkMap(checkV, 0))
			{
				walk = true;
				x = moveV;
				rot = -90;
			}
		}

		// jos 45 asteen kulmassa
		if (x != 0 && z != 0)
		{
			if (z < 0)
			{
				if (x < 0)
					rot = 45;
				if (x > 0)
					rot = -45;
			}
			if (z > 0)
			{
				if (x < 0)
					rot = 180 - 45;
				if (x > 0)
					rot = 180 + 45;
			}
		}

		if (walk == true)
		{
			player.transform.getTranslation(MyVars.tmpVector3);
			player.transform.setToRotation(0, 1, 0, rot);
			MyVars.tmpVector3.x += x;
			MyVars.tmpVector3.z += z;
			player.transform.setTranslation(MyVars.tmpVector3);
			player.animation.animate("UkkoArmature|Walk", -1, 1f, null, 0.2f);

			// aseta kamera
			scene.camera.position.set(MyVars.tmpVector3.x, 1200, MyVars.tmpVector3.z + 500);
			scene.camera.lookAt(MyVars.tmpVector3.x, 0, MyVars.tmpVector3.z);
			scene.camera.update();

			// tsekataan ollaanko omenan kohdalla
			int c = 0;
			for (Apple a : apples)
			{
				player.transform.getTranslation(MyVars.tmpVector3);
				MyVars.tmpVector3.y = 50; // nosta y samaksi missä decal on (muuten tulee väärä välimatka)
				if (a.scale > 0.3f && a.decal.getPosition().dst(MyVars.tmpVector3) < 50) // jos tarpeeksi lyhyt
				// välimatka omenasta pelaajaan
				{
					if (a.name.contains("Rot"))
					{
						energy--;
						sounds[3].play(); //("sounds/getrotapple.wav"));
						currentEffect = main.myVars.assets.get(GETROTAPPLE, ParticleEffect.class).copy();
						currentEffect.init();
						currentEffect.translate(MyVars.tmpVector3);
						currentEffect.scale(100, 100, 100);
						currentEffect.start();
						main.myVars.particleSystem.add(currentEffect);

					}
					else
					{
						score++;
						sounds[2].play(); //("sounds/getapple.wav"));
						currentEffect = main.myVars.assets.get(GETAPPLE, ParticleEffect.class).copy();
						currentEffect.init();
						currentEffect.translate(MyVars.tmpVector3);
						currentEffect.scale(100, 100, 100);
						currentEffect.start();
						main.myVars.particleSystem.add(currentEffect);
					}

					apples.removeIndex(c);

					break;
				}
				c++;
			}
		}
		else
		{
			player.animation.animate("UkkoArmature|Idle", -1, 1f, null, 0.2f);
		}

		int c = 0;
		for (Apple a : apples)
		{
			a.scale += a.scaleAdder * delta;

			if (a.scale > 1)
				a.scale = 1.0f;
			if (a.scale < 0)
				a.scale = 0;

			// omenat mätänee
			a.time += delta;
			if (a.name.contains("Rot") == false)
			{
				// omena mädäntyy  -  vaihdetaan texture (vain kerran)
				if (a.time >= a.rotTime)
				{
					Decal decal = Decal.newDecal(100, 100, textures[1], true); // rot
					decal.setPosition(a.decal.getPosition());
					a.name = a.name + "Rot";
					a.decal = decal;
					a.time = 0;
				}
			}
			else
			{
				// mädäntynyt omena näkyy hetken aikaa
				if (a.time >= a.rotTime)
				{
					// pienennetään omenaa
					a.scaleAdder = -1f;
					if (a.scale <= 0)
					{
						// hävitä omena
						apples.removeIndex(c);
						return true;
					}
				}
			}

			// eka kasvatetaan omena täyteen kokoonsa, sitten adder nollataan
			if (a.scale > 1.0f)
			{
				a.scale = 1;
				a.scaleAdder = 0;
			}

			c++;
		}

		// joskus tulee uusia omenoita
		if (MyVars.random.nextInt(100) == 0)
			for (int q = 0; q < MyVars.random.nextInt(5) + 5; q++)
				createApple();

		c = 0;
		for (Enemy e : enemies)
		{
			e.walk(map, delta);

			// jos vihollisen lähellä
			player.transform.getTranslation(MyVars.tmpVector3);
			if (e.getPosition().dst(MyVars.tmpVector3) < 50) // jos tarpeeksi lyhyt välimatka
			{
				energy--;
				sounds[4].play(); //("sounds/enemy.wav"));

				currentEffect = main.myVars.assets.get(ATTACK, ParticleEffect.class).copy();
				currentEffect.init();
				MyVars.tmpVector3.y += 80;
				currentEffect.translate(MyVars.tmpVector3);
				currentEffect.scale(100, 100, 100);
				currentEffect.start();
				main.myVars.particleSystem.add(currentEffect);

				scene.removeEntity(e.ent.name);
				enemies.removeIndex(c);
			}
			c++;
		}

		return true;
	}

	float colV = 0;

	@Override
	public void render(float delta)
	{
		// GAME OVER!
		if (energy <= 0)
		{
			boolean esc = false;
			if (Appleman.DESKTOP == false)
			{
				if (Gdx.input.isTouched(0))
				{
					if (Gdx.input.getX() < 100 && Gdx.input.getY() < 100)
						esc = true;

				}
			}

			if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE) || esc)
			{
				dispose();
				MenuScreen menuScreen = new MenuScreen(main);
				main.setScreen(menuScreen);
				return;
			}

			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

			// vain ekalla kerralla lataa isompi fontti
			if (colV == 0)
			{
				font = new BitmapFont(Gdx.files.internal("fonts/arial-32.fnt"));
				sounds[1].play(); //("sounds/gameover.wav"));
			}
			colV += delta;

			float c = (float) Math.sin(colV);
			main.myVars.spriteBatch.begin();
			font.setColor(1.0f, c, c / 2, 1.0f);
			font.draw(main.myVars.spriteBatch, "GAME OVER!", 10, MyVars.SCREENHEIGHT / 2 + 60);
			if (Appleman.DESKTOP)
			{
				font.draw(main.myVars.spriteBatch, "\nScore: " + score + "\n\nPress ESC.", 10,
						MyVars.SCREENHEIGHT / 2 + 60);
			}
			else
			{
				font.draw(main.myVars.spriteBatch, "\nScore: " + score + "\n\nTap the upper left of the screen.", 10,
						MyVars.SCREENHEIGHT / 2 + 60);
			}
			main.myVars.spriteBatch.end();
			return;
		}

		if (update(delta) == false)
			return;

		//---------- rendataan skene framebufferiin
		FrameBuffer dest = frameBuffer;
		dest.begin();
		{
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

			// rendaa eka kartta
			scene.vars.modelBatch.begin(scene.camera);
			scene.vars.modelBatch.render(mapModelCache, scene.environment);
			scene.vars.modelBatch.end();

			// rendaa omenat, varjot, henkilöt
			scene.render();

			for (int i = 0; i < apples.size; i++)
			{
				Decal decal = apples.get(i).decal;
				decal.setScale(apples.get(i).scale);
				decal.lookAt(scene.camera.position, scene.camera.up);
				main.myVars.decalBatch.add(decal);
			}

			// rendaa varjot
			player.transform.getTranslation(MyVars.tmpVector3);
			MyVars.tmpVector3.y += 2;
			MyVars.tmpVector3.z += 20;
			shadowDecal.setPosition(MyVars.tmpVector3);
			shadowDecal.setRotation(Vector3.Y, Vector3.Z);
			shadowDecal.setScale(1, 0.5f);
			main.myVars.decalBatch.add(shadowDecal);
			for (Enemy e : enemies)
			{
				e.ent.transform.getTranslation(MyVars.tmpVector3);
				MyVars.tmpVector3.y += 2;
				MyVars.tmpVector3.z += 20;
				e.shadowDecal.setPosition(MyVars.tmpVector3);
				main.myVars.decalBatch.add(e.shadowDecal);
			}

			main.myVars.decalBatch.flush();

			renderParticleEffects(scene.camera);
		}
		dest.end();

		// framebuffer ruudulle, käytetään shaderia
		FrameBuffer src = dest;
		dest = frameBuffer;
		src.getColorBufferTexture().bind();
		{
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
			toonShader.begin();
			{
				fullScreenQuad.render(toonShader, GL20.GL_TRIANGLE_FAN, 0, 4);
			}
			toonShader.end();
		}
		//----------

		// updates
		player.animation.update(Gdx.graphics.getDeltaTime());

		for (int i = 0; i < enemies.size; i++)
		{
			enemies.get(i).ent.animation.update(Gdx.graphics.getDeltaTime());
		}

		main.myVars.spriteBatch.begin();
		font.setColor(0.2f, 0.5f, 1.0f, 1.0f);
		font.draw(main.myVars.spriteBatch, "Score: " + score, 5, MyVars.SCREENHEIGHT - 5);
		font.draw(main.myVars.spriteBatch, "\nEnergy: " + energy, 5, MyVars.SCREENHEIGHT - 5);

		if (Appleman.DESKTOP == false)
		{
			control1.draw(main.myVars.spriteBatch);
			control2.draw(main.myVars.spriteBatch);
		}

		main.myVars.spriteBatch.end();
	}

	boolean checkMap(float x, float y)
	{
		player.transform.getTranslation(MyVars.tmpVector3);
		int xx = (int) ((MyVars.tmpVector3.x + SIZE / 2 + x) / SIZE);
		int yy = (int) ((MyVars.tmpVector3.z + SIZE / 2 + y) / SIZE);
		if (xx < 0 || yy < 0 || xx > MAPWIDTH - 1 || yy > MAPHEIGHT - 1) // just in case
			return false;

		if (map.map[xx][yy] != ' ')
			return false;

		return true;
	}

	@Override
	public void resize(int width, int height)
	{
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}

	@Override
	public void pause()
	{
	}

	@Override
	public void resume()
	{
	}

	@Override
	public void hide()
	{
	}

	// TäTä EI KUTSUTA AUTOMAATTISESTI joten ite pitää kutsua
	@Override
	public void dispose()
	{
		scene.dispose();

		for (Sound m : sounds)
			m.dispose();

		main.myVars.pointSpriteBatch.resetCapacity();
		main.myVars.billboardParticleBatch.resetCapacity();
		main.myVars.particleSystem.removeAll();

		if (currentEffect != null)
			currentEffect.dispose();
	}

	class Apple
	{
		public String name = "";
		public Decal decal = null;

		public float rotTime; // kun time>=rotTime, omena mätänee
		public float time = 0;

		public float scale = 0;
		public float scaleAdder = 0;
	}

	class Enemy
	{
		final Vector3 tmpVector3 = new Vector3();

		public Entity ent = null;
		public Decal shadowDecal = null;
		public boolean idle = true;

		public Vector3 path[] = null;
		public float posOnPath = 0;

		public Vector3 getPosition()
		{
			return ent.transform.getTranslation(tmpVector3);
		}

		public void walk(TileMap map, float delta)
		{
			if (path == null && idle == true)
				if (MyVars.random.nextFloat() < 0.1f)
					idle = false;
				else
					return;

			if (idle == false)
			{
				// luo viholliselle reitti
				if (path == null)
				{
					posOnPath = 0;

					int x, z;
					x = MyVars.random.nextInt((int) map.map[0].length);
					z = MyVars.random.nextInt((int) map.map.length);
					if (map.map[x][z] == ' ')
					{
						ent.transform.getTranslation(tmpVector3);
						tmpVector3.x /= SIZE;
						tmpVector3.z /= SIZE;

						path = map.getPath(tmpVector3, new Vector3(x, 0, z));
						if (path == null)
						{
							idle = true;
							return;
						}
						else
						{
							ent.animation.animate("UkkoArmature|Walk", -1, 1f, null, 0.2f);
						}

					}
					else
					{
						idle = true;
						path = null;
						return;
					}
				}
			}

			int curpos = (int) posOnPath;
			posOnPath += delta * 2;
			int newpos = (int) posOnPath;

			if (newpos >= path.length - 1) // määränpäässä
			{
				ent.animation.animate("UkkoArmature|Idle", -1, 1f, null, 0.2f);
				idle = true;
				posOnPath = 0;
				path = null;
				return;
			}

			if (curpos != newpos)
			{
				return;
			}

			newpos = curpos + 1;
			if (path[newpos].x < path[curpos].x)
				ent.transform.setToRotation(0, 1, 0, 90);
			if (path[newpos].x > path[curpos].x)
				ent.transform.setToRotation(0, 1, 0, -90);

			if (path[newpos].z < path[curpos].z)
				ent.transform.setToRotation(0, 1, 0, 0);
			if (path[newpos].z > path[curpos].z)
				ent.transform.setToRotation(0, 1, 0, 180);

			tmpVector3.set(path[newpos]);
			tmpVector3.sub(path[curpos]);
			tmpVector3.scl(posOnPath - (int) posOnPath);
			tmpVector3.add(path[curpos]);
			tmpVector3.x *= SIZE;
			tmpVector3.z *= SIZE;
			ent.transform.setTranslation(tmpVector3);
		}
	}

	class TileMap
	{
		public char map[][];
		public GridMap gmap;
		public GridPathfinding path = new GridPathfinding();

		void createTileMap(char map[][])
		{
			this.map = map;
			gmap = new GridMap(map[0].length, map.length);

			for (int x = 0; x < map[0].length; x++)
				for (int y = 0; y < map.length; y++)
				{
					if (map[x][y] == ' ')
						gmap.set(x, y, 1);
					else
						gmap.set(x, y, GridMap.WALL);
				}
		}

		public Vector3[] getPath(Vector3 start, Vector3 end)
		{
			GridLocation st = new GridLocation((int) start.x, (int) start.z, false);
			GridLocation en = new GridLocation((int) end.x, (int) end.z, false);

			GridPath gp = path.getPath(en, st, gmap);
			if (gp == null)
			{
				return null;
			}
			ArrayList<GridLocation> list = gp.getList();

			Vector3 vp[] = new Vector3[list.size() + 1];
			int c = 0;
			vp[c++] = new Vector3(start.x, 0, start.z);
			for (GridLocation l : list)
			{
				vp[c++] = new Vector3(l.getX(), 0, l.getY());
			}
			return vp;
		}

	}
}
