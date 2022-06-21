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

import ij.*;
import ij.measure.Calibration;
import ij.plugin.HyperStackConverter;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import java.awt.image.*;
import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.UUID;
import javax.imageio.ImageIO;

/**
 * This class creates the object Blob, which is composed of an hashmap of tiles and their ids
 * for the input image, a similar hashmap for the segmented image, an automatically-generated job id
 * and properties related to those images.
 * 
 * @author Diego Morone
 */
public class Blob {

    private HashMap<String, byte[]> blobtiles;
    private HashMap<String, byte[]> segmentedtiles;
    private String id;
    private String title;
    private Calibration calibration;
    private Integer width;
    private Integer height;
    private Integer nFrames;
    private Integer nSlices;
    private Double magnification;
    private Integer dstWidth;
    private Integer dstHeight; 
    private Integer ntilesx;
    private Integer ntilesy;
    final private Integer tilesize = 224; // Based on training tile size
    final private Double referencepixelsize = 0.405; // Based on training pixel size
    

    // Constructor
    public Blob () {
        this.blobtiles = new HashMap<String, byte[]>();
        this.segmentedtiles= new HashMap<String, byte[]>();
        this.id = generateID();
    }

    private String generateID() {
        String newid = "widu-" + UUID.randomUUID().toString();
        return newid;
    }

    /**
     * Simple method to return the associated UUID for a blob
     * 
     * @return String   UUID
     */
    public String getID() {
        return id;
    }

    /**
     * This method return an hashamp of tiles for the raw (unsegmented) image. 
     * Unique names correspond to filenames and are in the form 
     * <stack index>-<x position>-<y position>.png with 4 leading zeros. 
     * For example: 0001-0224-0244.png
     * 
     * @return HashMap<String, byte[]>  hashmap of tiles with their unique name
     */
    public HashMap<String, byte[]> getRawTiles() {
        return blobtiles;
    }

    /**
     * Function upscales the image to a specified pixel size and creates tiles with defined tilesize.
     * Remainder on the left and bottom corners is treated by increasing the canvas size with black pixels.
     * <p>
     * Tiles are then added to the blob
     *  
     * @param raw is the source image
     */
    public void populateBlob(ImagePlus raw) {

        this.calibration = raw.getCalibration();
        this.title = raw.getTitle();

        // Determine # of tiles x,y
        this.width = raw.getWidth();
        this.height = raw.getHeight();
        this.nFrames = raw.getNFrames();
        this.nSlices = raw.getNSlices();

        this.magnification = calibration.pixelWidth/referencepixelsize; 
        this.dstWidth = (int)Math.ceil(magnification*width);
        this.dstHeight = (int)Math.ceil(magnification*height);
        this.ntilesx = ((int)(dstWidth/tilesize))+1;
        this.ntilesy = ((int)(dstHeight/tilesize))+1;

        ImageStack ims = raw.getImageStack();

        for (int j = 1; j <= nFrames; j++ ) {
            for (int i =1; i <= nSlices; i++) {
                // get stack index
                Integer index = raw.getStackIndex(1, i, j);
                ImageProcessor rawp = ims.getProcessor(index);

                // Rescale
                rawp.setInterpolationMethod(ImageProcessor.BILINEAR);
                rawp = rawp.resize(dstWidth, dstHeight);

                //add black pixels on right and bottom to fit in 224x244 format
                rawp = canvasresize(rawp, ntilesx*tilesize, ntilesy*tilesize); 

                // do the crops
                for (int w=0; w < tilesize*ntilesx; w = w + tilesize) {
                    for (int u=0; u < tilesize*ntilesy; u = u + tilesize) {

                        rawp.setRoi(w, u, tilesize, tilesize);
                        ImageProcessor cropped = rawp.crop();

                        BufferedImage croppedImage = cropped.getBufferedImage();

                        this.addRaw(saveAsPNG(croppedImage), index, w, u); //add as PNG to this blob

                    }
                }

            }
        }
    }
        
    private void addRaw(byte[] f, Integer stackindex, Integer xtile, Integer ytile) {
        String name = String.format("%4s-%4s-%4s.png", stackindex, xtile, ytile).replace(" ", "0"); // 0001-0000-0000.png
        blobtiles.put(name, f);
    }

    /**
     * This method stiches together the segmented images of a blob
     * 
     * @return  ImagePlus of the segmented image, with same dimensions and calibration as input image
     * @see     ImagePlus
     */
    public ImagePlus tileSegmentation() {

        // Create an empty stack
        ImageStack outstk = new ImageStack(width, height);

        for (int i =1; i <= nFrames; i++) {
            for (int j= 1; j <= nSlices; j++) {

                Integer stackindex = (i-1)*nSlices + j ;

                // Create back mosaic for this slice
                BufferedImage outbi = new BufferedImage( dstWidth , dstHeight , BufferedImage.TYPE_INT_RGB);
                Graphics2D g = (Graphics2D) outbi.getGraphics();
             
                for (int w=0; w < ntilesx; w++) { 
                    for (int u=0; u < ntilesy; u++) {

                        String key = String.format("%4s-%4s-%4s.png", stackindex, w*tilesize, u*tilesize).replace(" ", "0");
                        Image tile = readSegmentedTile(key);
                       
                        g.drawImage(tile, w * tilesize, u * tilesize, null); // this creates an RGB, but we will convert to 8bit later
                        
                    }
                }

                g.dispose();

                // Downscale slice
                ImagePlus slice = new ImagePlus();
                slice.setImage(outbi);
                ImageProcessor slicep = slice.getProcessor();

                // Throw away the black pixels on right and bottom sides (see populate blob)
                slicep = canvasresize(slicep, dstWidth, dstHeight);

                // Resize
                slicep.setInterpolationMethod(ImageProcessor.BILINEAR);
                slicep = slicep.resize(width, height);
                
                // Add the rescaled slice to stack
                outstk.addSlice(slicep);
                

            }
        }

        // Convert stack to ImagePlus
        ImagePlus out = new ImagePlus("WIDU_"+title, outstk);
        ImageProcessor outp = out.getProcessor();

        // Convert RGB imageplus to 8bit
        if (outp.getBitDepth()==24 && outp.isGrayscale()) {
            new ImageConverter(out).convertToGray8();
        }

        // Set units and dimensions
        if (nSlices * nFrames > 1) {
            out = HyperStackConverter.toHyperStack(out, 1, nSlices, nFrames, "Color");
        }
        out.setCalibration(calibration);

        return out;

    }

    /*
     * Support function for reading pixels of a segmented remote image from a specified hashkey
     * and converting to a java image. 
     * 
     * @param hashkey of tile
     * @return Java RGB image
     */
    private Image readSegmentedTile(String hashkey) {

        Image out = null;

        try {
            if (segmentedtiles.containsKey(hashkey)) {
                InputStream input = new ByteArrayInputStream(segmentedtiles.get(hashkey));
                out = ImageIO.read(input);
            } else throw new IOException("no key found");
        } catch (IOException e) {
            IJ.log(e.getMessage());
        }

        return out;
    }

    /*
     * Support function for converting raw image to PNG,
     * for use with U-Net segmentation
     */
    private static byte[] saveAsPNG(BufferedImage bi) {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            ImageIO.write(bi, "png", bos);
        } catch (IOException e) {
            IJ.error(e.getMessage());
        }

		byte [] data = bos.toByteArray();
		return data;

	}

    /**
     * Add tile from raw image to this blob
     * 
     * @param byte[] array of pixel values
     * @param String name of the tile  
     */
    public void addRawTile (byte[] f, String name ) {
        blobtiles.put(name, f);
    }

    /**
     * Add tile from segmented image to this blob
     * 
     * @param byte[] array of pixel values
     * @param String name of the tile  
     */
    public void addSegmentedTile(byte[] f, String name) {
        segmentedtiles.put(name, f);
    }

    /**
     * From ImageJ/ij/plugin/CanvasResizer.java
     * 
     * @param ipOld image processor of old image
     * @param wNew new image width
     * @param hNew new image height
     * @return image processor with new canvas size
     */
    public ImageProcessor canvasresize(ImageProcessor ipOld, int wNew, int hNew) {
        ImageProcessor ipNew = ipOld.createProcessor(wNew, hNew);
		ipNew.setValue(0.0);
		ipNew.fill();
		ipNew.insert(ipOld, 0, 0);
		return ipNew;

    }
}
