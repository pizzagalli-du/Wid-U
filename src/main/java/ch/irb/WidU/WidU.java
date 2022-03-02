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
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;

/**
 * ImageJ plugin implementation of Wid-U transmitted light to pseudofluorescence in wide migration chambers.
 * 
 * This plugin 
 * - upscales and divides an open image into tiles, 
 * - establishes an SSH connection with a server, 
 * - sends the images,
 * - executes a command to perform U-Net segmentation
 * - sends back the images
 * - stiches the tiles into a segmented image 
 * 
 * @author Diego Morone
 */
public class WidU implements PlugIn {

    @Override
	public void run(String arg) {
        // Check for existing configuration, otherwise open settings panel
        Boolean settingsok = Boolean.parseBoolean(Prefs.get("ch.irb.widu.settingsok", "false"));
        if (!settingsok) IJ.runPlugIn(WidUSettings.class.getName(), "");

        // SSH variables
        String hostname = Prefs.get("ch.irb.widu.hostname", "localhost");
        Integer port = Integer.parseInt(Prefs.get("ch.irb.widu.port", "22"));
        String username = Prefs.get("ch.irb.widu.username", "");
        String cachefolder = Prefs.get("ch.irb.widu.cachefolder", "");

        // Tile size in pixels
        Integer[] tilesize = new Integer[]{
            Integer.parseInt(Prefs.get("ch.irb.widu.tilesizex", "10")),
            Integer.parseInt(Prefs.get("ch.irb.widu.tilesizey", "10")) 
        };

        // // Load current image
        // ImagePlus raw = IJ.getImage();

        // // Create blob with all tiles
        // Blob rawblob  = Blob.createBlob(raw, tilesize);
        // String id = rawblob.getID(); // UUID for job

        // Establish SSH connection
        SSHConnection ssh = new SSHConnection(hostname, port, username, cachefolder);
        
        // Debug
        ssh.testsend();

        // // Send blob to cachefolder
        // ssh.sendBlob(rawblob);

        // // Runs segmentation with Tensorflow and waits for completion
        // // TODO: define command
        // String command = "";
        // ssh.exec(id, command);

        // // Get back files
        // Blob segmentedblob = ssh.getremoteBlob(id);

        // // Delete all files for process on server and close connection
        // ssh.deleteremoteBlob(id);
        ssh.disconnect();

        // ImagePlus result = Blob.tileBlob(segmentedblob);
        // result.show();

        // // TODO: macro calls
        // if(!Recorder.record) {
        //     IJ.error("Wid-U", "Command recorder is not running");
        //     return;
        // }
        // Recorder.recordString("");
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