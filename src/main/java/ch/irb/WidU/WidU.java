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
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

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
 * - restores image dimensions and calibrations
 * 
 * @author Diego Morone
 */
public class WidU implements PlugIn {

    int nSlices,nFrames, nChannels;
    int chan, firstZ, lastZ, firstT, lastT;

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
        String command = Prefs.get("ch.irb.widu.command", "");

        // Load current image
        ImagePlus raw = IJ.getImage();
        ImageProcessor rawp = raw.getProcessor();

        if (rawp.getBitDepth() ==24) {
            if (rawp.isGrayscale()) {
                new ImageConverter(raw).convertToGray8();
            } else {
                IJ.error("RGB color images not supported. Please use grayscale or RGB single color images.");
                return;
            }
        }

        // Create blob with all tiles
        Blob blob = new Blob();

        // Upscale and split image in tiles
        try {
            blob.populateBlob(raw);
        } catch (Exception e) {
            IJ.error(e.getMessage());
        }

        // Establish SSH connection
        SSHConnection ssh = new SSHConnection(hostname, port, username, cachefolder);
        
        // Send blob to cachefolder
        ssh.sendBlob(blob);

        // Runs segmentation with Tensorflow and waits for completion
        command  = command.replace(System.getProperty("file.separator"), "/");
        
        if (ssh.exec(blob, command)) { // true if execution works fine

            // Get back files
            ssh.getremoteBlob(blob);

            // Perform tiling and show segmented image
            ImagePlus result = blob.tileSegmentation();
            result.show();

            // Delete all files for process on server and close connection
            ssh.deleteremoteBlob(blob);
        }

        ssh.disconnect();

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