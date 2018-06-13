# Hyperspace

Plugin coded to allow analysis of lambda stack in ImageJ.

## Usage

In order to use it, first open the lambda stack using BioFormats. 

/!\ This plugin has been tested with CZI files containing metadata with the emission wavelengths.

Then start the plugin. A MIP image will be presented and asking you to do a line selection corresponding to the region of interest.

<img src="images/LambdaStack.png" width="300"> <img src="images/MAX_Ag17_5_Image%20calculator.1_b1_p1ROI1.png" width="300">

<!--![Lambda Stack](images/LambdaStack.png) -->

<!--![Selection](images/MAX_Ag17_5_Image%20calculator.1_b1_p1ROI1.png) --> 

Once done, click OK and you will get the hyperspectral image corresponding to the ROI selected.

<!-- ![Result](images/Hyperspace_Ag17_5_Image%20calculator.1_b1_p1ROI1.png) --> 

<img src="images/Hyperspace_Ag17_5_Image%20calculator.1_b1_p1ROI1.png" width="300">

The X axis will correspond to the wavelengths found in the metadata. The plugin will also save:

* A CSV with the pixel intensities corresponding to the ROI
* A CSV with normalized pixel intensities corresponding to the ROI


[![DOI](https://zenodo.org/badge/75272895.svg)](https://zenodo.org/badge/latestdoi/75272895)
