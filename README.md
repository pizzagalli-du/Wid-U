# Wid-U, tracking of unlabeled cells in wide migration chambers via pseudofluorescence

Struggling in segmenting/tracking cells using only transmitted light?
This tools creates a pseudofluorence that facilitates this task.

If you benefited from it please cite our paper
Tracking unlabeled cancer cells imaged with low resolution in wide migration chambers via U-NET class-1 probability (pseudofluorescence)
Antonello, P., et. al & Pizzagalli DU
J Biol Eng 17, 5 (2023). https://doi.org/10.1186/s13036-022-00321-9

The program works in two modalities
a) Easy way/portable: you can install a macro on Fiji and use google colab (free) to run the deep learning part without installing anything on your computer or server.
b) Installed: you can install the plugin for FIJI and a script on a deep learning enabled machine (server part). This facilitates the analysis or several images.

If you need any help do not hesitate to contact us at pizzad@usi.ch

## Easy way / portable
1. Open Google Colab, and import the file widu_compute.ipynb
2. Copy the macros in the macro folders of fiji
3. In fiji export the images with the provided widu-export macro. This will create a zip file.
4. Upload the zip file in colab and download the processed zip file.
5. Import the processed zip file in FIJI using the widu import macro.
6. DONE

## Installed method

### On client side

1. Open Fiji ImageJ (or download from [htts://fiji.sc](https://fiji.sc))
2. Call updates from Help> Update...
3. Download the binary version of this plugin in the plugins/ folder of Fiji ImageJ
4. Go to Edit> Settings> Wid-U Settings... and set the server parameters

### On server side
Copy the bash and python files on a machine with tensorflow/cuda/anaconda
modify the bash script to load the correct anaconda environment
copy the right ssh keys to enable remote execution
Alternatively, run the provided VirtualBox machine preconfigured with everything

### Once installed you can use it 

1. Open a single image or timelapse video
1. Run Analyze> Wid-U
