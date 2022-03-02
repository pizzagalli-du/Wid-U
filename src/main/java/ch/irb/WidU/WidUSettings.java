/**************************************************************************
 *
 * Copyright (C) 2022   Paola Antonello, 
 *                      Diego Morone, 
 *                      Marcus Thelen,
 *                      Rolf Krause,
 *                      Diego Ulisse Pizzagalli  
 * 
 *	  Institute for Research in Biomedicine
 *	  Switzerland
*
*    Graduate School for Cellular and Molecular Sciences,
*    University of Bern, Switzerland
 *
 *    Euler Institute, Università della Svizzera Italiana,
 *    Switzerland
 *	
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 **************************************************************************/

package ch.irb.WidU;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.io.OpenDialog;

import java.awt.Font;
/**
 * Dialog for configuring settings for Wid-U plugin.
 *
 * @author Diego Morone
 */
public class WidUSettings implements PlugIn {
    public final String VERSION="0.1.0";

    String hostname;
    Integer port;
    String username ;
    String cachefolder;
    Integer tilesizex;
    Integer tilesizey;
    String auth;

    String path = null;

    @Override
    public void run(String arg) {

        if (showDialog()) {
            if(auth == "RSA key") 
                getRSAPath();
            

            if (testconnection())
                IJ.log("Connection test OK. Settings saved.");

        }

    }

    private boolean testconnection() {
        SSHConnection test = new SSHConnection(hostname, port, username, cachefolder);
        test.disconnect();
        return true;
    }

    /**
     * Show settings dialog and save settings to IJ preferences file
     * 
     * @return true if everything goes fine
     */
    private boolean showDialog() {
        GenericDialog gd = new GenericDialog("Wid-U Settings");
		gd.addMessage("Wid-U v"+VERSION);

        gd.addStringField("Host", Prefs.get("ch.irb.widu.hostname", "localhost"), 30);
        gd.addNumericField("Port", Integer.parseInt(Prefs.get("ch.irb.widu.port", "22")), 0);  
        gd.addStringField("Username", Prefs.get("ch.irb.widu.username", ""), 30);
        gd.addStringField("Cache folder", Prefs.get("ch.irb.widu.cachefolder", ""), 30);
        gd.addNumericField("Tile size x", Integer.parseInt(Prefs.get("ch.irb.widu.tilesizex", "10")), 0);
        gd.addNumericField("Tile size y", Integer.parseInt(Prefs.get("ch.irb.widu.tilesizey", "10")), 0);

        String[] auths = new String[]{"RSA key", "Password"};
        gd.addChoice("Authentication method", auths, Prefs.get("ch.irb.widu.auth", "Password"));

		Font citationFont = new Font("Arial", Font.PLAIN, 10);

		// TODO: add doi 
		gd.addMessage("Please cite Antonello et al., 2022\ndoi: xxxxxxx", citationFont);

        gd.showDialog();

        hostname = gd.getNextString();
        port = (int)gd.getNextNumber();
        username = gd.getNextString();
        cachefolder = gd.getNextString();
        tilesizex = (int)gd.getNextNumber();
        tilesizey = (int)gd.getNextNumber();
        auth = gd.getNextChoice();

        Prefs.set("ch.irb.widu.hostname", hostname);
        Prefs.set("ch.irb.widu.port", Integer.toString(port));
        Prefs.set("ch.irb.widu.username", username);
        Prefs.set("ch.irb.widu.cachefolder", cachefolder);
        Prefs.set("ch.irb.widu.tilesizex", Integer.toString(tilesizex));
        Prefs.set("ch.irb.widu.tilesizey", Integer.toString(tilesizey));
        Prefs.set("ch.irb.widu.auth", auth);

        Prefs.set("ch.irb.widu.settingsok", "true");

        return true;
    }

    /**
     * Get path of RSA key to store in settings
     * 
     * @return true if not canceled
     */
    public void getRSAPath() {

        OpenDialog rsad = new OpenDialog("Specify RSA key");

        path = rsad.getPath();

        if (path != null ) {
            Prefs.set("ch.irb.widu.rsapath", path);
        } else {
            IJ.error("Path not specified. Falling back to password authentication");
            Prefs.set("ch.irb.widu.auth", "Password");
            Prefs.set("ch.irb.widu.rsapath", "");
        }
    }

    /**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads
	 * an image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = WidU.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		ImagePlus image = IJ.openImage("~/test.tif");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
