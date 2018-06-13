import csv, os, re, sys
import os.path
from ij import IJ,WindowManager, ImagePlus
from ij.plugin import ZProjector
from loci.plugins import BF
from loci.plugins.in import ImporterOptions
from os.path import expanduser
from ij.gui import WaitForUserDialog, Roi, TextRoi, Overlay, Line
from array import *
from ij.process import FloatProcessor, ImageProcessor
from java.awt import Color, Font

###############
## VARIABLES ##
###############

# To loop through the already existing files
num = 1

# Size of the canvas once resized
canvasWidth  = 800
canvasHeight = 988

# X axis variables
fontSize     = 8
tickInterval = 1

###############
## FUNCTIONS ##
###############

def normalizeList(listNormal):
    """Normalize a list using the max and
    the min of that list.

    Parameters
    ----------
    listNormal : list of number
        List of numbers corresponding to the pixels
        intensities along the selection.

    Returns
    -------
    list
        Normalized list of the intensities.

    """
    max_value = max(listNormal)
    min_value = min(listNormal)
    for i in range(len(listNormal)):
        listNormal[i] = round((listNormal[i] - min_value) / (max_value - min_value),2)
    return listNormal


def insertZero(listNormal):
    """Add a zero in front of the list.
    Just allows the "first important" values to not
    touch the borders of the image

    Parameters
    ----------
    listNormal : list of numbers
        Should be the normalized list in order to avoid
        messing with the normalization.

    Returns
    -------
    list
        Input list with an extra zero at the front.

    """
    listNormal.insert(0,0)
    return listNormal

###############
## MAIN CODE ##
###############

# Get the ImagePlus of the current image
imp         = WindowManager.getCurrentImage()
nbrChannels = imp.getNChannels()

# Check if file is lambda stack
# by counting the number of channels
if nbrChannels == 1:
    IJ.showMessage("Not a stack!")
    raise Exception("Not a stack!")

# Get the name of the file with and without
# extension.
title      = imp.getTitle();
shortTitle = os.path.splitext(title)[0]

# Create output folder corresponding
# to image is not existing. Will be
# created in the user home folder.
home   = expanduser("~")
output = os.path.join(home,shortTitle)
if not os.path.exists(output):
    os.mkdir(output)

# Tries to read the EmissionWavelength from
# the metadata. Stops otherwise.
# TODO : Should work fine for CZIs but could be
# improved with default values or min/max values.
emissionWL = [0]
try:
    for x in xrange(1,nbrChannels+1,1):
        WL = int(imp.getNumericProperty("Information|Image|Channel|EmissionWavelength #" + str(x).zfill(2)))
        emissionWL.append(WL)
except ValueError:
    raise Exception("Couldn't find the EmissionWavelength info in the metadata")

# Make the maximum intensity projection
# and show it for user input.
project = ZProjector()
project.setMethod(ZProjector.MAX_METHOD)
project.setImage(imp)
project.doProjection()
projection = project.getProjection()
projection.show()

# Wait for the user to draw a line
# and check if line ROI.
IJ.setTool("line");
myWait = WaitForUserDialog ("waitForUser", "Make a line with the region of interest and press OK")
myWait.show()

roi = projection.getRoi()

# Check if roi is a line
# otherwise exit
if not (roi.isLine()):
    raise Exception("Line selection required")

# Loop through existing files to avoid
# overwriting. Will increment the number
# at the end. Saves the MIP with input.
fileName      = shortTitle + "ROI" + str(num);
overlayOutput = os.path.join(output,"MAX_" + fileName)

while(os.path.isfile(overlayOutput + ".png")):
    num          += 1
    fileName      = shortTitle + "ROI" + str(num)
    overlayOutput = os.path.join(output,"MAX_"+fileName)

hyperstackImage = os.path.join(output,"Hyperstack_"+fileName)
outputCSV = os.path.join(output,fileName)

IJ.run("Add Selection...")

print("Files will be saved in " + output)
IJ.saveAs(projection,"png",overlayOutput)
IJ.selectWindow(title)
IJ.run("Restore Selection")
projection.close()

imp.setRoi(roi)

# Get the pixel intensities along the
# selection.
pixelInt = []
for slice in xrange(1,nbrChannels+1,1):
    imp.setC(slice)
    curPixels = roi.getPixels().tolist()
    pixelInt.append(curPixels)

# Transpose the list to have EmissionWavelength
# in X.
pixelInt2         = ([list(x) for x in zip(*pixelInt)])
normalizedList    = (map(normalizeList,pixelInt2))
normalizedList    = (map(insertZero,normalizedList))
outNormalizedList = normalizedList

# Create the ImagePlus of the hyperspectrum image.
imp = ImagePlus("Hyperspectrum", FloatProcessor(normalizedList))

# Add the EmissionWavelengths as first row.
outNormalizedList.insert(0,emissionWL)
pixelInt2.insert(0,emissionWL)

# Writes 2 CSVs:
# * Pixel intensities
# * Normalized pixel intensities
with open(outputCSV + "_norm.csv", "wb") as f:
    writer = csv.writer(f)
    writer.writerows(outNormalizedList)
f.close()

with open(outputCSV + ".csv", "wb") as f:
    writer = csv.writer(f)
    writer.writerows(pixelInt2)
f.close()

# Resize the output and add LUT, X axis
# and calibration bar.
impProc = imp.getProcessor()
impProc.flipVertical()
impProc = impProc.rotateRight()
impProc.smooth()
impProc.setInterpolationMethod(ImageProcessor.NONE)
impProc = impProc.resize(canvasWidth,canvasHeight)
imp.setProcessor(impProc)
imp.show()

IJ.run(imp, "Rainbow RGB", "");
IJ.run(imp, "Canvas Size...", "width=800 height=1024 position=Top-Center zero");

testROI = Roi(0,canvasHeight,canvasWidth,6)
testROI.setFillColor(Color.WHITE)
imp.setRoi(testROI,True)

overlay = Overlay(testROI)
font = Font("SansSerif",Font.PLAIN,fontSize)

# Write the EmissionWavelengths according
# to the metadata read earlier.
for i in xrange(1,len(pixelInt2[0]),tickInterval):
    pixelSize = (canvasWidth/(len(pixelInt2[0])*1.0))
    calc      = (pixelSize)*(i)
    roi       = TextRoi(calc+(pixelSize*0.2),canvasHeight+10,str(emissionWL[i]),font)
    roi.setStrokeColor(Color(1.00, 1.00, 1.00));
    overlay.add(roi)

imp.setOverlay(overlay)
imp.show()
IJ.run(imp, "Calibration Bar...", "location=[Upper Right] fill=None label=White number=3 decimal=1 font=9 zoom=2 bold overlay");

# Saves the output file.
IJ.saveAs(imp,"png",hyperspaceImage)
