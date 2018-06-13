/* Plugin Hyperspace
 *
 * This macro allows the detection of the wavelength giving the maximum intensity in a region of interest. 
 *
 * In order to make this plugin work, a lambda stack must first be opened in FIJI and then run this plugin.
 * This plugin will run through all the images of the stack in order to make a Maximum Intensity Projection image of the stack and give a better overview of the whole stack.
 * The user will then be asked to make a line across a region of interest on the MIP image. This MIP image and the selection will be saved in a folder on the desktop.
 * This selection will then be applied to the original lambda stack and a profile plot will be made for each image.
 * Profile plots calculate the intensity for each pixel in a selection. 
 * This plot will give the maximum and the minimum intensity for each position in each image allowing a normalization for each image.
 * A hyperspectral image will then be created showing the normalized intensity for each position on the region of interest
 * depending on the wavelength. The Y axis will show the position in the region of interest, the X axis the wavelength.
 * This image will then be smoothed and a rainbow RGB LUT will be applied to it. 
 * A calibration bar will then be added to the image to give meaning to the colors.

 * Publications are important for us. Publications are signs that our work generates results. This enables us to apply for more funds to keep the centre running. 
 * Therefore you MUST acknowledge the CCI facility when you use this macro for a publication, a conference contribution, a grant application etc.
 * 
 * Plugin created by Laurent Guerard, Centre for Cellular Imaging 
 */

//Import list

import ij.*;
import ij.IJ;
import ij.WindowManager;

import ij.gui.Line;
import ij.gui.NewImage;
import ij.gui.Plot;
import ij.gui.ProfilePlot;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;

import ij.plugin.PlugIn;

import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import java.awt.geom.Line2D;

import java.awt.image.BufferedImage;

import java.io.*;

import java.util.Arrays;



public class Hyperspace_ implements PlugIn
{
	double[] lengthROI;
	
	public void run(String arg)
	{
		//Get the image and its processor
		ImagePlus imp = IJ.getImage();
		ImageProcessor ip = imp.getProcessor();

		//Invert the colors, white on black
		IJ.run("Colors...", "foreground=black background=white");
		
		//We get the title of the image in order to access the correct image
		String title = imp.getTitle();
		//Short title in order to get the name without the extension 
		String titleWithoutExtension = imp.getShortTitle();
		//Get the name of the future folder on the desktop
		String userHomeFolder = System.getProperty("user.home")+File.separator+"Desktop"+File.separator+titleWithoutExtension+File.separator;
		//Create the folder
		new File(System.getProperty("user.home")+File.separator+"Desktop"+File.separator+titleWithoutExtension).mkdirs();
		
		//Max intensity projection in order to select the best region
		IJ.run("Z Project...", "projection=[Max Intensity]");
		//Select the max image
		IJ.selectWindow("MAX_"+title);
		//Choose the line tool for the cross-section
		IJ.setTool("line");
		//Ask the user to draw the cross-section
		new WaitForUserDialog("Make a line with the region of interest and press OK").show();
		IJ.run("Add Selection...");

		//Incrementation to save the max intensity image with a number
		int num = 1;
 		String filename = "Max_"+titleWithoutExtension+"ROI"+num;
 		String extension = ".tif";
		
 		//We create the file
		File file = new File(userHomeFolder,filename+extension);

		//Loop while the file exists to avoid rewriting
		while(file.getAbsoluteFile().exists())
		{
			num++;
			filename = "Max_"+titleWithoutExtension+"ROI"+num;
			file = new File(userHomeFolder,filename+extension);
		}

		IJ.run("Flatten");
			
		//Save
		IJ.saveAs("Tiff",userHomeFolder+filename);
		ImagePlus impROI = IJ.getImage();		
		
		//Choose back the stack
		IJ.selectWindow(title);
		//Draw the line on every image of the stack
		IJ.run("Restore Selection");
		//Close the max projection
		impROI.close();
		//Choose back the stack
		IJ.selectWindow(title);
		
		//We get the number of slices and put values to get the minimum and the maximum
		int nSlices = imp.getStackSize();
		double min = 1000, max = 0;

		//Check if the image is a stack
		if (nSlices == 1)
		{
			IJ.error("Stack required");
			return;
		}
		else
		{
			
			double mini = 0, maxi = 4095;

			//Set the 1st slice
			imp.setSlice(1);
			
			//Get the drawn line
 			Roi roi = imp.getRoi();
				int roiType = 0;
				//Check if not null
				if (!(roi==null))
				{
					//Check if ROI is a line
					roiType = roi.getType();
					if (!(roiType==Roi.LINE))
					{
						IJ.error("Line or rectangular selection required.");
						return;
					}
					//Get the number of pixels of the line
					lengthROI = ((Line)roi).getPixels();
				}
			//Get the length of the ROI
			int positions = lengthROI.length;
			//2D array with the values, we add values to make round values
			double[][] valuesDouble = new double[nSlices+5][positions];
			//Arrays.fill(valuesDouble,0);

			/*for (double[] row : valuesDouble)
				Arrays.fill(row,0); */
			
			//Array to get the maximum values in each position
			double[] valuesMax = new double[positions];
			//Array to get the minimum values in each position
			double[] valuesMin = new double[positions];
			//Fill the maximum array with 0, so every value will be more
			Arrays.fill(valuesMax,0);
			//Fill the minimum array with 1000, so every value will be less
			Arrays.fill(valuesMin,1000);
			
			//Create the result image and its processor
			ImagePlus hypergraph = NewImage.createFloatImage("Hyperspectrum",nSlices+5,positions,1,NewImage.FILL_WHITE);
			ImageProcessor IPhyper = hypergraph.getProcessor();

			//The first slice is empty to make it start at 400nm and make if finish at 750nm
			for (int slice = 2; slice <= nSlices+2; slice++)
			{
				imp.setSlice(slice-1);

				//System.out.println("Number of slices : "+(slice+1)+" and number of nbValues : "+nbValues+" and number of positions : "+positions);
				//Get the profile plot with the different values of the cross section
				ProfilePlot pp = new ProfilePlot(imp);
				//We make the mini and maxi values of the profile plot
 				pp.setMinAndMax(mini, maxi);
 				//Get the values of the cross-section in each slice
 				double[] values = pp.getProfile();

 				//System.out.println(Arrays.toString(values));

				//Loop to get the values of each pixel in the cross-section
 				for(int pixel = 0; pixel < positions; pixel++)
 				{
 					//System.out.println("We are in : "+(slice+1)+" and position : "+pixel+". The value is : "+values[pixel]);
 					valuesDouble[slice][pixel]=values[pixel];
 				}
				
			}	

			//CSV file with the different values if necessary. This file will be saved in the same folder.
			BufferedWriter output = null;
			try
			{
				String filename3 = imp.getShortTitle()+"_profiles.csv";
				File file3 = new File(userHomeFolder,filename3);
				output = new BufferedWriter(new FileWriter(file3));

				//These are the wavelengths for which the plot was done
				output.write("421,431,441,450,460,470,479,489,499,509,518,528,538,547,557,567,577,586,596,606,615,625,635,644,654,664,674,683,693,703,712,722"+System.getProperty("line.separator"));

				//Loop through all theses values in the correct order to get the maximum and minimum values
				for (int pixel= 0; pixel < positions; pixel++)
				{
					for (int slice = 2; slice <= nSlices+2; slice++)
					{
						if (valuesDouble[slice][pixel] > valuesMax[pixel])
							valuesMax[pixel] = valuesDouble[slice][pixel];
						if (valuesDouble[slice][pixel] < valuesMin[pixel])
							valuesMin[pixel] = valuesDouble[slice][pixel];
					}
					//System.out.println("For the position : "+pixel+", the maximum value is : "+valuesMax[pixel]);
				}
				
				//Then loop once again to create the image and put the values
				for (int pixel =0; pixel < positions; pixel++)
				{
					for (int slice = 0; slice <= nSlices+5; slice++)
						{	
							//Fill the created slices in order to make it round
							if ((slice <= 1) || (slice > nSlices+2))
							{
								IPhyper.putPixelValue(slice,pixel,0);					
							}
							//"real" slices
							else
							{
								//We put the values normalized for each position
								IPhyper.putPixelValue(slice,pixel,((valuesDouble[slice][pixel]-valuesMin[pixel])/(valuesMax[pixel]-valuesMin[pixel])));
								double pixelValue = (double)((valuesDouble[slice][pixel]-valuesMin[pixel])/(valuesMax[pixel]-valuesMin[pixel]));
								//Two decimals in the CSV file
								String pixelValueString = String.format("%.2f",pixelValue);
								//Add "," between the values
								if((slice+1)!=nSlices)
								{							
									output.append(pixelValueString+",");
								}
								//If it's the last value, then break line
								else
								{
									output.append(pixelValueString+System.getProperty("line.separator"));
								}
							}							
						}
				}
									
				output.flush();
				output.close();
				
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			//Show the created image
			hypergraph.show();

			//Select the created image
			WindowManager.setWindow(WindowManager.getWindow("Hyperspectrum"));
			
			
			//Run the rainbow RGB LUT
 			IJ.run("Rainbow RGB");
 			//Smooth 		
 			IJ.run("Smooth");
 			//Resize it to a size lower than 1024*1024 in order to add things in the image
			IJ.run("Size...", "width=800 height=988 average interpolation=None");
 			//Make room on the right to add the calibration bar	
 			IJ.run("Canvas Size...", "width=1024 height=988 position=Center-Left zero");
 			//Add the calibration bar
 			IJ.run("Calibration Bar...", "location=[Upper Right] fill=None label=White number=3 decimal=1 font=11 zoom=3");
 			//Convert the image to RGB
 			IJ.run("RGB Color");
 			//Change the size of the image to add the scale
 			IJ.run("Canvas Size...", "width=1024 height=1024 position=Top-Center zero");
 			//Buffer the image to add graphics
 			BufferedImage buffered = (BufferedImage) WindowManager.getImage("Hyperspectrum with bar").getImage();
 			Graphics g = buffered.getGraphics();
 			//Array of values to add on the scale
 			int x[] = {400,450,500,550,600,650,700,750};
 			//Color of the scale
 			g.setColor(Color.WHITE);
 			//Font of the number 
 			Font font = new Font(Font.SANS_SERIF, Font.BOLD, 20);
 			g.setFont(font);
 			//Draw the line
 			g.drawLine(0,988,800,988);
 			//Put the values in the correct place
 			for (int i = 0; i<x.length; i++)
 			{
 				int calc = ((int)(800/(x.length-1)))*i;
 				//System.out.println(calc);
 				g.drawString(Integer.toString(x[i]),calc,1015);
 				g.drawLine(calc,988,calc,995);
 			}
 			
 			//Get the RGB file name and the number
 			String filename2 = titleWithoutExtension+"RGB"+num;
			//System.out.println("2 : "+filename2);
			//Create it
			File file2 = new File(userHomeFolder,filename2);

			//Select the new image and save it
			IJ.selectWindow("Hyperspectrum with bar");
 			IJ.saveAs("Tiff",userHomeFolder+File.separator+filename2);
 			//Select the old image that is still open and close it
 			IJ.selectWindow("Hyperspectrum");
 			ImagePlus annoyingImage = IJ.getImage();
 			annoyingImage.changes = false;
 			annoyingImage.close(); 
 			
		}
 			
 			
	}

}

