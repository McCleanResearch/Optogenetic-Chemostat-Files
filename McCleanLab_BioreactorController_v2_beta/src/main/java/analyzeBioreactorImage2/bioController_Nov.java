
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
import ij.process.ImageProcessor;

import java.awt.Button;
import java.awt.event.ActionEvent;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import java.awt.event.ActionListener;

import mmcorej.CMMCore;

import org.micromanager.api.MMPlugin;
import org.micromanager.api.MultiStagePosition;
import org.micromanager.api.PositionList;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.ImageUtils;
import org.micromanager.utils.MMScriptException;

import util.opencsv.CSVReader;
import util.opencsv.CSVWriter;
import bsh.EvalError;
import bsh.Interpreter;




public class bioController_Nov implements MMPlugin,ActionListener {
   public static final String menuName = "Bioreactor Controller 2.51";
   public static final String tooltipDescription =
      "This is used to control a bioreactor and microsocpe";
  
   public ParametersGetter brSettings=null;
   public PositionList pl;
   private int acqSet=0; //images taken so far
   private long startTimeMillis=0;
   private long startTimeInAcqSet=0;
   public String startTime;
   public VirtualStack[] virtualStackOfBgSubAcquiredImages=new VirtualStack[2];
   public ImagePlus[] virtualImpOfBgSubAcquiredImages=new ImagePlus[2];
   public VirtualStack processedBgSubRoiStack;
   public ImagePlus processedBgSubRoiStackImg;
   public String csvResults;
   public String tempCsvResults;
   public final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-z");
   public final String IMAGE_OF_INTEREST="raw";
   public final String BG_IMAGE="rawBG";
   public final String SYNTHESIZED_BG="synthBG";
   public final String BG_SUBTRACTED_IMAGE="bgSub";
   public final String BINARY_IMAGE="bin";
   public final String PROCESSED_IMAGE="proc";
   public final String LINE_SEPARATOR="\r\n";
   public final String COMMAND_TERMINATOR="#";
   public final String MICROCONTROLLER_RECORDS="_microcontrollerRecords";
   public final Boolean ROISTACKISPROCCESSED=new Boolean(false); //THe stack will never be pre-processed from the perspective of this class
   private Button escapeButton=new Button("(Broken)Escape after set?");
   private JDialog dialog=new JDialog();
   private boolean escape=false;



   
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
	//Testing 1/6
	   if(!getParameters())
		   return;
		setup();
		this.csvResults=brSettings.getSaveMetaDir()+startTime+"_allResults.csv";
		this.tempCsvResults=brSettings.getSaveMetaDir()+"_recentResults.csv";
		IJ.log("\\Clear"); //empties the Log 
		IJ.log("About to begin acquiring images!"); 
		if(this.pl.getNumberOfPositions()<1){
			IJ.log("The position list is empty! Nothing to do here. LED timecourse was not sent");
			this.acqSet=brSettings.getIterations();
		}
		escapeButton.addActionListener(this);
		dialog.add(escapeButton);
		dialog.setSize(150, 100);
		dialog.setVisible(true);
		//-------------------Main loop: Collect and analyze images-------------------------------------
		while(this.acqSet<brSettings.getIterations()){

			IJ.showStatus("Collecting set "+Integer.toString(this.acqSet)+" of "+Integer.toString(brSettings.getIterations()));
			sendImmediateMessage(brSettings.getMediaPumpRatio(),0);
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
			if(acqSet==0)
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
				IJ.showStatus("Collecting background image 0 of set "+Integer.toString(this.acqSet)+" of "+Integer.toString(brSettings.getIterations()));
				//Give 5 seconds for the pump to start. This ensures that the first background image
				//is different from the recently acquired image of interest
				delay(5000);
				//Note that the acquisition set is 0. This means that the background images
				//will overwrite each other after every acquisition. This is desirable
				//because it saves disc space.
				acquireImages(pl,0,this.BG_IMAGE,0,this.brSettings,this.core_);
				
				//Now get the others. This time, the delay before taking the next set is more sophisticated.
				for(int i=1;i<brSettings.getBackgroundImages();i++){
					IJ.showStatus("Collecting background image "+Integer.toString(i)+" of set "+Integer.toString(this.acqSet)+" of "+Integer.toString(brSettings.getIterations()));
					delayForPumps(System.currentTimeMillis(),brSettings.getSamplePumpRatio(),brSettings.getInterval());
					//0 is sent in as the image number so that BG images overwrite each other after each set
					acquireImages(pl,0,this.BG_IMAGE,i,this.brSettings,this.core_);
				}
				//IJ.log("About to generate BGs");
				IJ.showStatus("Generating processed backgound images");
				//The imageNum or AcqSet sent to generateBGs() is always 0. The BG images write over each other so that
				//memory is not wasted on them
				generateBGs(pl.getNumberOfPositions(),0,this.BG_IMAGE,this.SYNTHESIZED_BG,brSettings);
				//IJ.log("about to subtract BG");
				generateBgSubtractedImages(pl.getNumberOfPositions(),acqSet,this.IMAGE_OF_INTEREST,this.SYNTHESIZED_BG,brSettings);
				imageTitle=this.BG_SUBTRACTED_IMAGE;
			}
			//IJ.log("About to analyze images");
			
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
			//Fill an array with the path names for the processed images.
			//This will speed up future image analysis, because the pre-processed images
			//will be available
			String[] processedImageSavePaths=new String[tmpRoiStack.size()];
			String[] processedImageTitles=new String[tmpRoiStack.size()];
			for(int i=1;i<=tmpRoiStack.size();i++){
				processedImageSavePaths[i-1]=getFormattedPath(brSettings.getSaveImgDir(), brSettings.getRoiGroup()
						   , brSettings.getRoiPreset(), PROCESSED_IMAGE,this.acqSet, i-1, 0);
				processedImageTitles[i-1]=getFormattedPath("", brSettings.getRoiGroup()
						   , brSettings.getRoiPreset(), PROCESSED_IMAGE,this.acqSet, i-1, 0);
				}
			//New code below
			List<Roi> setRois = null;
			//If extra variables should be sent to the beanshell script, declare them here or earlier.
			//Note that you should only send in Objects, not primitive values (i.e Integer, not int).
			//THen, simply "set" the variables name and send in the reference, as I have done below.
			IJ.showStatus("Identifying ROIs and measuring properties");
			if(brSettings.analysisOptionSelected.equals(ParametersGetter.OPTION3)){
				//Use Beanshell script to analyze images
				Interpreter i = new Interpreter();  // Construct an interpreter
				try {
					//Set variables
				i.set("roiStack", tmpRoiStack);                    	// Send the ImageStack from which the ROIs will be found
				i.set("fluorStack", tmpFluorStack);                 // Send the ImageStack from which the fluorescence intensity should be measured
				i.set("pathToCSV", tempCsvResults);                   	// Send the filepath where results should be appended to a CSV table
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
				YeastFluorGetter yfg=null;
				if(brSettings.analysisOptionSelected.equals(ParametersGetter.OPTION2)){
				//Use the "Default" analysis via YeastFluorGetter class
				yfg=new YeastFluorGetter10xPC(tmpRoiStack,tmpFluorStack
														,tempCsvResults,this.acqSet
														,brSettings.paParams,brSettings.getMaxEccentricity()
														,processedImageSavePaths,ROISTACKISPROCCESSED);
				}else{
					yfg=new YeastFluorGetter(tmpRoiStack,tmpFluorStack
							,tempCsvResults,this.acqSet
							,brSettings.paParams,brSettings.getMaxEccentricity()
							,processedImageSavePaths,ROISTACKISPROCCESSED);
				}
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
			if(!YeastFluorGetter.saveRois(setRois,brSettings.getSaveMetaDir()+"RoiSetFromPreviousSetTo"+Integer.toString(acqSet)+".zip"))
				IJ.log("An error occured while saving ROIs from set: "+Integer.toString(acqSet));
			setRois.clear();
			
			if(this.processedBgSubRoiStack==null){
				this.processedBgSubRoiStack=new VirtualStack(tmpRoiStack.getWidth(),tmpRoiStack.getHeight(),tmpRoiStack.getColorModel(),brSettings.getSaveImgDir());
			}
			
			for(String title:processedImageTitles){
				//IJ.log("About to add: "+title+" to "+this.processedBgSubRoiStack.getDirectory());
				this.processedBgSubRoiStack.addSlice(title+".tif");
			}
			for(int i=0;i<this.virtualStackOfBgSubAcquiredImages.length;i++){
				if(this.virtualImpOfBgSubAcquiredImages[i]!=null)
					this.virtualImpOfBgSubAcquiredImages[i].hide();
				this.virtualImpOfBgSubAcquiredImages[i]=new ImagePlus("Background subtracted images",this.virtualStackOfBgSubAcquiredImages[i]);
				this.virtualImpOfBgSubAcquiredImages[i].show();
				IJ.run(this.virtualImpOfBgSubAcquiredImages[i],"Set... ", "zoom=25");
			}
			if(this.processedBgSubRoiStackImg!=null){
				this.processedBgSubRoiStackImg.hide();
			}
			this.processedBgSubRoiStackImg=new ImagePlus("Processed BG subtracted Roi Stack",this.processedBgSubRoiStack);
			this.processedBgSubRoiStackImg.show();
			IJ.run(this.processedBgSubRoiStackImg,"Set... ", "zoom=25");
			tmpRoiStack=null;
			tmpFluorStack=null;
			
			appendResults(this.csvResults,this.tempCsvResults,this.acqSet);
			
			if(brSettings.isReloadTimecourse()){
				sendMeasurementsToMatlab();
				reloadTimeCourse();
			}
			
			System.gc();
			long remainingDelay=(long) (startTimeInAcqSet+(brSettings.getMinSetDelay()*1000*60)-System.currentTimeMillis());
			if(this.escape){
				remainingDelay=0;
			}

			if(remainingDelay>0){
				//IJ.log("The program will now delay for "+Long.toString(remainingDelay)+" milliseconds");
				delay(remainingDelay);
			}
			this.acqSet++;
			if(this.escape){
				this.acqSet=brSettings.getIterations();
			}
		} //end of long while-loop
		IJ.log("Acquisition complete! The bioreactor controller is finished");
   }

	public void sendMeasurementsToMatlab(){
		String consoleOutput=brSettings.getSaveMetaDir()+"_MatLab_console_itr_"+String.format("%03d", this.acqSet)+".txt";
		sendMeasurementsToMatlab(brSettings.getPathToMatlabExe(), consoleOutput,this.tempCsvResults, brSettings.getPathToMatlabScript(), brSettings.getPathToTimecourse());
	}

	public void sendMeasurementsToMatlab(String pathToMatlabExe, String pathToConsoleOutput,String pathToResults, String pathToMatlabScript, String pathToTimecourse){
		try {
			//Process p = null;
			/*In the absense of variables, the following lines might be like this
			 * 
			 * Runtime.getRuntime().exec("\"c:\\Program Files\\MATLAB\\R2016a\\bin\\matlab.exe\"
			 * -nodisplay -nosplash -nodesktop -logfile \"c:\\temp\\helloWorldLog.txt\" -r \"try,
			 * m=csvread('C:\\temp\\csvInput.csv',1,0); run('C:\\Users\\Cameron\\Documents\\MATLAB\\helloWorld.m');
			 * dlmwrite('C:\\temp\\csvOutput.csv',timecourse,'precision',9,'roffset',1,'delimiter',',','newline','pc'),
			 *  catch, exit(1), end, exit(0)\"");
			 *    
			 *    If you're thinking "WTF, that doesn't help!" I don't blame you. Everything after "exec("
			 *    is basically a Windows command line command (DOS command?). Those things that begin with
			 *    "-" are Windows functions. So I tell Windows to run Matlab.exe, but keep it minimal. 
			 *    The "logfile" of the program (in this case the output of the Matlab console) should be saved
			 *    to consoleOutput. This text file should be handy for troubleshooting errors. 
			 *    
			 *    Then the important command is "-r". Everything after this should operate as though it had
			 *    been entered directly in the MatLab console. So I upload tempCsvResults (skipping the headers) into
			 *    MatLab as the variable "measurements". Then I run the MatLab script which contains the control code.
			 *    The Matlab script should end by assigning the recommended LED timecourse to the variable "timecourse"
			 *    as a matrix where the columns are [Time in seconds, number of LEDs to have on at that time, PWM to set at that time]
			 */
			
			Runtime.getRuntime().exec("\""+pathToMatlabExe
					+"\" -nodisplay -nosplash -nodesktop "
					+ "-logfile \""+pathToConsoleOutput+"\" "
					+ "-r \"try, measurements=csvread('"+pathToResults+"',1,0); "
						+ "run('"+pathToMatlabScript+"'); "
						+ "dlmwrite('"+pathToTimecourse+"',timecourse,'precision',9,'roffset',1,'delimiter',',','newline','pc'), catch, exit(1), end, exit(0)\"");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
   
   public void delay(long delay){
	   IJ.wait((int)delay);
   }
	public boolean getParameters(){
	    String[] cfg_groups=core_.getAvailableConfigGroups().toArray();
		String[] cfg_presets=getAllPresets(cfg_groups);
		int positions;
		try {positions=app_.getPositionList().getNumberOfPositions();
			}catch (MMScriptException e) {e.printStackTrace();return false;}
		//("From bioController, numPositions will be set to: "+Integer.toString(positions));
		if(this.brSettings==null)
			this.brSettings=new ParametersGetter("Bioreactor Settings", positions, cfg_groups, cfg_presets,this);
		else{
			this.brSettings.setGroups(cfg_groups);
			this.brSettings.setPresets(cfg_presets);			
			this.brSettings.setCaller(this);
			this.brSettings.setTitle("Bioreactor Settings");
			this.brSettings.setNumPositions(positions);
		}
		boolean parametersGotten=brSettings.showDialog();
		if(!parametersGotten){
			//("cancelled");
			dispose();
			return false;
		}else
			return true;
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		if(arg0.getSource()==escapeButton&&!escape){
			IJ.log("Will soon halt this execution");
			this.escape=true;
			escapeButton.setLabel("Undo escape?");
			dialog.setVisible(false);
			dialog.setVisible(true);
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public void appendResults(String pathToMainFile, String pathToAppendee,int iteration){
		FileReader reader;
		FileWriter writer;
		try {
			reader = new FileReader(pathToAppendee);
			CSVReader readCsv=new CSVReader(reader);
			Iterator<String[]> itrCsv=readCsv.readAll().iterator();
			
			
			writer=new FileWriter(pathToMainFile, true);
			CSVWriter writeCsv =new CSVWriter(writer);
			
			if(iteration==0)// headers should be written
				writeCsv.writeNext(itrCsv.next());
			else //ignore the first line
				itrCsv.next();
			while(itrCsv.hasNext())
				writeCsv.writeNext(itrCsv.next());
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			IJ.showMessage("File not found. Cannot continue");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			IJ.showMessage("Input/Output Exception. Cannot continue");
			e.printStackTrace();
		}
		// if iteration=0, include first line, else skip first line
		
		//Append it to mainFile
	}

	public void generateBgSubtractedImages(int positions,int acqSet,String imageTitle
			,String bgTitle, ParametersGetter brSettings){
		//generateBgSubtractedImages(pl.getNumberOfPositions(),acqSet,this.IMAGE_OF_INTEREST,this.SYNTHESIZED_BG,brSettings);
		//("385");
		for(int i=0;i<positions;i++){
			for(int channel=0;channel<brSettings.getAcqPresets().length;channel++){
				String imagePath=getFormattedPath(brSettings.getSaveImgDir(), brSettings.getAcqGroups()[channel]
						   , brSettings.getAcqPresets()[channel], imageTitle,acqSet, i, 0);
				String bgPath=getFormattedPath(brSettings.getSaveImgDir(), brSettings.getAcqGroups()[channel]
						   , brSettings.getAcqPresets()[channel], bgTitle,0, i, 0);
				//("392: "+imagePath);
				ImagePlus image=getImage(imagePath);
				//("394: "+bgPath);
				ImagePlus bg=getImage(bgPath);
				ImageCalculator ic = new ImageCalculator();
				ImagePlus bgSubtractedImage=ic.run(brSettings.getBgOperations()[channel]+" create", image, bg);
				String bgSubImgPath=getFormattedPath(brSettings.getSaveImgDir(), brSettings.getAcqGroups()[channel]
						   , brSettings.getAcqPresets()[channel], this.BG_SUBTRACTED_IMAGE,acqSet, i, 0);
				String bgSubImgTitle=getFormattedPath("", brSettings.getAcqGroups()[channel]
						   , brSettings.getAcqPresets()[channel], this.BG_SUBTRACTED_IMAGE,acqSet, i, 0);
				//("400: "+bgSubImgPath);
				saveImage(bgSubtractedImage,bgSubImgPath);
				
				if(this.virtualStackOfBgSubAcquiredImages[channel]==null){
					this.virtualStackOfBgSubAcquiredImages[channel]=new VirtualStack(bgSubtractedImage.getWidth(),bgSubtractedImage.getHeight()
							,bgSubtractedImage.getProcessor().getColorModel(),brSettings.getSaveImgDir());
				}
				//("About to add: "+bgSubImgTitle+" to "+this.virtualStackOfBgSubAcquiredImages[channel].getDirectory());
				this.virtualStackOfBgSubAcquiredImages[channel].addSlice(bgSubImgTitle+".tif");
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
	   //generateBGs(pl.getNumberOfPositions(),acqSet,this.BG_IMAGE,this.SYNTHESIZED_BG,brSettings);
	   for(int i=0;i<positions;i++){
		   for(int j=0;j<brSettings.getAcqPresets().length;j++){
			   //("working on BG at pos "+Integer.toString(i)+" and set "+Integer.toString(j));
			   String bg0Path=getFormattedPath(brSettings.getSaveImgDir(), brSettings.getAcqGroups()[j]
					   , brSettings.getAcqPresets()[j], getAs,imageNum, i, 0);
			   //("path is"+bg0Path);
			   ImagePlus bg0=getImage(bg0Path);
			   //("Image gotten 4622222");
			   //("bg0 width is: "+Integer.toString(bg0.getWidth()));
			   //("bg0 height is: "+Integer.toString(bg0.getHeight()));
			   ImageStack bgStack= new ImageStack(bg0.getWidth(),bg0.getHeight());
			   //("464");
			   bgStack.addSlice(bg0.getProcessor());
			   //("Image gotten 466");
				for(int k=1;k<brSettings.getBackgroundImages();k++){
					String bgPath=getFormattedPath(brSettings.getSaveImgDir(), brSettings.getAcqGroups()[j]
							   , brSettings.getAcqPresets()[j], getAs,imageNum, i, k);
					 //("470: "+bgPath);
					ImagePlus bg=getImage(bgPath);
					bgStack.addSlice(bg.getProcessor());
					}
				 //("474");
				ImagePlus bgStackAsImp=new ImagePlus("stack",bgStack);
				ZProjector zStack=new ZProjector(bgStackAsImp);
				zStack.setMethod(ZProjector.MEDIAN_METHOD);
				zStack.setStartSlice(1);
				zStack.setStopSlice(bgStackAsImp.getStackSize());
				//("480");
				zStack.doProjection();
				//("482");
				ImagePlus syntheticBg = zStack.getProjection();
				//("484");
				String syntheticBgPath=getFormattedPath(brSettings.getSaveImgDir(), brSettings.getAcqGroups()[j]
						   , brSettings.getAcqPresets()[j], saveAs,imageNum, i, 0);
				//("487: "+syntheticBgPath);
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
		//IJ.run("Clear Results", ""); //clear previous results
		//IJ.run("Close All", "");//close all windows
	   	setChannel(brSettings.getAcqGroups()[0],brSettings.getAcqPresets()[0]);
		takeImage();
		//This first image is taken because the command is slow the first time that it is run. 
		//Subsequent calls load the image within 50milliseconds (on the old 32 bit computer it was made for :D )
		//It causes problems if imageJ commands are called immediatly afterwards
				
		//*******Take images for each position in the list, in each channel***************//
		int positions=pl.getNumberOfPositions();
		for (int l=0; l < positions; l++) {
			//IJ.log("Set: "+set+". Position: "+Integer.toString(l));
			goToPosition(pl,l,core_);
			for (int i=0;i<brSettings.getAcqPresets().length;i++){
				//IJ.log("Channel is: "+brSettings.getAcqPresets()[i]);
				//This portion needs to be fast to reduce discrepancies between images
				setChannel(brSettings.getAcqGroups()[i],brSettings.getAcqPresets()[i]);
				ImagePlus img=takeImage();			
				String imagePath=getFormattedPath(brSettings.getSaveImgDir(),brSettings.getAcqGroups()[i]
						   ,brSettings.getAcqPresets()[i],title,set,l,version);
				saveImage(img,imagePath);
				}
			// Return to configurationpresets[0]. This is useful to ensure that the excitation light is off
				setChannel(brSettings.getDarkGroup(),brSettings.getDarkPreset());
			}
		//Give time to save images
		delay(100);
		//ensure that all windows are closed
		//IJ.run("Close All", "");
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
	   //IJ.log("inside setup");
	   sendImmediateMessage(brSettings.getMediaPumpRatio(),brSettings.getSamplePumpRatio());
	   try {
		    //IJ.log("inside try");
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
		   //IJ.log("setup complete");
   }
   
   public void reloadTimeCourse(){
	   brSettings.reloadTimecourse();
	   loadTimeCourse();
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
	   return dir+group+"_"+preset+"_"+title+"_Set_"+String.format("%05d", imageNum)+"_Pos_"+String.format("%03d", pos)+"_v_"+Integer.toString(version);
   }
   public static void saveImage(ImagePlus image,String formattedPath){
	   //IJ.log("About to save: "+formattedPath);
	   String format="Tiff";
	   IJ.saveAs(image,format,formattedPath);
	   image.hide();
   }
   public static ImagePlus getImage(String formattedPath){
	   String suffix=".tif";
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
	   /*app_.snapSingleImage();
	   ImagePlus img=WindowManager.getImage("").duplicate();
	   img.setTitle("newest image");
	   img.hide();
		return img;
		 */
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
