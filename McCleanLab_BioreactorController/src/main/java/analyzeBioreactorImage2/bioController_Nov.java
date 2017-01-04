
package analyzeBioreactorImage2;

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
REQUIRES:
1) FIJI (or imageJ)
2) Micromanager
3) The users to review the values of the initially declared variables below. The values of certain parameters are hard-coded into the script for each time that this is run.
    
    
    Written by Cameron Stewart
						Research intern at UW-Madison
               cameronstewart92@gmail.com
    Last revised on February 29, 2016
 */
 



import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.VirtualStack;
import ij.gui.Roi;
import ij.plugin.ImageCalculator;
import ij.plugin.ZProjector;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.swing.JOptionPane;

import mmcorej.CMMCore;

import org.micromanager.api.MMPlugin;
import org.micromanager.api.MultiStagePosition;
import org.micromanager.api.PositionList;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.ImageUtils;
import org.micromanager.utils.MMScriptException;

import bsh.EvalError;
import bsh.Interpreter;

import com.jogamp.common.util.InterruptSource.Thread;



public class bioController_Nov implements MMPlugin {
   public static final String menuName = "Bioreactor Controller 2.5";
   public static final String tooltipDescription =
      "This is used to control a bioreactor and microsocpe";
  
   public ParametersGetter brSettings;
   public PositionList pl;
   private int acqSet=0; //images taken so far
   private long startTimeMillis=0;
   private long startTimeInAcqSet=0;
   public String startTime;
   public VirtualStack[] virtualStackOfBgSubAcquiredImages=new VirtualStack[2];
   public ImagePlus[] virtualImpOfBgSubAcquiredImages=new ImagePlus[2];
   public VirtualStack processedBgSubRoiStack;
   public ImagePlus processedBgSubRoiStackImg;
   public final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-z");
   public final String IMAGE_OF_INTEREST="";
   public final String BG_IMAGE="BG";
   public final String SYNTHESIZED_BG="synthesizedBG";
   public final String BG_SUBTRACTED_IMAGE="backgroundSubtracted";
   public final String BINARY_IMAGE="binary";
   public final String LINE_SEPARATOR="\r\n";
   public final String COMMAND_TERMINATOR="#";
   public final String MICROCONTROLLER_RECORDS="_microcontrollerRecords";
   public final String ANALYSIS_RESULTS="_analysisResults";
   public final Boolean ROISTACKISPROCCESSED=new Boolean(false); //THe stack will never be pre-processed from the perspective of this class



   
   // Provides access to the Micro-Manager Java API (for GUI control and high-
   // level functions).
private ScriptInterface app_;
   // Provides access to the Micro-Manager Core API (for direct hardware
   // control)
private CMMCore core_;

   @Override
   public void setApp(ScriptInterface app) {
      app_ = app;
      core_ = app.getMMCore();
   }

   @Override
   public void dispose() {
	   System.gc();
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
      return "University of Wisconsin-Madison, 2017";
   }

@SuppressWarnings("unchecked")
@Override
   public void show() {
	   if(!getParameters())
		   return;
		setup();
		IJ.log("\\Clear"); //empties the Log 
		IJ.log("About to begin acquiring images!"); 
		//-------------------Main loop: Collect and analyze images-------------------------------------
		while(this.acqSet<brSettings.getIterations()){
			this.startTimeInAcqSet=System.currentTimeMillis();
			//--------------Acquire images of settled cells--------------
			/* First, the sampling pump is stopped so that the fluid is the microfluidic
			 * stops flowing. If this is the first set of images to be collected or if
			 * the LED matrix instructions should be updated dynamically, then upload
			 * the instructions to the LED matrix. Then, the program delays to give additional
			 * time for the microbes to settle to a uniform focal plane. Now that the 
			 * microbes have settled, acquire images at every position in the position list
			 * and in every channel described in the acqGroups and acqPresets String arrays in
			 * the ParametersGetter object. (Acq is short for acquisition).
			 */
			sendImmediateMessage(brSettings.getMediaPumpRatio(),0);
			if(brSettings.isReloadTimecourse()||acqSet==0)
				loadTimeCourse();
			long timeToSettle=(long) (startTimeInAcqSet+brSettings.getSettlingDelay()-System.currentTimeMillis());
			if(timeToSettle>0)
				delay(timeToSettle);
			acquireImages(this.pl,this.acqSet,this.IMAGE_OF_INTEREST,0,this.brSettings,this.core_);
			
			//--------------Acquire images of flowing cells--------------
			/*Now, before we can get images of more settled cells, we want to pump cell 
			 * culture from the bioreactor to the microfluidic, so that we take images 
			 * of fresh cells. However, we can use this time intelligently by acquiring
			 * several images of the cells flowing by. These are the background images.
			 * Supposing that we have decided to take 5 background images, we will take 
			 * images at each position. THen we will wait long enough to ensure that the 
			 * cell culture has moved (since the pumps go on and off). THen we will take 
			 * images again, and so on. These 5 images we have of each position aren't
			 * useful for looking at the cells. THe cells are blurry because their flowing
			 * by. However, for each pixel location at each stage position, we can select the
			 * median intensity pixel. Assuming that the flowing cells are randomly scattered
			 * at low density throughout these images, the median intensity composite image 
			 * of the background (aka synthetic BG) will look like an images without any cells
			 * in it at all. If we then subtract this composite image from the previously
			 * acquired settled images, the resulting image (aka BG subtracted image) should
			 * contain only cells on a dark background. Doing this makes finding the cells as
			 * Regions of Interest (ROIs) much easier.
			 * 
			 */
			
			sendImmediateMessage(brSettings.getMediaPumpRatio(),brSettings.getSamplePumpRatio());
			String imageTitle=this.IMAGE_OF_INTEREST;
			//This "if" allows the user to not take any background images. "imageTitle" is the title
			//of the image which should be analyzed at the end. It will be the IMAGE_OF_INTEREST if
			//no BG images were taken. Otherwise, it will be the BG_SUBTRACTED_IMAGE
			if(brSettings.getBackgroundImages()>0){
				//Give 5 seconds for the pump to start. This ensures that the first background image
				//is different from the recently acquired image of interest
				delay(5000);
				//Note that the acquisition set is 0. This means that the background images
				//will overwrite each other after every acquisition. This is desirable
				//because it saves disc space.
				acquireImages(pl,0,this.BG_IMAGE,0,this.brSettings,this.core_);
				
				//Now get the others. This time, the delay before taking the next set is more sophisticated.
				for(int i=1;i<brSettings.getBackgroundImages();i++){
					delayForPumps(System.currentTimeMillis(),brSettings.getSamplePumpRatio(),brSettings.getInterval());
					//0 is sent in as the image number so that BG images overwrite each other after each set
					acquireImages(pl,0,this.BG_IMAGE,i,this.brSettings,this.core_);
				}
				generateBGs(pl.getNumberOfPositions(),acqSet,this.BG_IMAGE,this.SYNTHESIZED_BG,brSettings);
				generateBgSubtractedImages(pl.getNumberOfPositions(),acqSet,this.IMAGE_OF_INTEREST,this.SYNTHESIZED_BG,brSettings);
				imageTitle=this.BG_SUBTRACTED_IMAGE;
			}
			
			//--------------Analyze the images--------------
			ImageStack tmpRoiStack=null;
			ImageStack tmpFluorStack=null;
			for(int position=0;position<pl.getNumberOfPositions();position++){
				String roiImgPath=getFormattedPath(brSettings.getSaveImgDir(), brSettings.getRoiGroup()
						   , brSettings.getRoiPreset(), imageTitle,acqSet, position, 0);
				String fluorImgPath=getFormattedPath(brSettings.getSaveImgDir(), brSettings.getIntensityGroup()
						   , brSettings.getIntensityPreset(), imageTitle,acqSet, position, 0);
				ImagePlus roiImage=getImage(roiImgPath);
				ImagePlus fluorImage=getImage(fluorImgPath);
				
				if(tmpRoiStack==null||tmpFluorStack==null){
					tmpRoiStack=new ImageStack(roiImage.getWidth(),roiImage.getHeight());
					tmpFluorStack=new ImageStack(fluorImage.getWidth(),fluorImage.getHeight());
				}
				tmpRoiStack.addSlice(roiImage.getProcessor());
				tmpFluorStack.addSlice(fluorImage.getProcessor());
			}
			String csvFilePath=brSettings.getSaveMetaDir()+startTime+this.ANALYSIS_RESULTS+".csv";
			//Fill an array with the path names for the processed images.
			//This will speed up future image analysis, because the pre-processed images
			//will be available
			String[] processedImageSavePaths=new String[tmpRoiStack.size()];
			String[] processedImageTitles=new String[tmpRoiStack.size()];
			for(int i=1;i<=tmpRoiStack.size();i++){
				processedImageSavePaths[i-1]=getFormattedPath(brSettings.getSaveImgDir(), brSettings.getRoiGroup()
						   , brSettings.getRoiPreset(), "_Processed_",this.acqSet, i-1, 0);
				processedImageTitles[i-1]=getFormattedPath("", brSettings.getRoiGroup()
						   , brSettings.getRoiPreset(), "_Processed_",this.acqSet, i-1, 0);
				}
			//New code below
			List<Roi> setRois = null;
			//If extra variables should be sent to the beanshell script, declare them here or earlier.
			//Note that you should only send in Objects, not primitive values (i.e Integer, not int).
			//THen, simply "set" the variables name and send in the reference, as I have done below.
			
			if(brSettings.isCallExternalImageAnalysis()){
				//Use Beanshell script to analyze images
				Interpreter i = new Interpreter();  // Construct an interpreter
				try {
					//Set variables
				i.set("roiStack", tmpRoiStack);                    	// Send the ImageStack from which the ROIs will be found
				i.set("fluorStack", tmpFluorStack);                 // Send the ImageStack from which the fluorescence intensity should be measured
				i.set("pathToCSV", csvFilePath);                   	// Send the filepath where results should be appended to a CSV table
				i.set("set", this.acqSet);                    							 // Send in the set (this is a column in the CSV)
				i.set("paParams", brSettings.paParams);      								 // Send in the ParticleAnalyzer parameters
				i.set("maxEccentricity", brSettings.getMaxEccentricity());      			 // Send in the maxEccentricity
				i.set("pathToSaveProcessedImages", processedImageSavePaths);     			// Send an array of path names to save the processed images
				i.set("inputRoiStackIsProcessed", ROISTACKISPROCCESSED);        				// This variable enables re-analysis of pre-processed images, 
																							//but it will never be used from here

				// Source an external script file
				i.source(brSettings.getpathToExternalImageAnalysis());
				setRois = (List<Roi>) i.get("roiList");    // retrieve a variable
				i=null;
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (EvalError e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			
			}else{
				//Use the "Default" analysis via YeastFluorGetter class
				YeastFluorGetter yfg=new YeastFluorGetter(tmpRoiStack,tmpFluorStack
														,csvFilePath,this.acqSet
														,brSettings.paParams,brSettings.getMaxEccentricity()
														,processedImageSavePaths,ROISTACKISPROCCESSED);
				setRois=yfg.analyzeAndGetResults();
				yfg=null;
			}
			//New code above 1/2/17
			
			int base=(this.acqSet)*pl.getNumberOfPositions();
			
			//Rois were found in the tmp stacks. THis loop increments their slice position to correspond to the total stacks
			for(Roi roi:setRois){
				int slice=roi.getPosition();
				roi.setPosition(slice+base);
			}
			//save Rois after every set			
			RoiManager rm=YeastFluorGetter.saveRois(setRois,brSettings.getSaveMetaDir()+"RoiSetFromPreviousSetTo"+Integer.toString(acqSet)+".zip");
			rm.reset();
			setRois.clear();
			
			if(this.processedBgSubRoiStack==null){
				this.processedBgSubRoiStack=new VirtualStack(tmpRoiStack.getWidth(),tmpRoiStack.getHeight(),tmpRoiStack.getColorModel(),brSettings.getSaveImgDir());
			}
			
			for(String title:processedImageTitles){
				this.processedBgSubRoiStack.addSlice(title);
			}
			for(int i=0;i<this.virtualStackOfBgSubAcquiredImages.length;i++){
				this.virtualImpOfBgSubAcquiredImages[i]=new ImagePlus("Background subtracted images",this.virtualStackOfBgSubAcquiredImages[i]);
				this.virtualImpOfBgSubAcquiredImages[i].show();
			}
			this.processedBgSubRoiStackImg=new ImagePlus("Processed BG subtracted Roi Stack",this.processedBgSubRoiStack);
			this.processedBgSubRoiStackImg.show();
			tmpRoiStack=null;
			tmpFluorStack=null;
			
			System.gc();
			long remainingDelay=(long) (startTimeInAcqSet+(brSettings.getMinSetDelay()*1000*60)-System.currentTimeMillis());
			if(brSettings.isEscaped()){
				remainingDelay=0;
				this.acqSet=brSettings.getIterations();
			}

			if(remainingDelay>0)
				delay(remainingDelay);			
		}
   
   }
   
   public void delay(long delay){
	   try {
		Thread.sleep(delay);
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
   }
	public boolean getParameters(){
	    String[] cfg_groups=core_.getAvailableConfigGroups().toArray();
		String[] cfg_presets=getAllPresets(cfg_groups);
		int positions;
		try {positions=app_.getPositionList().getNumberOfPositions();
			}catch (MMScriptException e) {e.printStackTrace();return false;}
		this.brSettings=new ParametersGetter("Bioreactor Settings", positions, cfg_groups, cfg_presets,this);
		boolean parametersGotten=brSettings.showDialog();
		if(!parametersGotten){
			IJ.log("cancelled");
			dispose();
			return false;
		}else
			return true;
	}

	public void generateBgSubtractedImages(int positions,int acqSet,String imageTitle
			,String bgTitle, ParametersGetter brSettings){
		for(int i=0;i<positions;i++){
			for(int channel=0;channel<brSettings.getAcqPresets().length;channel++){
				String imagePath=getFormattedPath(brSettings.getSaveImgDir(), brSettings.getAcqGroups()[channel]
						   , brSettings.getAcqPresets()[channel], imageTitle,acqSet, i, 0);
				String bgPath=getFormattedPath(brSettings.getSaveImgDir(), brSettings.getAcqGroups()[channel]
						   , brSettings.getAcqPresets()[channel], bgTitle,acqSet, i, 0);
				ImagePlus image=getImage(imagePath);
				ImagePlus bg=getImage(bgPath);
				ImageCalculator ic = new ImageCalculator();
				ImagePlus bgSubtractedImage=ic.run(brSettings.getBgOperations()[channel]+" create", image, bg);
				String bgSubImgPath=getFormattedPath(brSettings.getSaveImgDir(), brSettings.getAcqGroups()[channel]
						   , brSettings.getAcqPresets()[channel], this.BG_SUBTRACTED_IMAGE,acqSet, i, 0);
				saveImage(bgSubtractedImage,bgSubImgPath);
				
				if(this.virtualStackOfBgSubAcquiredImages[channel]==null){
					this.virtualStackOfBgSubAcquiredImages[channel]=new VirtualStack(bgSubtractedImage.getWidth(),bgSubtractedImage.getHeight()
							,bgSubtractedImage.getProcessor().getColorModel(),brSettings.getSaveImgDir());
				}
		
				this.virtualStackOfBgSubAcquiredImages[channel].addSlice(bgSubImgPath);
			}
		}
	}

   private String[] getAllPresets(String[] cfg_groups){
	   	int numPresets=0;
	   	String[] arrPresets;
	   	int iter=0;
	   
	   	
	   	//Count the total number of presets inside each group
		for(int i=0;i<cfg_groups.length;i++){
			numPresets+=core_.getAvailableConfigs(cfg_groups[i]).toArray().length;
		}
		//Prepare an array just big enough to fit all the presets
		arrPresets=new String[numPresets];
		
		//Fill the "arrPresets" array with all of the micromanager configuration presets. Note that they
		//are placed in the same array, without regard to the group they belong to.
		//This will cause problems if there are presets in different cfg_groups with the same name.
		
		//For each group...
		for(int i=0;i<cfg_groups.length;i++){
			//Get the corresponding array of presets...
			String[] temp=core_.getAvailableConfigs(cfg_groups[i]).toArray();
			//Fill the "arrPresets" array with each of the presets in the array of presets for that group
			for(int l=0;l<temp.length;l++){
				arrPresets[iter]=temp[l];
				iter++;
			}
		}
		return arrPresets;
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
   
   public void generateBGs(int positions,int imageNum,String getAs, String saveAs,ParametersGetter brSettings){
	   for(int i=0;i<positions;i++){
		   for(int j=0;j<brSettings.getAcqPresets().length;j++){
			   String bg0Path=getFormattedPath(brSettings.getSaveImgDir(), brSettings.getAcqGroups()[j]
					   , brSettings.getAcqPresets()[j], getAs,imageNum, i, 0);
			   ImagePlus bg0=getImage(bg0Path);
			   ImageStack bgStack= bg0.createEmptyStack();
			   bgStack.addSlice(bg0.getProcessor());
				for(int k=1;k<brSettings.getBackgroundImages();k++){
					String bgPath=getFormattedPath(brSettings.getSaveImgDir(), brSettings.getAcqGroups()[j]
							   , brSettings.getAcqPresets()[j], getAs,imageNum, i, k);
					ImagePlus bg=getImage(bgPath);
					bgStack.addSlice(bg.getProcessor());
					}
				ImagePlus bgStackAsImp=new ImagePlus("stack",bgStack);
				ZProjector zStack=new ZProjector(bgStackAsImp);
				zStack.setMethod(ZProjector.MEDIAN_METHOD);
				zStack.setStartSlice(1);
				zStack.setStopSlice(bgStackAsImp.getStackSize());
				zStack.doProjection();
				ImagePlus syntheticBg = zStack.getProjection();
				
				String syntheticBgPath=getFormattedPath(brSettings.getSaveImgDir(), brSettings.getAcqGroups()[j]
						   , brSettings.getAcqPresets()[j], saveAs,imageNum, i, 0);
			    saveImage(syntheticBg,syntheticBgPath);
		   }
	   }
	   
   }

   
   //This ensures that the sampling pump is on for at least 3 seconds,
   //which ensures that the cells will have moved. Note, 3 seconds is an 
   //arbitrary amount of time. 1 second might be fine. 60 seconds would be fine too, 
   //but why waste time?
   public void delayForPumps(long startTime,double onRatio,double interval){
	   double offRatio=1-onRatio;
	   if((3000+startTime+(long)(offRatio*interval*1000))>System.currentTimeMillis()){
		   //*1000 converts seconds to milliseconds
			// While not enough time has passed to ensure that the media has moved since the last set of images were taken.
			//5 seconds + the amount of time that the sampling pump is off
			delay(3000+startTime+(long)(offRatio*interval*1000)-System.currentTimeMillis());
		}
   }
   
   //This sends messages to the microcontroller, listens for responses, and writes them to
   //the meta-data directory
   public void sendImmediateMessage(double mediaPumpRatio, double samplePumpRatio){
	   sendImmediateMessage(mediaPumpRatio,samplePumpRatio,brSettings.getPort(),brSettings.getSaveMetaDir());
	}   
   
   public void sendImmediateMessage(double mediaPumpRatio, double samplePumpRatio,String port,String saveDir){
		String answer="no answer";
		
					boolean messageSent=false;
					//immeC indicates the initiation of a message
					String command = "immeC,"+mediaPumpRatio+","+samplePumpRatio; 
					while(!messageSent){
						try {
							core_.setSerialPortCommand(port, command, COMMAND_TERMINATOR); 
							delay(500);
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
							clearMessageBuffer();
						}
						try {
							answer = core_.getSerialPortAnswer(port, COMMAND_TERMINATOR);
						} catch (Exception e){
							//print("Exception: " + e.getMessage() + "\n Will resend message.");
							messageSent=false;
							clearMessageBuffer();
						}
					if(answer.substring(0,5).equals("Start")){
						messageSent=true;
						//print(answer.substring(5,answer.length()-1)+
						//"<- milliseconds since timecourse began, measured temperature of bioreactor, pwm of heating pad, LEDs ON, pwm of LEDs");
						if(saveDir!=null){
							FileWriter writer;
							try {
								writer = new FileWriter(saveDir+startTime+MICROCONTROLLER_RECORDS+".csv",true);
							writer.write("The real time is ,"+SDF.format(new Date()));
							writer.write(",Computer time in milliseconds since the start of the run,"+(System.currentTimeMillis()-startTimeMillis));
							writer.write(","+answer+","+LINE_SEPARATOR);
							writer.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								clearMessageBuffer();
							} 
						}
					}else{
						messageSent=false;
						clearMessageBuffer();
						}
					}
	} 
   
   //assumes that groups.size() = presets.size()
   public void acquireImages(PositionList pl, int set, String title, int version, ParametersGetter brSettings, CMMCore core_){
		IJ.run("Clear Results", ""); //clear previous results
		IJ.run("Close All", "");//close all windows
		takeImage();
		//This first image is taken because the command is slow the first time that it is run. 
		//Subsequent calls load the image within 50milliseconds (on the old 32 bit computer it was made for :D )
		//It causes problems if imageJ commands are called immediatly afterwards
				
		//*******Take images for each position in the list, in each channel***************//
		int positions=pl.getNumberOfPositions();
		for (int l=0; l < positions; l++) {
			IJ.log("Set: "+set+". Position: "+l+Integer.toString(l));
			goToPosition(pl,l,core_);
			for (int i=0;i<brSettings.getAcqPresets().length;i++){
				IJ.log("Channel is: "+brSettings.getAcqPresets()[i]);
				//This portion needs to be fast to reduce discrepancies between images
				setChannel(brSettings.getAcqGroups()[i],brSettings.getAcqPresets()[i]);
				ImagePlus img=takeImage();			
				String imagePath=getFormattedPath(brSettings.getSaveImgDir(),brSettings.getAcqGroups()[i]
						   ,brSettings.getAcqPresets()[i],"settled",set,l,0);
				saveImage(img,imagePath);
				}
			// Return to configurationpresets[0]. This is useful to ensure that the excitation light is off
				setChannel(brSettings.getDarkGroup(),brSettings.getDarkPreset());
			}
		//Give time to save images
		delay(100);
		//ensure that all windows are closed
		IJ.run("Close All", "");
}
   //THis method handles errors that arise when moving to a new stage position by simply pausing and trying again.
   public void goToPosition(PositionList pl, int pos,CMMCore core_){
	    try {
			MultiStagePosition.goToPosition(pl.getPosition(pos), core_);
			core_.waitForSystem();
	    } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			delay(1000);
			goToPosition(pl,pos,core_);
		}
   }

   public void setup(){
	   IJ.log("inside setup");
	   sendImmediateMessage(brSettings.getMediaPumpRatio(),brSettings.getSamplePumpRatio());
	   try {
		    IJ.log("inside try");
			app_.clearMessageWindow();
			app_.closeAllAcquisitions();
			this.startTimeMillis=System.currentTimeMillis();
			this.startTime=SDF.format(new Date());
			// These 4 lines make the image stack. The image is acquired so that the ImageStack's width and height
			// can be set. It is later globally available and set to the correct dimensions
			/*core_.snapImage();	
			IJ.log("image snapped");
			this.imp.setProcessor(ImageUtils.makeProcessor(core_, core_.getImage()));
			this.stack= this.imp.createEmptyStack();
			this.imp.close();
			*/
			core_.setConfig(brSettings.getDarkGroup(),brSettings.getDarkPreset());
		} catch (MMScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {this.pl=app_.getPositionList();
			} catch (MMScriptException e1) {e1.printStackTrace();}
		this.acqSet=0;
		   IJ.log("setup complete");
   }
   
   public void loadTimeCourse(){
		loadTimeCourse(brSettings.getLED_timecourse(),brSettings.getPort()); 
	}
   
   public void loadTimeCourse(List<String[]> timecourse,String port){
		//print("port is  ->"+brSettings.getPort());
		int i=0;//row counter
		for (String[] row:timecourse){
			boolean messageSent=false;
			while(!messageSent){
					try {
						core_.setSerialPortCommand(port, "timeC,"+i+","+Arrays.toString(row), COMMAND_TERMINATOR);
						delay(500);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
										
					String answer="no answer";
				   try {
					answer = core_.getSerialPortAnswer(port, COMMAND_TERMINATOR);
				   } catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
		   			}
				   if(!(answer.equals(Arrays.toString(timecourse.get(i))))){
				   	clearMessageBuffer();
				   	IJ.log("Message transmission to microcontroller failed. Will retry...");
					delay(1000);
				   }else{//message was confirmed! Increment the counter
					   IJ.log("Message: "+answer+" was sent to the microcontroller successfully");
					   messageSent=true;
					   i++;
				   }
			}
		
		} 
	}
 
   
   public void clearMessageBuffer(){
		for(int i=0;i<3;i++){
			try{
				//This clears out the message buffer
				core_.getSerialPortAnswer(brSettings.getPort(), COMMAND_TERMINATOR);
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

   //Images are retrieved and stored from the hard drive. THey are not maintained in a java collection because
   //they could be too large. This method ensures that images are saved in a standard format
   public static String getFormattedPath(String dir, String group, String preset, String title,int imageNum, int pos, int version){
	   return dir+group+"_"+preset+"_"+title+"_Set_"+String.format("%05d", imageNum)+"_Pos_"+String.format("%03d", imageNum)+"_v_"+version;
   }
   public static void saveImage(ImagePlus image,String formattedPath){
	   String format="Tiff";;
	   IJ.saveAs(image,format,formattedPath);
   }
   public static ImagePlus getImage(String formattedPath){
	   String suffix=".tiff";
	   return IJ.openImage(formattedPath+suffix);
   }
  
   public ImagePlus takeImage(){
	   ImagePlus img=new ImagePlus();
		try {			
			core_.clearCircularBuffer();
			core_.waitForSystem();
			core_.snapImage();
			core_.waitForSystem();
			ImageProcessor proc0= ImageUtils.makeProcessor(core_, core_.getImage());
			img=new ImagePlus("",proc0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			takeImage();
		}
		return img;
   }
	public void setChannel(String group,String preset){
		try {
			delay(20);
			core_.waitForSystem();
			core_.setConfig(group,preset);
			core_.waitForSystem();
			delay(20); //let vibrations dissipate
		} catch (Exception e) {
			e.printStackTrace();
			setChannel(group,preset);
		}
	}
      
}
