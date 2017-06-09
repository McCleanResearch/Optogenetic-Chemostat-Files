package analyzeBioreactorImage2;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.StackStatistics;

public class YeastFluorGetter10xPC extends YeastFluorGetter {
	
	public YeastFluorGetter10xPC(ImageStack roiStack, ImageStack fluorStack, String pathToCSV,int set
			,double[] paParams,double maxEccentricity,String[] processedImagePaths,boolean inputStackIsProcessed){
		super(roiStack,fluorStack,pathToCSV,set,paParams,maxEccentricity,processedImagePaths,inputStackIsProcessed);
	}
	public ImagePlus preProcess(ImagePlus imp){
		IJ.run(imp, "Median...", "radius=3");
		IJ.run(imp, "Subtract Background...", "rolling=50");
		return imp;
	}	
	public ImagePlus makeBinary() {// CJS
		ImagePlus imp=new ImagePlus("",this.roiStack.duplicate());
		IJ.run(imp, "8-bit", "");
		// This workflow is based on http://fiji.sc/Segmentation

		// This code can be easily replaced by code recorded while manually
		// manipulating an image Plugins > Macros> Record... > Record BeanShell
		// This script assumes that rm will be updated (probably by using the
		// Particle Analyzer)
		StackStatistics stats=new StackStatistics(imp);
		int threshold=stats.mode*4;
		IJ.setRawThreshold(imp,threshold,255,"No Update");
		IJ.run(imp, "Convert to Mask", "method=Moments background=Dark black");
		IJ.run(imp, "Options...", "iterations=1 count=1 black do=Nothing");
		IJ.run(imp, "Close-", "stack");
		IJ.run(imp, "Fill Holes", "stack");
		IJ.run(imp, "Despeckle", "stack");
		return imp;
	}
}
