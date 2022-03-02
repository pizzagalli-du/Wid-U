/**************************************************************************
 *
 * Copyright (C) 2022   Paola Antonello, 
 *                      Diego Morone, 
 *                      Marcus Thelen,
 *                      Rolf Krause,
 *                      Diego Ulisse Pizzagalli  
 * 
 *	  Institute for Research in Biomedicine,
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

import java.io.File;
import java.util.UUID;
import java.util.Vector;

import ij.ImagePlus;

/**
 * This class creates the object Blob, which is composed of a vector of tiles
 * and an automatically-generated id.
 * 
 * @author Diego Morone
 */
public class Blob {

    private Vector<File> blobtiles;
    private String id;

    // Constructor
    public Blob (Vector<File> blobtiles, String id) {
        this.blobtiles = blobtiles;
        this.id = id;
    }

    public String getID() {
        return id;
    }

    /**
     * 
     * @param raw
     * @param tilesize
     * @return
     */
    public static Blob createBlob(ImagePlus raw, Integer[] tilesize) {

        Blob blob =null;

        // // Determine # of tiles x,y
        // Integer width = raw.getWidth();
        // Integer height = raw.getHeight();

        // Integer ntilesx = width/tilesize[0];
        // Integer ntilesy = height/tilesize[1];

        // // TODO: create positions array for cropping
        // ImagePlus tmp;
        // File tmpfile;

        // // TODO: upscale images

        // // TODO: Iterator?
        // raw.setRoi(x, y, w, h);
        // tmp = raw.crop();

        // // TODO: save tile to filestream as jpg compressed

        // // TODO: convert imageplus to file?
        //blob.add(tmpfile);

        // blob.id = blob.generateID();

        return blob;
    }

    public static ImagePlus tileBlob(Blob segmentedblob) {
        // TODO: create back mosaic

        // TODO: downscale
        return null;
    }

    /**
     * 
     * @param tile
     */
    private void add(File tile) {
        blobtiles.add(tile);
    }

    /**
     * 
     * @return
     */
    private String generateID() {
        String newid = UUID.randomUUID().toString();
        return newid;
    }
}
