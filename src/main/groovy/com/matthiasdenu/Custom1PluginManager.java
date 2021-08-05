/*
 * See org.jvnet.hudson.test.TestPluginManager
 *
 * The MIT License
 *
 * Copyright (c) 2010, Kohsuke Kawaguchi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.matthiasdenu;

import hudson.*;
import org.jvnet.hudson.test.WarExploder;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * See org.jvnet.hudson.test.TestPluginManager
 *
 * {@link PluginManager} to speed up unit tests.
 *
 * <p>
 * Instead of loading every plugin for every test case, this allows them to reuse a single plugin
 * manager.
 *
 * <p>
 * TODO: {@link Plugin} start/stop/postInitialize invocation semantics gets different. Perhaps
 *
 * @author Kohsuke Kawaguchi
 * @see HudsonTestCase#useLocalPluginManager
 */
public class Custom1PluginManager extends PluginManager {

  public final static PluginManager INSTANCE;
  private static final Logger LOGGER = Logger.getLogger(Custom1PluginManager.class.getName());

  static {
    try {
      INSTANCE = new Custom1PluginManager();
      Runtime.getRuntime().addShutdownHook(new Thread("delete " + INSTANCE.rootDir) {
        @Override
        public void run() {
          // Shutdown and release plugins as in PluginManager#stop
          ((Custom1PluginManager) INSTANCE).reallyStop();

          // allow JVM cleanup handles of jar files...
          System.gc();

          try {
            Util.deleteRecursive(INSTANCE.rootDir);
          } catch (IOException x) {
            x.printStackTrace();
          }
        }
      });
    } catch (IOException e) {
      throw new Error(e);
    }
  }

  public Custom1PluginManager() throws IOException {
    // CustomPluginManager outlives a Jetty server, so can't pass in ServletContext.
    super(null, Util.createTempDir());
  }

  /**
   * @see LocalPluginManager#loadBundledPlugins
   */
  @Override
  protected Collection<String> loadBundledPlugins() throws Exception {
    try {
      return loadBundledPlugins(new File(WarExploder.getExplodedDir(), "WEB-INF/plugins"));
    } finally {
      try {
        Method loadDetachedPlugins = PluginManager.class.getDeclaredMethod("loadDetachedPlugins");
        loadDetachedPlugins.setAccessible(true);
        loadDetachedPlugins.invoke(this);
      } catch (NoSuchMethodException x) {
        // Jenkins 1.x, fine
      }
    }
  }

  private Set<String> loadBundledPlugins(File fromDir) throws IOException, URISyntaxException {
    Set<String> names = new HashSet<>();

    File[] children = fromDir.listFiles();
    if (children != null) {
      for (File child : children) {
        try {
          names.add(child.getName());

          copyBundledPlugin(child.toURI().toURL(), child.getName());
        } catch (IOException e) {
          LOGGER.log(Level.SEVERE, "Failed to extract the bundled plugin " + child, e);
        }
      }
    } else {
      LOGGER.log(Level.FINE, "No plugins loaded from {0}. Directory does not exist.", fromDir);
    }
    // If running tests for a plugin, include the plugin being tested
    URL u = getClass().getClassLoader().getResource("the.jpl");
    if (u == null) {
      u = getClass().getClassLoader().getResource("the.hpl"); // keep backward compatible
    }
    if (u != null) {
      try {
        String thisPlugin;
        try (InputStream is = u.openStream()) {
          thisPlugin = new Manifest(is).getMainAttributes().getValue("Short-Name");
        }
        if (thisPlugin == null) {
          throw new IOException("malformed " + u);
        }
        names.add(thisPlugin + ".jpl");
        copyBundledPlugin(u, thisPlugin + ".jpl");
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Failed to copy the.jpl", e);
      }
    }

    // and pick up test dependency *.jpi that are placed by maven-hpi-plugin TestDependencyMojo.
    // and copy them into $JENKINS_HOME/plugins.
    URL index = getClass().getResource("/test-dependencies-custom/index");
    if (index != null) {// if built with maven-hpi-plugin < 1.52 this file won't exist.
      try (BufferedReader r = new BufferedReader(
          new InputStreamReader(index.openStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = r.readLine()) != null) {
          final URL url = new URL(index, line + ".jpi");
          File f;
          try {
            f = new File(url.toURI());
          } catch (IllegalArgumentException x) {
            if (x.getMessage().equals("URI is not hierarchical")) {
              throw new IOException(
                  "You are probably trying to load plugins from within a jarfile (not possible). If"
                      + " you are running this in your IDE and see this message, it is likely"
                      + " that you have a clean target directory. Try running 'mvn test-compile' "
                      + "from the command line (once only), which will copy the required plugins "
                      + "into target/test-classes/test-dependencies - then retry your test", x);
            } else {
              throw new IOException(index + " contains bogus line " + line, x);
            }
          }
          // TODO should this be running names.add(line + ".jpi")? Affects PluginWrapper.isBundled & .*Dependents
          if (f.exists()) {
            copyBundledPlugin(url, line + ".jpi");
          } else {
            copyBundledPlugin(new URL(index, line + ".hpi"), line + ".jpi"); // fallback to hpi
          }
        }
      }
    }

    return names;
  }

  // Overwrite PluginManager#stop, not to release plugins in each tests.
  // Releasing plugins result fail to access files in webapp directory in following tests.
  @Override
  public void stop() {
    for (PluginWrapper p : activePlugins) {
      p.stop();
    }
  }

  /**
   * As we don't actually shut down classloaders, we instead provide this method that does what
   * {@link #stop()} normally does.
   */
  private void reallyStop() {
    super.stop();
  }
}
