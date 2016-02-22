package org.mjt.appleman.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import org.mjt.appleman.Appleman;

public class HtmlLauncher extends GwtApplication {

        @Override
        public GwtApplicationConfiguration getConfig () {
                return new GwtApplicationConfiguration(1042, 768);
        }

        @Override
        public ApplicationListener createApplicationListener () {
                return new Appleman();
        }
}