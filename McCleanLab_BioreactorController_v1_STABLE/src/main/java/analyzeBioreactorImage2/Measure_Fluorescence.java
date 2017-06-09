package analyzeBioreactorImage2;
/*
 * To the extent possible under law, the Fiji developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import java.text.SimpleDateFormat;
import java.util.*;

import ij.gui.Roi;
import ij.VirtualStack;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JDialog;
import bsh.EvalError;
import bsh.Interpreter;

/**
 * ProcessPixels
 * 
 * A template for processing each pixel of either GRAY8, GRAY16, GRAY32 or
 * COLOR_RGB images.
 * 
 * @author The Fiji Team
 */
public class Measure_Fluorescence implements PlugIn,ActionListener,ItemListener {
	private int positions;
	private String roiStackName;
	private String fluorStackName;
	private ImageStack roiStack;
	private ImageStack fluorStack;
	private int stackSize;
	public final String LINE_SEPARATOR = "\n\r";
	private String dir;
	private String filePath;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-z");
	private boolean inputStackIsProcessed;
	private boolean callScript=false;
	private String beanshellPath=null;
	public double[] paParams=new double[4]; //String array with [minArea, maxArea,minCircularity,maxCircularity] for particle analyzer
	private double maxEccentricity;
	private VirtualStack preProcessedStack;
	private Button button1=new Button("Pause execution after current set?");
	private Button button2=new Button("Escape after current set?");
	private JDialog dialog=new JDialog();
	private boolean pause=false;
	private boolean escape=false;
	private Checkbox replaceImageAnalysisBox;

	/**
	 * @return 
	 * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
	 */
	public boolean setup() {
		IJ.log("If nothing is printed after this, there is a problem with the JDialog");
		button1.addActionListener(this);
		button1.setBounds(0, 0, 300, 50);
		button2.setBounds(0,150,300,50);
		dialog.add(button1);
		dialog.add(button2);
		//frame.setTitle("frame");
		//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //frame.setLayout(new FlowLayout());
		dialog.setSize(300, 150);
		dialog.setVisible(true);
		
		IJ.log("About to let the plugin run!"); 
		if (!IJ.showMessageWithCancel(
				"Preconditions",
				"This plugin works on Virtual Stacks and regular stacks that require <~60% of \n\r"
				+ "free RAM (exact fraction unknown). A stack that is too big to fit in RAM can \n\r"
				+ "be opened as a virtual stack. To open a stack as a virtual stack, use:\n\r"
				+ "File>Import>Image Sequence  and select the Virtual Stack option\n\n\r"
				+ "This plugin assumes that the stack is sorted, with the \n\r"
						+ "first frame corresponding to the first image at the first\n\r"
						+ "microscope position. If it is not sorted, use the tools in \n\r "
						+ "Image>Stacks>Tools>Stack Sorter.\n\r"
						+ "Each stack should correspond to an imaging channel. For\n\r"
						+ "example: an mCherry stack and a phase contrast stack")) {
			return false;
		}
		NonBlockingGenericDialog gd = new NonBlockingGenericDialog("Analysis parameters");
		gd.addMessage("If there are 5"
				+ " positions in a stack of 20, then \n\r"
				+ "it will be divided into 4 data sets of 5 images each");
		gd.addNumericField("Microscope positions per acquisition set (0,1...n-2,n-1)=n positions",1, 0);
		gd.addChoice(
				"Stack with images to find ROIs (cells) from (i.e. phase contrast image)",
				WindowManager.getImageTitles(), WindowManager.getCurrentImage()
						.getTitle());
		gd.addCheckbox("The \"find ROI\" stack has already been pre-processed?", false);
		gd.addCheckbox("Analyze images with custom Beanshell script instead of default?", false);
		gd.addChoice(
				"Stack with images to measure ROIs (cells) in (i.e. fluorescence image)",
				WindowManager.getImageTitles(), WindowManager.getCurrentImage()
						.getTitle());
		gd.addNumericField("Min cell area", 400, 3);
		gd.addNumericField("Max cell area", 12000, 3);
		gd.addSlider("Min circularity (4pi(area/perimeter^2)", 0.01, 1, 0.5);
		gd.addSlider("Max circularity (4pi(area/perimeter^2)", 0.01, 1, 1.0);
		gd.addNumericField("Max ratio of major axis to minor axis",3,3);
		loadListeners(gd);
		gd.showDialog();
		if (gd.wasCanceled()) {
			gd.dispose();
			return false;
		} else {
			this.positions = (int) gd.getNextNumber();
			this.roiStackName = gd.getNextChoice();
			this.inputStackIsProcessed=gd.getNextBoolean();
			this.callScript=gd.getNextBoolean();
			this.fluorStackName = gd.getNextChoice();
			this.paParams[0]=gd.getNextNumber();
			this.paParams[1]=gd.getNextNumber();
			this.paParams[2]=gd.getNextNumber();
			this.paParams[3]=gd.getNextNumber();
			this.maxEccentricity=gd.getNextNumber();
		}
		IJ.showMessage("Select the folder in which to save your results");
		this.dir = IJ.getDirectory("Folder in which to save results");
		this.filePath = dir + "summary" + sdf.format(new Date()) + ".csv";
		
		ImagePlus findRoiImg=WindowManager.getImage(roiStackName);
		if(findRoiImg.getOverlay()!=null){
			findRoiImg.getOverlay().clear();
		}
		this.roiStack=findRoiImg.getStack();
		this.stackSize = this.roiStack.getSize();
		
		//The properties of the ROIs will be measured from these
		ImagePlus fluorImg=WindowManager.getImage(fluorStackName);
		if(fluorImg.getOverlay()!=null){
			fluorImg.getOverlay().clear();
		}
		this.fluorStack=fluorImg.getStack();
		int fluorStackSize=this.fluorStack.getSize();

		//This ensures that the stacks have the same size
		if(this.stackSize!=fluorStackSize){
			IJ.log("---------ERROR!---------\n\r"
					+ "The two stacks should be the same size!\n\r"
					+ roiStackName +" contains "+stackSize+ " images but \n\r"
					+ fluorStackName+" contains "+fluorStackSize+" images!.\n\r"+
					"This plugin cannot proceed.");
			return false;
		}
		
		return true;
	}

	/**
	 * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
	 */
	@SuppressWarnings("unchecked")
	public void run(String arg) {
		if(!setup()){
			return;
		};
		//I save the Rois in a list because, from experimentation (not understanding), there can only be 1 RoiManager
		//and method calls to an apparently separate RoiManager affect all of them
		List<Roi> allRois=new LinkedList<Roi>();
		/*//12/23    saving Rois with Manager
		FileOutputStream roiFile=null;
		
		try {
			roiFile=new FileOutputStream(this.dir+"RoiSet.zip");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		RoiEncoder roiWriter=new RoiEncoder(roiFile);
		*/
		ImageStack tmpRoiStack=null;
		ImageStack tmpFluorStack=null;
		for (int curSlice = 1; curSlice <= this.stackSize; curSlice++) {
			ImageProcessor roiImgProc=this.roiStack.getProcessor(curSlice).duplicate();
			ImageProcessor fluorImgProc=this.fluorStack.getProcessor(curSlice);
			
			if(tmpRoiStack==null||tmpFluorStack==null){
				tmpRoiStack=new ImageStack(roiImgProc.getWidth(),roiImgProc.getHeight(),this.roiStack.getColorModel());
				tmpFluorStack=new ImageStack(fluorImgProc.getWidth(),fluorImgProc.getHeight(),this.fluorStack.getColorModel());
			}
			tmpRoiStack.addSlice(roiImgProc);
			tmpFluorStack.addSlice(fluorImgProc);
			if(curSlice%this.positions==0||curSlice==this.stackSize){
				int base=curSlice-this.positions;
				int set=(curSlice-1)/this.positions;
				IJ.log("Working on set " + set);
				if(this.preProcessedStack==null){
					this.preProcessedStack=new VirtualStack(tmpRoiStack.getWidth(),tmpRoiStack.getHeight(),tmpRoiStack.getColorModel(),this.dir);
				}
				String[] processedImageSavePaths=new String[tmpRoiStack.size()];
				String[] processedImageNames=new String[tmpRoiStack.size()];
				for(int i=1;i<=tmpRoiStack.size();i++){
					processedImageSavePaths[i-1]=dir+String.format("%05d", base+i)+"_Processed_"+this.roiStack.getSliceLabel(i+base);
					processedImageNames[i-1]=String.format("%05d", base+i)+"_Processed_"+this.roiStack.getSliceLabel(i+base);
				}
				List<Roi> setRois = null;
				if(this.callScript){
					//Use Beanshell script to analyze images
					Interpreter i = new Interpreter();  // Construct an interpreter
					try {
					i.set("roiStack", tmpRoiStack);                    				 // Set variables
					i.set("fluorStack", tmpFluorStack);                   			 // Set variables
					i.set("pathToCSV", this.filePath);                   			 // Set variables
					i.set("set", set);                    							 // Set variables
					i.set("paParams", paParams);      								 // Set variables
					i.set("maxEccentricity", this.maxEccentricity);      			 // Set variables
					i.set("pathToSaveProcessedImages", processedImageSavePaths);     // Set variables
					i.set("inputRoiStackIsProcessed", inputStackIsProcessed);        // Set variables
					// Source an external script file
					i.source(this.beanshellPath);
					setRois = (List<Roi>) i.get("roiList");    // retrieve a variable
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
															,this.filePath,set
															,paParams,this.maxEccentricity
															,processedImageSavePaths,inputStackIsProcessed);
					setRois=yfg.analyzeAndGetResults();
					yfg=null;
				}
				if(setRois!=null){
					for(Roi roi:setRois){
						int slice=roi.getPosition();
						roi.setPosition(slice+base);
						IJ.showStatus("Loading ROIs...");
						allRois.add(roi);  //12/22
					}
				}
				//save Rois every 20 sets and clear linked list to conserve memory
				
				if(set%5==0&&set>0){
					RoiManager rm=YeastFluorGetter.saveRois(allRois,this.dir+"RoiSetFromPreviousSetTo"+Integer.toString(set)+".zip");
					rm.reset();
					allRois.clear();
				}
				for(String imageName:processedImageNames){
					this.preProcessedStack.addSlice(imageName);
				}
				tmpRoiStack=null;
				tmpFluorStack=null;
				System.gc();
				if(escape)
					curSlice=this.stackSize+1;
				while(pause){
					IJ.showStatus("Execution paused");
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
			}
		}
		/*//using manager instead
		try {
			roiFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		//12/22
		RoiManager rm=YeastFluorGetter.saveRois(allRois,this.dir+"RoiSetFromPreviousSetTo"+Integer.toString((this.stackSize-1)/this.positions)+".zip");
		allRois.clear();
		
		ImagePlus findRoiImg=WindowManager.getImage(roiStackName);
		ImagePlus fluorImg=WindowManager.getImage(fluorStackName);
		if(findRoiImg.getOverlay()!=null){
			findRoiImg.getOverlay().clear();
		}
		if(fluorImg.getOverlay()!=null){
			fluorImg.getOverlay().clear();
		}
		rm.moveRoisToOverlay(findRoiImg);//12/22
		rm.moveRoisToOverlay(fluorImg);//12/22
		
		if(this.preProcessedStack.size()>0){
			ImagePlus newVirtualProcessedStack=new ImagePlus("Processed "+sdf.format(new Date()),this.preProcessedStack);
			newVirtualProcessedStack.show();
			if(newVirtualProcessedStack.getOverlay()!=null){
				newVirtualProcessedStack.getOverlay().clear();
			}

			rm.moveRoisToOverlay(newVirtualProcessedStack);//12/22
		}
		
	}
	
	private void loadListeners(GenericDialog gd){
		@SuppressWarnings("unchecked")
		Vector<Checkbox> checkboxes=(Vector<Checkbox>)gd.getCheckboxes();
		replaceImageAnalysisBox=checkboxes.get(1);
		replaceImageAnalysisBox.addItemListener(this);
		
		//addItemListener
		
	}
	public void itemStateChanged(ItemEvent e){
		if(e.getSource().equals(this.replaceImageAnalysisBox)&&this.replaceImageAnalysisBox.getState()){
			IJ.showMessage("Select the beanshell script to perform image analysis. It will be sent :\n\r\n\r"
					+ "The stack with images to find ROIs as an ImageStack variable called \t\t\t\"roiStack\"\n\r"
					+ "The stack with images to measure ROIs as an ImageStack variable called \t\t\t\"fluorStack\"\n\r"
					+ "The filepath where results will be saved as a CSV as a String called \t\t\t\"pathToCSV\"\n\r"
					+ "The current number of this set (current slice/positions) as an integer called \t\t\t\"set\"\n\r"
					+ "{min Area,max Area, min Circularity, max Circularity} as a String[] called \t\t\t\"paParams\"\n\r"
					+ "The maximum Eccentricity as a double called \t\t\t\t\t\"maxEccentricity\"\n\r"
					+ "A set of filepaths where processed images should be saved as a String[] called \t\"pathToSaveProcessedImages\"\n\r"
					+ "Whether the input stack does not need to be pre-processed as a boolean called \t\"inputRoiStackIsProcessed\"");
			
			this.beanshellPath = IJ.getFilePath("Select the external image analysis script. It will be sent filepaths to images");
		}

	}

	public void showAbout() {
		IJ.showMessage("ProcessPixels",
				"a template for processing each pixel of an image");
	}
	@Override
	public void actionPerformed(ActionEvent arg0) {
		if(arg0.getSource()==button1&&!pause){
			IJ.log("Will soon pause this execution");
			this.pause=true;
			button1.setLabel("Continue?");
			dialog.setVisible(false);
			dialog.setVisible(true);
		}else if(arg0.getSource()==button1&&pause){
			IJ.log("Will soon continue this execution");
			this.pause=false;
			button1.setLabel("Pause?");
			dialog.setVisible(false);
			dialog.setVisible(true);
		}else if(arg0.getSource()==button2&&!escape){
			IJ.log("Will soon halt this execution");
			this.escape=true;
			button2.setLabel("Undo escape?");
			dialog.setVisible(false);
			dialog.setVisible(true);
		}else if(arg0.getSource()==button2&&escape){
			IJ.log("Execution will continue");
			this.escape=false;
			button2.setLabel("Escape?");
			dialog.setVisible(false);
			dialog.setVisible(true);
		}
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}


}
