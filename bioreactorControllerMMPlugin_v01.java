
/**
 * 
 * Copyright University of Wisconsin-Madison
 * 
 *               This file is distributed in the hope that it will be useful,
 *               but WITHOUT ANY WARRANTY; without even the implied warranty
 *               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 *               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
 *               
 *               /*
SUMMARY:
This Micro-Manager plugin is intended to control a microscope and to control a microcontroller running the chemostatController.ino file. It tells the microcontroller
what ratio of time peristaltic pumps should be on, and when the sampling pump should be off. It controls the microscope to acquire images. It then
uses ImageJ's software to analyze those images.

INPUT:
1) This program assumes that the positions to images are listed in micromanager's "stage position list."

OUTPUT:
1) Many images
2) A summary.csv file summarizing the experiment
3) A microcontrollerRecords.csv file, containing messages received from the microcontroller
4) A translations.csv file. IT is useful to know the translations that were applied images in order to align them.
REQUIRES:
1) FIJI (or imageJ)
2) Micromanager
3) The users to review the values of the initially declared variables below. The values of certain parameters are hard-coded into the script for each time that this is run.
    
    
    Written by Cameron Stewart
						Research intern at UW-Madison
               cameronstewart92@gmail.com
    Last revised on February 29, 2016
 */
 

package feedbackController2;

import ij.gui.GenericDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import mmcorej.CMMCore;
import mmcorej.StrVector;

import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;

import bsh.EvalError;
import bsh.Interpreter;
import mmcorej.CharVector; 
import org.micromanager.api.PositionList;
import org.micromanager.api.MultiStagePosition;
import org.micromanager.api.StagePosition;
import org.micromanager.utils.ImageUtils;
import org.micromanager.utils.MMScriptException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.System;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import ij.IJ;
import ij.Prefs;
import ij.ImagePlus;
import ij.io.Opener;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.ZProjector;
import ij.process.ImageProcessor;
import ij.ImageStack;
import ij.io.FileSaver;
import ij.WindowManager;  
import ij.text.TextWindow; 
import ij.plugin.Duplicator;
import ij.text.TextPanel;
import ij.plugin.frame.RoiManager;
import ij.plugin.ImageCalculator;


public class bioreactorControllerMMPlugin implements MMPlugin {
   public static final String menuName = "Bioreactor Controller";
   public static final String tooltipDescription =
      "This is used to control a bioreactor and microsocpe";
   private static final String DELIMITER=",";
   private static final int COLUMNS=3;//Number of columns to expect in the timecourse.csv
   private String controllerPort;
   private double controllerInterval;
   private String saveDir;
   private Double mediaRatioOn=.5;
   private Double sampleRatioOn=.5;
   private int bgSamples=5;
   private long settlingTime=1*1000;
   private static final String imageTypeName="Tiff";
   private static final String imageTypeSuffix=".tiff";
   private String timeCourseFile="bioreactorTimecourse.csv";
   private String[][] timeCourse;
   private ImagePlus imp=new ImagePlus();
   private ImagePlus imp2=new ImagePlus();
   private ImagePlus imp3=new ImagePlus();
   private ImagePlus impStack=new ImagePlus();
   private ZProjector Zstack=new ZProjector();
   private ImageStack stack;
   private String[] groups;
   private String[] presets;
   int numPresets=0;
   private String findRoiGroup;
   private String findRoiPreset;
   private String measureRoiGroup;
   private String measureRoiPreset;
   private String darkGroup;
   private String darkPreset;
   private PositionList pl;
   private String analyzeParticlesParameters="size=2000-15000 circularity=0.50-1.00 exclude include add";
   private String lineSeparator="\r\n";
   private String commandTerminator="#";
   String dataFile="Summary";
   String translationFile="Translations";
   String tempFile="Temperature";
   String bgOperationFind="Difference";
   String bgOperationAnalyze="Subtract";
   private int imageNumber=0; //images taken so far
   private int ROIs=0;
   private double curIntensity=-31415.9;
   long imageTime;
   SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-z");
   String startTime;
   boolean messageSent; //This is used by sendImmediateMessage to know if the message was sent successfully or should be resent
   private long startTimeMillis;
   private long startTimeInAcqSet=0;
   private long lastBG;
   private JTextArea jTextArea1=new JTextArea();
   private String[] configPresets;
   private String[] configGroups;
   private String[] BG_operations;
   String [] LED_input= new String[] {"CSV file (window to choose file will appear after clicking \"run\")", "3 fields below"};
   public RoiManager rm;
   public ResultsTable rt = new ResultsTable();
   boolean exportToScript=false;
   String beanshellScript;
   boolean callScriptAfterAnalysis=false;
   String scriptAfterAnalysis;
   boolean reloadTimecourse=false;
   int minimumDelay;
   ImageCalculator ic = new ImageCalculator();
   int iterations=1;
   private GenericDialog gd;
   
   
   
   // Provides access to the Micro-Manager Java API (for GUI control and high-
   // level functions).
   @SuppressWarnings("unused")
private ScriptInterface app_;
   // Provides access to the Micro-Manager Core API (for direct hardware
   // control)
   @SuppressWarnings("unused")
private CMMCore core_;

   @Override
   public void setApp(ScriptInterface app) {
      app_ = app;
      core_ = app.getMMCore();
   }

   @Override
   public void dispose() {
      // We do nothing here as the only object we create, our dialog, should
      // be dismissed by the user.
	   gd.dispose();
	   gd=null;
   }

   @Override
   public void show() {
	if(gd==null){
		gd = new GenericDialog("Bioreactor Controller (BETA)");
		   gd.addStringField("Microcontroller Port", "COM7");
		   gd.addNumericField("Microcontroller interval", 30, 0,8,"seconds");
		   //gd.addStringField("Save images to this directory", file.getAbsolutePath(), 100);
			gd.addNumericField("Media pump ratio on", 0.50, 2);
			gd.addNumericField("Sample pump ratio on", 0.50, 2);
			gd.addNumericField("Background samples", 5, 0, 8, "images");
			gd.addNumericField("Delay before image is taken", 1, 0, 8, "seconds");
	
			//gd.addStringField("CSV file of LED values over time", file.getAbsolutePath()+"\\bioreactorTimecourse.csv", 100);
			gd.addRadioButtonGroup("Get LED input from", LED_input, 1, 2, "3 fields below");
			gd.addStringField("List the number of LEDs of the LED matrix that should be illuminated for each interval as comma seperated values [0-64]", "64,0,32,0,16",80);
			gd.addStringField("List the pulse-width-modulated current to the LED matrix for each interval as comma seperated values [0-15]", "15,0,8,0,4",80);
			gd.addNumericField("Delay between each LED setting", 120, 0,8,"minutes");
	
			
			
			
	
			//gd.addTextAreas("comma seperated PWM", "Comma seperated num LEDs", 1, 50);
			setGroupsAndPresets();
			gd.addChoice("Find ROIs from images in this group", groups, groups[0]);
			gd.addChoice("Find ROIs from images in this preset", presets, presets[0]);
			gd.addChoice("Measure intensity from ROIs in this group", groups, groups[0]);
			gd.addChoice("Measure intensity from ROIs in this group", presets, presets[0]);
			gd.addChoice("Between image acquisitions, remain in this group", groups, groups[0]);
			gd.addChoice("Between image acquisitions, remain at this preset", presets, presets[0]);
			
			String [] analysis= new String[] {"Default (optomized for S. cerrivisiae w/ 40x Phc objective)", "Custom beanshell script"};
			gd.addRadioButtonGroup("Find ROIs and measure their attributes from background subtracted images via", analysis, 1, 2, "Default (optomized for S. cerrivisiae w/ 40x Phc objective)");
			gd.addCheckbox("Call external script after analyzing images (for feedback control)?", false);
			gd.addCheckbox("Reload LED time course settings after analyzing images (for feedback control is values are updated in CSV)?", false);
			gd.addNumericField("Minimum delay between sets of images (first called immediatly before second set of acquisitions if minimum delay has not passed)", 1, 0,8,"minutes");
			gd.addNumericField("Number of iterations of acquiring and analyzing images (Reference: 288 intervals of 5 minutes=1 Day. 72 intervals of 20 minutes=1 day)", 3, 0,8,"iterations");
			gd.setOKLabel("Ok");
			gd.hideCancelButton();
			//gd.addCheckboxGroup(3, 4, toppings, null, null);
	}
		gd.showDialog();
		if (gd.wasCanceled()) 
			return;
		controllerPort=gd.getNextString();
		controllerInterval= gd.getNextNumber();
		//saveDir=gd.getNextString();
		mediaRatioOn=gd.getNextNumber();
		sampleRatioOn=gd.getNextNumber();
		bgSamples=(int) gd.getNextNumber();
		settlingTime=(long)(1000*gd.getNextNumber());//converts seconds to milliseconds
		if(gd.getNextRadioButton().equals("CSV file (window to choose file will appear after clicking \"run\")")){
		    
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("CSV file containing the timecourse values to send to the microcontroller");
		    FileNameExtensionFilter filter = new FileNameExtensionFilter(
		        "comma seperated values", "csv");
		    chooser.setFileFilter(filter);
		    int returnVal = chooser.showOpenDialog(new JFrame());
		    if(returnVal == JFileChooser.APPROVE_OPTION) {
		    		   timeCourse=getValuesFromCSV(chooser.getSelectedFile().getAbsolutePath());
		    }
			gd.getNextString();
			gd.getNextString();
			gd.getNextNumber();
		}else{
			timeCourse=getValuesFromFields(gd.getNextString(), gd.getNextString(),(int) gd.getNextNumber());
		}
		
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("Directory in which images will be saved");
	    int returnVal = chooser.showOpenDialog(new JFrame());
	    if(returnVal == JFileChooser.APPROVE_OPTION) {
	    		   saveDir=chooser.getSelectedFile().getAbsolutePath()+"\\";
	    }
	    
		findRoiGroup=gd.getNextChoice();
		findRoiPreset=gd.getNextChoice();
		measureRoiGroup=gd.getNextChoice();
		measureRoiPreset=gd.getNextChoice();
		darkGroup=gd.getNextChoice();
		darkPreset=gd.getNextChoice();
		exportToScript=gd.getNextRadioButton().equals("Custom beanshell script");
		if(exportToScript){
			JFileChooser chooser2 = new JFileChooser();
			chooser2.setDialogTitle("Beanshell script for alternate image analysis");
		    FileNameExtensionFilter filter2 = new FileNameExtensionFilter(
		        "beanshell script", "bsh");
		    chooser2.setFileFilter(filter2);
		    int returnVal2 = chooser2.showOpenDialog(new JFrame());
		    if(returnVal2 == JFileChooser.APPROVE_OPTION)
		    	beanshellScript=chooser2.getSelectedFile().getAbsolutePath();
		}
		callScriptAfterAnalysis=gd.getNextBoolean();
		if(callScriptAfterAnalysis){
				JFileChooser chooser3 = new JFileChooser();
				chooser3.setDialogTitle("Script to call after image analysis");
			    int returnVal3 = chooser3.showOpenDialog(new JFrame());
			    if(returnVal3 == JFileChooser.APPROVE_OPTION)
			    	scriptAfterAnalysis=chooser3.getSelectedFile().getAbsolutePath();
			} 
		reloadTimecourse=gd.getNextBoolean();
		minimumDelay=(int)gd.getNextNumber();
		iterations=(int)gd.getNextNumber();
		IJ.log("about to set up");
		setup();
		IJ.log("about to prepare the data file");
		prepareDataFile();
		for(int acqSet=0;acqSet<iterations;acqSet++){
			startTimeInAcqSet=System.currentTimeMillis();
			sendImmediateMessage(mediaRatioOn,0,0,0);//stop the pump
			if(reloadTimecourse||acqSet==0)
				loadTimeCourse();
			imageNumber=0; //images taken so far in this set of acquisitions
			ROIs=0;//Regions of Interest found so far in this set of acquisitions
			curIntensity=-31415.9; //set the intensity to something unreasonable, so that it will be apparent if it hasn't changed
			while(startTimeInAcqSet+settlingTime>System.currentTimeMillis())
				try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
			acquireImages();
			sendImmediateMessage(mediaRatioOn,sampleRatioOn,0,0);
			generateBG();
			//getData() is an important method. Processes the images, analyzes them, and saves the data
			String[][] processedImages=processImages();
			if(exportToScript)
				callScript(beanshellScript,processedImages);
			else
				analyzeImagesAndSaveData();
			System.gc();
			while(startTimeInAcqSet+(minimumDelay*1000)>System.currentTimeMillis())
				try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
		}
		JOptionPane.showMessageDialog(null, "Done","ok",JOptionPane.PLAIN_MESSAGE);
		
		/*try {
			core_.snapImage();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			imp=(ImagePlus) core_.getImage();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		imp.show();
*/
		
		//JTable jt=new JTable(4,5);
   }
   
   @Override
   public String getInfo () {
      return "This plugin was made by the McClean Lab at UW-Msdison. It is referenced in the JoVE paper"
      		+ "\"Design and Implementation of an Automated Illuminating, Culturing, and Sampling System for Microbial Optogenetic Applications\""
      		+ "It was written by Cameron Stewart (cstewart7@wisc.edu). Please direct questions or comments to Megan McClean (mmcclean@wisc.edu)";
   }

   @Override
   public String getDescription() {
      return tooltipDescription;
   }
   
   @Override
   public String getVersion() {
      return "1.0";
   }
   
   @Override
   public String getCopyright() {
      return "University of Wisconsin-Madison, 2016";
   }
   private void setGroupsAndPresets(){
		groups=core_.getAvailableConfigGroups().toArray();
		for(int i=0;i<groups.length;i++){
			numPresets=numPresets+core_.getAvailableConfigs(groups[i]).toArray().length;
		}
		presets=new String[numPresets];
		numPresets=0;//set back to zero and use as counter
		for(int i=0;i<groups.length;i++){
			String[] temp=core_.getAvailableConfigs(groups[i]).toArray();
			for(int l=0;l<temp.length;l++){
				presets[numPresets+l]=temp[l];
			}
			numPresets=numPresets+temp.length;
		}
   }
   public void callScript(String scriptName,String[][]sendImages){
		Interpreter bsh2 = new Interpreter();
		try {
			bsh2.set( "images", sendImages);
			bsh2.source(scriptName);
			JOptionPane.showMessageDialog(null, "response is: "+(String)bsh2.get("response"),"ok",JOptionPane.PLAIN_MESSAGE);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (EvalError e) {
				e.printStackTrace();
		}
	}
   
   public String[][] processImages(){
	   String[][] images=new String[pl.getNumberOfPositions()][configPresets.length];
		//PREPARE IMAGES. Subtract background from each image (or other operation) and translate Images
		for (int l=0; l < pl.getNumberOfPositions(); l++) {
			for (int i=0;i<configPresets.length;i++){
				backgroundArithmetic(saveDir+"BG_"+configGroups[i]+"_"+configPresets[i]+"_Position_"+l+imageTypeSuffix,
											saveDir+imageNumber+"_"+configGroups[i]+"_"+configPresets[i]+"_Position_"+l+imageTypeSuffix,
											saveDir+imageNumber+"_"+configGroups[i]+"_"+configPresets[i]+"_Position_"+l+"_BG_Subtracted"+imageTypeSuffix,
											BG_operations[i]);
				images[l][i]=saveDir+imageNumber+"_"+configGroups[i]+"_"+configPresets[i]+"_Position_"+l+"_BG_Subtracted"+imageTypeSuffix;
			}		
			IJ.run("Close All", "");//close all windows
			IJ.run("Clear Results", ""); //clear previous results
			//WARNING: The stack is ordered alphabetically. Insert titles alphabetically ordered. title 1 should precede title2
			alignImages(saveDir+imageNumber+"_"+findRoiGroup+"_"+findRoiPreset+"_Position_"+l+"_BG_Subtracted"+imageTypeSuffix,
					saveDir+imageNumber+"_"+measureRoiGroup+"_"+measureRoiPreset+"_Position_"+l+"_BG_Subtracted"+imageTypeSuffix);
			IJ.run("Close All", "");//close all windows
		}
		return images;
   }
   
   
   public void analyzeImagesAndSaveData(){

			
			//**************************Analyze images************************************//
						
				//ANALYZE IMAGES processAndThreshold fills the ROI manager after creating a binary using code that comes from ImageJ > PlugIns> Macros > Record > Beanshell script
				IJ.run("Clear Results", ""); //clear previous results
				for (int l=0; l < pl.getNumberOfPositions(); l++) {
				int ROIsAtPosition=processAndThreshold(saveDir+imageNumber+"_"+findRoiGroup+"_"+findRoiPreset+"_Position_"+l+"_BG_Subtracted"+imageTypeSuffix,
												saveDir+imageNumber+"_"+findRoiGroup+"_"+findRoiPreset+"_Position_"+l+"_BG_Subtracted_Binary"+imageTypeSuffix);
				IJ.run("Close All", "");//close all windows
				//If there are regions of interest, analyze the following image using the ROIs from the ROI manager. An overlay will be saved on it (which doesn't hange the image)
				if(ROIsAtPosition>0)
					analyzeROIs(saveDir+imageNumber+"_"+measureRoiGroup+"_"+measureRoiPreset+"_Position_"+l+"_BG_Subtracted"+imageTypeSuffix);
				ROIs=ROIs+ROIsAtPosition;
				}

			//**************************Save data************************************//
				
				if(ROIs>0){
					saveResults(saveDir+"Results"+imageNumber+".xls",saveDir+dataFile+startTime+".csv");
				IJ.run("Distribution...", "parameter=Median or=100 and=0-1000");
				IJ.saveAs("PNG", saveDir+"Median Distribution"+imageNumber+".png");
				IJ.run("Distribution...", "parameter=Max or=100 and=0-1000");
				IJ.saveAs("PNG", saveDir+"Max Distribution"+imageNumber+".png");
				IJ.run("Distribution...", "parameter=Mean or=100 and=0-1000");
				IJ.saveAs("PNG", saveDir+"Mean Distribution"+imageNumber+".png");
				IJ.run("Distribution...", "parameter=Area or=180 and=300-8550");
				IJ.saveAs("PNG", saveDir+"Area Distribution"+imageNumber+".png");
				}else{//If processAndThreshold returned no ROIs, there is nothing to do. Update dataFile accordingly
				FileWriter writer;
				try {
					writer = new FileWriter(saveDir+dataFile+startTime+".csv",true);
					writer.write(imageTime+", NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 0");
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				} 
			}
			FileWriter writer;
			try {
				writer = new FileWriter(saveDir+dataFile+startTime+".csv",true);
				writer.write(", "+mediaRatioOn+", "+sampleRatioOn+lineSeparator);
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
	}
   
   
   public void saveResults(String title, String dataFile){
		IJ.saveAs("Results", title);
		TextWindow tw = (TextWindow)WindowManager.getFrame("Results"); 
		 TextPanel tp=tw.getTextPanel();
		 ResultsTable st = tp.getResultsTable(); 
		
		 int n=st.getCounter(); //Assumes that summaries (Mean, SD, Max, Min...) have NOT been added after the last result
		 double[] meanArr=st.getColumnAsDoubles(st.getColumnIndex("Mean"));
		 double[] SDArr=st.getColumnAsDoubles(st.getColumnIndex("StdDev"));
		 double[] medianArr=st.getColumnAsDoubles(st.getColumnIndex("Median"));
		 double[] maxArr=st.getColumnAsDoubles(st.getColumnIndex("Max"));
		 double[] areaArr=st.getColumnAsDoubles(st.getColumnIndex("Area"));
		 double medianMean=StatUtils.percentile(meanArr, 50);
		 double medianSD=StatUtils.percentile(SDArr, 50);
		 double medianMedian=StatUtils.percentile(medianArr, 50);
	 	 double medianMax=StatUtils.percentile(maxArr, 50);
		 double medianArea=StatUtils.percentile(areaArr, 50);
		 double meanMax=StatUtils.mean(maxArr);
		 double meanArea=StatUtils.mean(areaArr);
		 double meanMean=StatUtils.mean(meanArr);
		 double ninetyPMean=StatUtils.percentile(meanArr, 90);
		 double ninetyPMedian=StatUtils.percentile(medianArr, 90);
		 double ninetyPMax=StatUtils.percentile(maxArr, 90);
		 
		 curIntensity=meanMax;

			FileWriter writer;
			try {
				writer = new FileWriter(dataFile,true); 
			writer.write(imageTime+", "+Double.toString(medianMean)+", "+Double.toString(meanMean)+", "+Double.toString(ninetyPMean)+", "+Double.toString(medianSD)+", "
			+Double.toString(medianMedian)+", "+Double.toString(ninetyPMedian)+", "+Double.toString(meanMax)+", "+Double.toString(medianMax)+", "+Double.toString(ninetyPMax)+", "
			+Double.toString(meanArea)+", "+Double.toString(medianArea)+", "+Integer.toString(n)+lineSeparator);
			writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
   
   
   public void backgroundArithmetic(String bg, String image, String saveAs, String method){
		imp.setImage(IJ.openImage(image));
		imp2.setImage(IJ.openImage(bg)); //Background image created by generateBG()
		imp3.setImage(ic.run(method+" create", imp, imp2));
		imp.show();
		imp2.show();
		imp3.show();
		imp.close();
		imp2.close();
		IJ.saveAsTiff(imp3,saveAs);
		imp3.close();
	}	
   
   public void analyzeROIs(String title){//This fills the results table
		rm = RoiManager.getInstance();
		imp.setImage(IJ.openImage(title));
		imp.show();
		IJ.run(imp,"Set Measurements...", "area mean standard min median redirect=None decimal=3");
		rm.runCommand("Measure"); //This will measure the selected imp, which is the one that was just shown.
		rm.moveRoisToOverlay(imp); 
		IJ.saveAs(imp, imageTypeName,title);
		imp.close();
	}
   
   public void alignImages(String title1, String title2){
		IJ.run("Close All", "");
		imp.setImage(IJ.openImage(title1));
		stack.addSlice(imp.getProcessor());
		imp.close();
		imp2.setImage(IJ.openImage(title2));
		stack.addSlice(imp2.getProcessor());
		imp2.close();

		impStack.setImage(new ImagePlus("stack",stack));
		impStack.show();
		
		IJ.run(impStack, "StackReg", "transformation=[Rigid Body]");

		impStack=WindowManager.getCurrentImage();

		stack=impStack.getStack();
		
		imp2.setImage(new ImagePlus("imp2",stack.getProcessor(2)));
		IJ.saveAs(imp2,imageTypeName, title2);

		imp.setImage(new ImagePlus("imp",stack.getProcessor(1)));
		IJ.saveAs(imp,imageTypeName, title1);

		while(stack.getSize()>0)
			stack.deleteLastSlice();
		impStack.close();
		imp.close();
		imp2.close();
	}

   public int processAndThreshold(String title, String saveAs){//CJS

		//This workflow is based on http://fiji.sc/Segmentation
		IJ.run("Close All", "");
		imp.setImage(IJ.openImage(title));
		//This code can be easily replaced by code recorded while manually
		//manipulating an image Plugins > Macros> Record... > Record BeanShell
		//This script assumes that rm will be updated (probably by using the Particle Analyzer)
		IJ.run(imp,"Despeckle", "");
		IJ.run(imp, "Subtract Background...", "rolling=30");
		IJ.run(imp, "Find Edges", "");
		//IJ.run(imp, "Subtract Background...", "rolling=5"); //too slow. not very helpful
//		IJ.setAutoThreshold(imp, "IsoData dark"); //THresholds tends to be too low. Includes too much junk
		IJ.setAutoThreshold(imp, "Moments dark");
		Prefs.blackBackground = true;
		IJ.run(imp, "Convert to Mask", "");

	//remove outliers 20 pixels
	//close twice
		IJ.run(imp, "Remove Outliers...", "radius=10 threshold=50 which=Bright");
		IJ.run(imp, "Options...", "iterations=5 count=1 black do=Nothing");
		IJ.run(imp, "Dilate", "");
		IJ.run(imp, "Options...", "iterations=2 count=1 black do=Nothing");
		IJ.run(imp, "Close-", "");
		IJ.run(imp, "Fill Holes", "");
		IJ.run(imp, "Options...", "iterations=5 count=1 black do=Nothing");
		IJ.run(imp, "Erode", "");
		IJ.run(imp, "Remove Outliers...", "radius=20 threshold=50 which=Bright");

		rm = RoiManager.getInstance();
		if (rm==null) rm = new RoiManager();
		if(rm.getCount()!=0){//Ensure that ROI manager is clear (the RoiManager.reset() function doesn't seem to work)
			rm.runCommand("Deselect");
			rm.runCommand("Delete");
		}
		rm.runCommand("Show All with labels");
		rm.runCommand("Show All");
		//This is configured for the 40X objective
		IJ.run(imp, "Analyze Particles...", analyzeParticlesParameters);//changed from 1215
			//"Analyze Particles..." automatically updates rm
		IJ.saveAsTiff(imp,saveAs);
		imp.close();
			//If nothing was measured, it is non-diagnostic. Update expHistory accordingly
		return rm.getCount();
	}
   
   public void generateBG(){
		//print("The current time is "+sdf.format(new Date()));
		//print("The microscope will now acquire "+bgSamples+" at each of the "+pl.getNumberOfPositions()+" positions specified.");
		//print("The median pixel of the images at each position will be used to create a background image for each position.");
		//print("The background image will then be subtracted from later acquired images so that the difference between the image and the background is emphasized.");
		for(int q=0;q<bgSamples;q++){
			try {core_.setConfig(darkGroup,darkPreset);} catch (Exception e1) {e1.printStackTrace();}
			lastBG=System.currentTimeMillis();
					//Take images
				for (int l=0; l < pl.getNumberOfPositions(); l++) {
					try {
						MultiStagePosition.goToPosition(pl.getPosition(l), core_);} catch (Exception e) {e.printStackTrace();}
					for (int i=0;i<configPresets.length;i++){
						try {
							core_.setConfig(configGroups[i],configPresets[i]);
							core_.waitForSystem();
							Thread.sleep(20); //let vibrations dissipate
							core_.snapImage();
							Thread.sleep(20);//Make sure things are synchronized
							core_.setConfig(darkGroup,darkPreset);
							imp.setProcessor(ImageUtils.makeProcessor(core_, core_.getImage())); 
							IJ.saveAsTiff(imp,saveDir+"BG_"+configGroups[i]+"_"+configPresets[i]+"_Position_"+l+"_Sample_"+q+imageTypeSuffix);
							Thread.sleep(100);//Give time for images to save
							imp.close();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
			}//end for loop
			//Turn off pinch valve (open). Turn on room lights
				
				while(System.currentTimeMillis()<5000+(lastBG+((1-sampleRatioOn)*controllerInterval)*1000)){//*1000 converts seconds to milliseconds
					// While not enough time has passed to ensure that the media has moved since the last set of images were taken.
					//5 seconds + the amount of time that the sampling pump is off
					try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
				}
				
		}
		//Make image from the median pixel of each
		for (int l=0; l < pl.getNumberOfPositions(); l++) {
			for(int i=0;i<configPresets.length;i++){
			IJ.run("Close All", "");//close all windows
			//Here I create an imageStack from the parameters of the first image. The subsequent "for" loop
			//adds the subsequent images as slices in the stack
				if(bgSamples>0){
					imp=IJ.openImage(saveDir+"BG_"+configGroups[i]+"_"+configPresets[i]+"_Position_"+l+"_Sample_"+"0"+imageTypeSuffix);
					ImageStack stack= imp.createEmptyStack();
					stack.addSlice(imp.getProcessor());
					imp.close();
				
					for(int q=1;q<bgSamples;q++){
						imp=IJ.openImage(saveDir+"BG_"+configGroups[i]+"_"+configPresets[i]+"_Position_"+l+"_Sample_"+q+imageTypeSuffix);
						stack.addSlice(imp.getProcessor());
						imp.close();
						}
					impStack.setImage(new ImagePlus("stack",stack));
					Zstack.setImage(impStack);
					Zstack.setMethod(ZProjector.MEDIAN_METHOD);
					Zstack.setStartSlice(1);
					Zstack.setStopSlice(stack.getSize());
					Zstack.doProjection();
					imp = Zstack.getProjection();
					IJ.saveAsTiff(imp,saveDir+"BG_"+configGroups[i]+"_"+configPresets[i]+"_Position_"+l+imageTypeSuffix);
					stack.deleteSlice(stack.getSize());
					impStack.close();
					imp.close();
					}
				}
		}
	}
   
   
   public void sendImmediateMessage(double mediaRatioOn, double sampleRatioOn, int other1, int other2){
		clearMessageBuffer();
		String answer="no answer";
		messageSent=false;
		//immeC indicates the initiation of a message
		String command = "immeC,"+mediaRatioOn+","+sampleRatioOn+","+other1+","+other2; 
		while(!messageSent){
			try {
				core_.setSerialPortCommand(controllerPort, command, commandTerminator); 
				Thread.sleep(500);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				messageSent=true;
				answer="no answer";
				answer = core_.getSerialPortAnswer(controllerPort, commandTerminator);
			} catch (Exception e){
				//print("Exception: " + e.getMessage() + "\n Will resend message.");
				messageSent=false;
			}
		if(answer.substring(0,5).equals("start")){
			//print(answer.substring(5,answer.length()-1)+
			//"<- milliseconds since timecourse began, measured temperature of bioreactor, pwm of heating pad, LEDs ON, pwm of LEDs");
			FileWriter writer;
			try {
				writer = new FileWriter(saveDir+tempFile+".csv",true);
			writer.write("The real time is ,"+sdf.format(new Date()));
			writer.write(",Computer time in milliseconds since the start of the run,"+(System.currentTimeMillis()-startTimeMillis));
			writer.write(",Ardunio time then temp in C then heating pad pwm then number of LEDs ON then LED pwm,"+answer.substring(5,answer.length()-1)+","+lineSeparator);
			writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}else{messageSent=false;}
		}
	}   
   
   @SuppressWarnings("unused")
public void acquireImages(){
		IJ.run("Clear Results", ""); //clear previous results
		IJ.run("Close All", "");//close all windows
		try {
			core_.snapImage();
		//This first image is taken because the command is slow the first time that it is run. 
		//Subsequent calls load the image within 50milliseconds (on the old 32 bit computer it was made for :D )
		//It causes problems if imageJ commands are called immediatly afterwards
		Thread.sleep(500);
		//imageTime is when the image was taken, relative to the start of this script being run, in minutes
		imageTime=((System.currentTimeMillis()-startTimeMillis)/60000);
		
		//*******Take images for each position in the list, in each channel***************//
		
		for (int l=0; l < pl.getNumberOfPositions(); l++) {
			MultiStagePosition.goToPosition(pl.getPosition(l), core_);
			core_.waitForSystem();
			for (int i=0;i<configPresets.length;i++){//This portion needs to be fast to reduce discrepancy between images
				core_.setConfig(configGroups[i],configPresets[i]);
				core_.waitForSystem();
				Thread.sleep(50); //Give time for things to settle	
				core_.snapImage();			
				imp.setProcessor(ImageUtils.makeProcessor(core_, core_.getImage()));  	
				if(true)//this could be changed to false if the analysis in getData was also changed
					IJ.saveAsTiff(imp,saveDir+imageNumber+"_"+configGroups[i]+"_"+configPresets[i]+"_Position_"+l+imageTypeSuffix); 
				else
					IJ.saveAsTiff(imp,saveDir+imageNumber+"_"+configGroups[i]+"_"+configPresets[i]+"_Position_"+l+"_BG_Subtracted"+imageTypeSuffix);
				imp.close();
				}
				core_.setConfig(darkGroup,darkPreset);// Return to configurationpresets[0]. This is useful to ensure that the excitation light is off
			}
		Thread.sleep(100); //Give time to save images
		IJ.run("Close All", "");//close all windows
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
}
   public void prepareDataFile(){
		FileWriter writer;
		try {
			writer = new FileWriter(saveDir+dataFile+startTime+".csv",true);
			writer.write(lineSeparator+lineSeparator);
			writer.write("time (min), Median Mean Intensity, Mean Mean Instensity, 90th Percentile of Means, Median SD of Intensity, Median Median Intensity, 90th Percentile of Medians,"
				+"Mean Max, Median Max, 90th Percentile of Maxes, Mean Area, Median Area, Sample Size, MediaRatio, SampleRatioOn"+lineSeparator);
			writer.close();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
   private String[][] getValuesFromCSV(String filename){
		// This code assumes that the value of the 2nd column of the 2nd row is the number of remaining rows
		//filename is the name of the file that will be opened in the current directory. Ex: bioreactorParameters.csv
		//columns is the number of columns that will be saved as values to the values array. So if there are three
		//columns of data, enter 3 and they will be stored as columns 0-2. 
	   String[][] values = null;
		try {
			//Create the file reader
			BufferedReader fileReader = new BufferedReader(new FileReader(filename));
			//Ignore header
			String line = fileReader.readLine();
			//Get the number of parameters from the second line
			line = fileReader.readLine();
			String[] tokens = line.split(DELIMITER);
			int numValues=Integer.parseInt(tokens[1]);
			values=new String[numValues][COLUMNS];
			for(int iter0=0; iter0<numValues; iter0++) {
				line = fileReader.readLine();
				tokens = line.split(DELIMITER);
				for(int iter1=0; iter1<COLUMNS; iter1++){
					values[iter0][iter1]=tokens[iter1];
					}
				}
				fileReader.close(); //This releases the system resources. Thus, recurrent instantiations
			}
		
		catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Error in CSV file reader","ok",JOptionPane.PLAIN_MESSAGE);
			e.printStackTrace();
			}
		return values;
	}
   private String[][] getValuesFromFields(String LEDs_csv, String pwm_csv, int delay){
	   String[][] values = null;
	   String[] LED_tokens = LEDs_csv.split(DELIMITER);
	   String[] pwm_tokens = pwm_csv.split(DELIMITER);
	   values=new String[LED_tokens.length][COLUMNS];
	   for(int iter0=0; iter0<LED_tokens.length; iter0++) {
		   values[iter0][0]=String.valueOf(delay*(iter0+1));
		   values[iter0][1]=LED_tokens[iter0];
		   values[iter0][2]=pwm_tokens[iter0];
			}
	   	return values;
	   
   }
   public void setup(){
	   try {
		app_.clearMessageWindow();
		app_.closeAllAcquisitions();
		pl = app_.getPositionList();
		sendImmediateMessage(mediaRatioOn,sampleRatioOn,0,0);//start the pumps
		
		// These 4 lines make the image stack. The image is aqcuired so that the ImageStack's width and height
		// can be set. It is later globally available and set to the correct dimensions
		core_.snapImage();			
		imp.setProcessor(ImageUtils.makeProcessor(core_, core_.getImage()));
		stack= imp.createEmptyStack();
		imp.close();
		
		startTimeMillis=System.currentTimeMillis();
		startTime=sdf.format(new Date());
		core_.setConfig(darkGroup,darkPreset);
		
		   
		configPresets=new String[] {findRoiPreset,measureRoiPreset};
		configGroups=new String[] {findRoiGroup,measureRoiGroup};
		BG_operations= new String[]{bgOperationFind,bgOperationAnalyze};

	} catch (MMScriptException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
   }
   public void loadTimeCourse(){
		clearMessageBuffer();
		//print("port is  ->"+controllerPort);
		for (int i=0;i<timeCourse.length;i++){
			messageSent=false;
			while(!messageSent){
					messageSent=true;
					try {
						core_.setSerialPortCommand(controllerPort, "timeC,"+i+","+Arrays.toString(timeCourse[i]), commandTerminator);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						core_.setSerialPortCommand(controllerPort, "query,"+i, commandTerminator);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					String answer="no answer";
				   try {
					answer = core_.getSerialPortAnswer(controllerPort, commandTerminator);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				  // print("Answer is:   ->"+answer);
				   if(!(answer.equals("query,"+Arrays.toString(timeCourse[i])))){
				   	messageSent=false;
				   	try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				   }

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		
		} 
	}
   public void clearMessageBuffer(){
		for(int i=0;i<4;i++){
			try{
				//This clears out the message buffer
				core_.getSerialPortAnswer(controllerPort, commandTerminator);
			}catch (Exception e){}
		}
	}
   public void callScript(String scriptName, ImagePlus imp){
		Interpreter bsh2 = new Interpreter();
		try {
			bsh2.set( "imp", imp);
			bsh2.source(scriptName);
		} catch (EvalError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}
      
}
