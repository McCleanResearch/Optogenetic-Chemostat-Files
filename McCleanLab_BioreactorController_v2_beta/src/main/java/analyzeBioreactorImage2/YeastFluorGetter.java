package analyzeBioreactorImage2;


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.io.RoiEncoder;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/*This object requires:
 * 
 * an image by which the ROIs will be found, such as a phase contrast image
 * an image at which the ROIs should be measured
 * a pathToSaveResults to write the measurements to a CSV file
 * a particle analyzer to analyze them with
 * Access to the cvMatch_Template plugin when using the "Align slices in stack..." command
 * 
 */

public class YeastFluorGetter{

	public ImageStack roiStack;
	public ImageStack binStack;//binary
	public ImageStack fluorStack;
	public String pathToSaveResults;
	public ParticleAnalyzer pa;
	public ResultsTable rt;
	public double maxEccentricity=3;
	public int set=0;
	public String[] pathToSaveProcessedImages;
	public boolean inputRoiStackIsProcessed;
	
	/*
	 * "null" is a valid input for any of the objects except the third boolean parameter.
	 *  Moreover, it is perfectly reasonable to call this with the non-ImagePlus
	 * objects as null.
	 * 
	 * If the input string is null, the pathToSaveResults will be set to
	 * a "YeastFluorGetter<yyyy-MM-dd_HH-mm-ss-z>.csv file in the 
	 * current working directory.
	 */
	public YeastFluorGetter(ImageStack roiStack, ImageStack fluorStack, String pathToSaveResults,int set
			,double[] paParams,double maxEccentricity,String[] processedImagePaths,boolean inputStackIsProcessed){
		/*
		 * "null" is a valid but bad input for the first two stacks.
		 * I can't imagine when it would ever be useful.
		 * 
		 * If pathToSaveResults is null, it will be set to
		 * a "YeastFluorGetter<yyyy-MM-dd_HH-mm-ss-z>.csv file in the 
		 * current working directory. 
		 */
		if(roiStack==null)
			roiStack=new ImageStack();
		if(fluorStack==null)
			fluorStack=new ImageStack();
		if(pathToSaveResults==null){
			Path currentRelativePath = Paths.get("");
			String s = currentRelativePath.toAbsolutePath().toString();
			SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-z");
			pathToSaveResults=s+File.separator+"YeastFluorGetter"+sdf.format(new Date())+".csv";
		}
		this.roiStack=roiStack;
		this.fluorStack=fluorStack;
		this.pathToSaveResults=pathToSaveResults;
		this.set=set;
		if(paParams==null){
			this.pa=getDefaultParticleAnalyzer();
		}else{
			this.pa=getParticleAnalyzer(paParams[0],paParams[1],paParams[2],paParams[3]);
		}
		this.maxEccentricity=maxEccentricity;
		this.pathToSaveProcessedImages=processedImagePaths;
		this.inputRoiStackIsProcessed=inputStackIsProcessed;
		/* 
		 * resultsTable is allowed to be null. It's checked later because
		 * I don't want to assign its default value until immediately
		 * before it is used. This enables me to pick a default value with
		 * the most up-to-date information
		 */
	}
	public List<Roi> analyzeAndGetResults(){
		/*
		 * Preprocess the roiStack if it hasn't been done already.
		 * Then, if I have a filePath for each image in the stack,
		 * save each image in the stack at the corresponding filePath.
		 */
		prepareStacks();
		saveProcessedStack();
		return getResults();
	}
	
	public void prepareStacks(){
		if(!this.inputRoiStackIsProcessed){
			this.roiStack=processRoiStack();
			fluorStack=processFluorStack(fluorStack);
			//Note, the second parameter is translated to match the first parameter
			//I want the ROI stack to be translated, because it is then saved as a 
			//processed stack
			alignImages(this.fluorStack,this.roiStack);
			
		}
		this.binStack=makeBinary().getImageStack();
	}
	
	public void saveProcessedStack(){
		ImagePlus roiImg=new ImagePlus("roi img",this.roiStack);
		//Converting from 16 bit to 8 bit cuts the file size in half, saving memory
		IJ.run(roiImg, "8-bit", "");
		//I need to reassign the processor. The imagePlus address does not change,
		//but the imageProcessor address does
		this.roiStack=roiImg.getImageStack();
		if(this.pathToSaveProcessedImages.length==this.roiStack.size()){
			for(int i=0;i<this.pathToSaveProcessedImages.length;i++){
				//the first processor is retrieved with getProcessor(1)
				ImageProcessor tmpProc=roiStack.getProcessor(i+1).duplicate();
				ImagePlus tmpImg=new ImagePlus("",tmpProc);
				IJ.save(tmpImg,this.pathToSaveProcessedImages[i]);
			}
		}
	}

	public ParticleAnalyzer getParticleAnalyzer(double minArea, double maxArea, double minCircularity, double maxCircularity) {
		IJ.run("Set Measurements...",
				"area mean standard modal min centroid center perimeter "
						+ "bounding fit shape feret's integrated median skewness kurtosis area_fraction "
						+ "stack redirect=None decimal=3");
		// These settings don't affect how it is shown once the dialog is used,
		// but they're written here in case this should later be implemented
		// without a dialog
		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.ADD_TO_MANAGER
				+ ParticleAnalyzer.INCLUDE_HOLES + ParticleAnalyzer.SHOW_NONE,
				Analyzer.getMeasurements(), ResultsTable.getResultsTable(),
				minArea, maxArea,  minCircularity,  maxCircularity);
		return pa;
	}
	
	//This stacks edits the contents of the parameters it is given. It replaces
	//each stack position with an aligned image
	   public void alignImages(ImageStack sourceStack, ImageStack translatedStack){
		   IJ.showStatus("Aligning images");
			for(int i=1;i<=sourceStack.getSize();i++){
			    ImagePlus sourceImg=new ImagePlus("imp1",sourceStack.getProcessor(i));
				ImageStack alignedStack= sourceImg.createEmptyStack();
				alignedStack.addSlice(sourceImg.getProcessor());
				alignedStack.addSlice(translatedStack.getProcessor(i));
				ImagePlus impStack=new ImagePlus("stack",alignedStack);
				int imageWidth=sourceImg.getWidth();
				int imageHeight=sourceImg.getHeight();
				int x0=(int) (imageWidth*0.05);
				int windowWidth=(int) (imageWidth*0.9);
				int y0=(int) (imageWidth*0.05);
				int windowHeight= (int) (imageHeight*0.9);
				String cmd="Align slices in stack...";
				String args="method=5 windowsizex="+Integer.toString(windowWidth)+" windowsizey="
						+Integer.toString(windowHeight)+" x0="+Integer.toString(x0)+" y0="+Integer.toString(y0)
						+" swindow=0 subpixel=false itpmethod=0 ref.slice=1 show=true";
				
				IJ.run(impStack, cmd, args);
				
				alignedStack=impStack.getImageStack();
				translatedStack.setProcessor(alignedStack.getProcessor(2), i);
			}
		}
	
	public ParticleAnalyzer getDefaultParticleAnalyzer() {
		IJ.run("Set Measurements...",
				"area mean standard modal min centroid center perimeter "
						+ "bounding fit shape feret's integrated median skewness kurtosis area_fraction "
						+ "stack redirect=None decimal=3");
		// These settings don't affect how it is shown once the dialog is used,
		// but they're written here in case this should later be implemented
		// without a dialog
		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.ADD_TO_MANAGER
				+ ParticleAnalyzer.INCLUDE_HOLES + ParticleAnalyzer.SHOW_NONE,
				Analyzer.getMeasurements(), ResultsTable.getResultsTable(),
				100, 10000,  .5,  1);
		return pa;
	}
	
	public ImageStack processRoiStack(){
		return processRoiStack(this.roiStack);
	}
	
	public ImageStack processRoiStack(ImageStack roiStack){
		ImageStack processedStack=new ImageStack(roiStack.getWidth(),roiStack.getHeight());
		for(int i=1;i<=roiStack.size();i++){
			ImageProcessor tmpProc=roiStack.getProcessor(i);
			ImagePlus tmpImg=new ImagePlus("",tmpProc);
			tmpImg=preProcess(tmpImg);
			processedStack.addSlice(tmpImg.getProcessor());
		}
		ImagePlus tmpStk=new ImagePlus("",processedStack);
		IJ.run(tmpStk, "8-bit", "");
		return tmpStk.getStack();
	}
	
	public ImageStack processFluorStack(ImageStack fluorStack){
		ImageStack processedStack=new ImageStack(fluorStack.getWidth(),fluorStack.getHeight());
		for(int i=1;i<=fluorStack.size();i++){
			ImageProcessor tmpProc=fluorStack.getProcessor(i);
			ImagePlus tmpImg=new ImagePlus("",tmpProc);
			IJ.run(tmpImg, "Despeckle", "");
			processedStack.addSlice(tmpImg.getProcessor());
		}
		ImagePlus tmpStk=new ImagePlus("",processedStack);
		IJ.run(tmpStk, "8-bit", "");
		return tmpStk.getStack();
	}
	
	public ImagePlus preProcess(ImagePlus imp){
		IJ.run(imp, "Despeckle", "");
		IJ.run(imp, "Subtract Background...", "rolling=50");
		return imp;
	}
	
	public ImagePlus makeBinary(){
		return makeBinary(this.roiStack.duplicate());
	}
	
	/*
	 * This method takes an image and converts it to a binary.
	 * It is very likely to be tweaked and re-tweaked as the
	 * acquisition methods (phase contrast vs. DIC, 10x vs. 40x ...) or 
	 * microbe of interest change.
	 * 
	 * It takes an ImagePlus and returns an 8-bit ImagePlus
	 * binary
	 */
	public ImagePlus makeBinary(ImageStack imStk) {// CJS
		ImagePlus imp=new ImagePlus("",imStk);
		IJ.run(imp, "8-bit", "");
		// This workflow is based on http://fiji.sc/Segmentation

		// This code can be easily replaced by code recorded while manually
		// manipulating an image Plugins > Macros> Record... > Record BeanShell
		// This script assumes that rm will be updated (probably by using the
		// Particle Analyzer)

		IJ.setAutoThreshold(imp, "Moments dark stack");
		IJ.run(imp, "Convert to Mask", "method=Moments background=Dark black");
		IJ.run(imp, "Despeckle", "stack");
		IJ.run(imp, "Options...", "iterations=1 count=1 black do=Nothing");
		
		IJ.run(imp, "Dilate", "stack");
		IJ.run(imp, "Close-", "stack");
		IJ.run(imp, "Dilate", "stack");
		IJ.run(imp, "Close-", "stack");
		IJ.run(imp, "Fill Holes", "stack");
		IJ.run(imp, "Options...", "iterations=2 count=1 black do=Nothing");
		IJ.run(imp, "Erode", "stack");
		//IJ.run(imp, "Despeckle", "stack");
		IJ.run(imp, "Remove Outliers...", "radius=20 threshold=50 which=Bright stack");
		IJ.run(imp, "Options...", "iterations=2 count=1 black do=Nothing");
		IJ.run(imp, "Erode", "stack");

		return imp;
	}

	
	/*
	 *  This is very likely to be tweaked and re-tweaked.
	 *  
	 *  It takes a binary image and a particle analyzer, 
	 *  and it returns an ROI manager containing ROIs found
	 *  by the particle analyzer
	 *  
	 *  Warning! Calling this method will clear a 
	 *  pre-existing ROI manager of its ROIs
	 */
	public RoiManager findCells(ImageProcessor binProc, ParticleAnalyzer pa) {// CJS
		RoiManager rm = RoiManager.getInstance();
		if (rm == null)
			rm = new RoiManager();
		if (rm.getCount() != 0) {// Ensure that ROI manager is clear (the
									// RoiManager.reset() function doesn't seem
									// to work)
			rm.runCommand("Deselect");
			rm.runCommand("Delete");
		}

		// This explicitly sets the RoiManager that the ParticleAnalyzer
		// implicitly uses
		ParticleAnalyzer.setRoiManager(rm);
		
		
		pa.analyze(new ImagePlus("",binProc));
		IJ.run("Clear Results", "");
		return rm;
	}
	
	/*
	 * Measure the properties of the image for each ROI in the RoiManager.
	 * Return a results table of the measurements.
	 * 
	 * Warning! This does not set what the measurement variables should be.
	 * They are set as as FIJI global constants which can be modified like in the 
	 * following example (used in the default settings of this object):	
	 * 
	 * IJ.run("Set Measurements...",
				"area mean standard modal min centroid center perimeter "
						+ "bounding fit shape feret's integrated median skewness kurtosis area_fraction "
						+ "stack redirect=None decimal=3");
	*
	*WARNING! If a null value for the ResultsTable was sent in,
	*then this will take the default one, clear it, and then use it. 
	*Ensure that there is not important information in the 
	*default table
	 */
	public List<Roi> getResults() {
		List<Roi> rois=new LinkedList<Roi>();
		for(int i=1;i<=this.binStack.size();i++){
			RoiManager rm=findCells(this.binStack.getProcessor(i),this.pa);
			Roi[] roiArr=rm.getRoisAsArray();
			for(Roi roi:roiArr){
				//gets [max caliper distance, angle, min caliper distance, feretX, feretY] 
				double[] feretVals=roi.getFeretValues();
				if(feretVals[0]/feretVals[2]<=this.maxEccentricity){
					roi.setPosition(i);
					rois.add(roi);
				}
			}
			if(this.rt==null){
				this.rt=ResultsTable.getResultsTable();
			}
			
			//I'm deleting each row instead of using rt.reset() because 
			//rt.reset() triggers a save dialog
			IJ.run("Clear Results", "");
			//this.rt.reset();
			rm.runCommand(new ImagePlus("",this.fluorStack.getProcessor(i)), "Measure");
			//Ensure that this.rt is pointing to the ResultsTable used by the measure command.
			//This re-assignment seems redundant, but without it I didn't get a results table with headers.
			this.rt=ResultsTable.getResultsTable();
			if(i==1)
				writeHeaders();
			saveResults(this.set,i-1);
			IJ.run("Clear Results", "");
			
		}
		return rois;
	}
	public ResultsTable getResultsTable(){
		return this.rt;
	}
	
	public void writeHeaders(){
		writeHeaders(this.pathToSaveResults,this.rt);
	}
	
	public void writeHeaders(String pathToSaveResults, ResultsTable rt){
		try {
			FileWriter writer = new FileWriter(pathToSaveResults, false);
			writer.write("row," + Arrays.toString(rt.getHeadings())
					+ ", Position,Acquisition Set\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void saveResults(){
		saveResults(0,0);
	}
	public void saveResults(int set,int pos){
		saveResults(this.rt,this.pathToSaveResults,set,pos);
	}
	
	//This saves the results to a CSV file in a standardized format
	public void saveResults(ResultsTable rt,String savePath,int set,int pos){
		for (int row = 0; row < rt.getCounter(); row++) {
			try{
				FileWriter writer = new FileWriter(pathToSaveResults, true);
				writer.write(rt.getRowAsString(row).replaceAll("\t", ",") + ",");
				writer.write(Integer.toString(pos)+","+Integer.toString(set)+ "\n");
				//writer.write(String.format("%05d", pos)+","+
					//	String.format("%05d", set)+ "\n");
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public ImageStack getBinary(){
		return this.binStack;
	}
	
	//Old, working version
	/*
	public static RoiManager saveRois(List<Roi> list, String filePath){
		RoiManager rm=RoiManager.getInstance();
		if (rm == null)
			rm = new RoiManager();
		if (rm.getCount() != 0) {// Ensure that ROI manager is clear (the
									// RoiManager.reset() function doesn't seem
									// to work)
			rm.runCommand("Deselect");
			rm.runCommand("Delete");
		}
		rm.runCommand(IJ.getImage(),"Show None");
		double counter=0;
		IJ.showStatus("Loading ROIs to manager...");
		for(Roi roi:list){
			rm.addRoi(roi);
			counter++;
			if(counter%50==0)
				IJ.showProgress(counter/list.size());
		}
		IJ.showProgress(1);
		rm.runCommand("Save", filePath);
		return rm;
	}
	*/
	//Beta version
	public static boolean saveRois(List<Roi> list, String filePath){
		DataOutputStream out = null;
		try {
			ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)));
			out = new DataOutputStream(new BufferedOutputStream(zos));
			RoiEncoder re = new RoiEncoder(out);
			double counter=0;
			IJ.showStatus("Saving "+Integer.toString(list.size())+" ROIs");
			for (Roi roi:list) {
				if(counter%50==0)
					IJ.showProgress(counter/list.size());
				counter++;
				if (roi==null) continue;
				String label=roi.getName()+"-"+Integer.toString((int)counter);
				if (!label.endsWith(".roi")) label += ".roi";
				zos.putNextEntry(new ZipEntry(label));
				re.write(roi);
				out.flush();
			}
			out.close();
		} catch (IOException e) {
			IJ.error(""+e);
			return false;
		} finally {
			if (out!=null)
				try {out.close();} catch (IOException e) {return false;}
		}
		return true;
	}
}






















