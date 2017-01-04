package analyzeBioreactorImage2;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Font;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import util.opencsv.CSVReader;



public class ParametersGetter implements ItemListener,ActionListener,TextListener {
	private String port = "COM4";
	private final double interval = 30;
	private double mediaPumpRatio = 0.5;
	private double samplePumpRatio = 0.5;
	private int backgroundImages = 5;
	private double settlingDelay = 20;
	private String roiGroup;
	private String roiPreset;
	private String intensityGroup;
	private String intensityPreset;
	private String darkGroup;
	private String darkPreset;
	private boolean reloadTimecourse = false;
	private double minSetDelay = 3;
	private int iterations = 1000;;
	private String[] groupOptions;
	private String[] presetOptions;
	private String[] acqGroups;
	private String[] acqPresets;
	private String saveImgDir;
	private String saveMetaDir;
	private String imageAnalysisScript;
	private String feedbackScript;
	private Font boldFont = new Font("Serif", Font.BOLD, 36);
	private  List<String[]> LED_timecourse = new LinkedList<String[]>();
	private String bgOperationFind = "Difference";
	private String bgOperationAnalyze = "Subtract";
	private String[] bgOperations= { bgOperationFind, bgOperationAnalyze };
	private boolean loadSettingsFromCsv=false;
	private String pathToSettings="";
	private boolean loadTimecourseFromCsv=false;
	private String pathToTimecourse="";
	private boolean callExternalImageAnalysis=false;
	private String pathToExternalImageAnalysis="";
	private Checkbox loadSettingsBox;
	private Checkbox loadTimecourseBox;
	private Checkbox replaceImageAnalysisBox;
	private Checkbox callFeedbackScriptBox;
	private Checkbox sendTimecourseAndRatiosNow;
	private final String LINESEPARATOR="\r\n";
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-z");
	private String curTime;
	private static final String imageTypeName="Tiff";
	private static final String imageTypeSuffix=".tiff";
	private String title;
	private int numPositions;
	private GenericDialog genD;
	public static final int secondsColumn=0;
	public static final int LEDsColumn=1;
	public static final int pwmColumn=2;
	public static final int valsRow=1; //The headers row should be at 0. This is the first row with values that I care about
	public double[] paParams={1000,10000,0.5,1.0}; //String array with [minArea, maxArea,minCircularity,maxCircularity] for particle analyzer
	private double maxEccentricity=3;
	private Button escapeButton=new Button("Escape after current set?");
	private JDialog dialog=new JDialog();
	private boolean escape=false;
	private Plot hoursCourse;
	private static final int plotLineWidth=5;
	private bioController_Nov caller=null;
	

	
	

	public ParametersGetter(String title, int numPositions,String[] cfg_groups, String[] cfg_presets,bioController_Nov caller) {
		if (this.LED_timecourse.size() == 0) {
			this.LED_timecourse.add(new String[] { "10", "0", "0" });
			this.LED_timecourse.add(new String[] { "20", "0", "0" });
			this.LED_timecourse.add(new String[] { "30", "0", "0" });
			this.LED_timecourse.add(new String[] { "40", "0", "0" });
			this.LED_timecourse.add(new String[] { "50", "0", "0" });
			this.LED_timecourse.add(new String[] { "60", "32", "8" });
			this.LED_timecourse.add(new String[] { "120", "0", "0" });
			this.LED_timecourse.add(new String[] { "180", "64", "15" });
			this.LED_timecourse.add(new String[] { "240", "0", "0" });
			this.LED_timecourse.add(new String[] { "300", "0", "0" });
		}
		this.title=title;
		this.numPositions=numPositions;
		this.groupOptions=cfg_groups;
		this.presetOptions=cfg_presets;
		this.roiGroup=this.groupOptions[0];
		this.roiPreset=this.presetOptions[0];
		this.caller=caller;
	}

	/*
	 * if(this.LED_timecourse.size()==0){ this.LED_timecourse = fillTimecourse(
	 * "Pre-load 1 of 2 (OPTIONAL) Select timecourse CSV [seconds, LEDs on, PWM], or Cancel and set later"
	 * ); }
	 * 
	 * if(!brSettings.modified){ boolean loadSuccess=brSettings.loadFromFile(
	 * "P 2 of 2 (OPTIONAL) Select the Bioreactor Controller settings CSV file, or Cancel and set later"
	 * ,cfg_groups,cfg_presets); if(!loadSuccess){
	 * brSettings.loadDefault(cfg_groups, cfg_presets); } }
	 */
	// Returns true if the dialog was Okayed. REturns false if it was cancelled.
	public boolean showDialog() {
		prepareDialog();
		if (this.genD.wasCanceled()) {
			readDialogInputs(this.genD);
			this.genD.dispose();
			return false;
		} else {
			readDialogInputs(this.genD);
			this.saveImgDir=IJ.getDirectory("Directory in which images will be saved");
			this.saveMetaDir=IJ.getDirectory("Directory in which meta-data will be saved");
			this.curTime=sdf.format(new Date());
			IJ.log("Saving settings to: "+this.saveMetaDir+"Settings"+this.curTime+".csv");
			saveSettingsToCsv();
			IJ.log("Saving timecourse to: "+this.saveMetaDir+"timecourse"+this.curTime+".csv");
			saveTimecourseToCsv();
			IJ.log("ready to run!");
			if(IJ.showMessageWithCancel("FINAL MESSAGE", "Begin acquiring images (pump ratios and LED timecourse will be sent)?")){
				escapeButton.addActionListener(this);
				dialog.add(escapeButton);
				//frame.setTitle("frame");
				//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		        //frame.setLayout(new FlowLayout());
				dialog.setSize(100, 200);
				dialog.setVisible(true);
				return true;
			}else{return false;}
		}

	}
	
	public void prepareDialog(){
		this.genD = loadDialog();
		loadListeners(this.genD);
		this.genD.showDialog();
	}
	
	public void plotTimecourse(List<String[]> timecourse){
		/*
		 * So,a tricky part here is that I need to enter the values so that they are displayed as a 
		 * step function. For example, instead of [0,0],[5,1], I really want [0,0],[4.99,0],[5,1]
		 */
		if(this.hoursCourse!=null){
			PlotWindow pt=this.hoursCourse.show();
			pt.setVisible(false);
			pt.dispose();
			this.hoursCourse.dispose();
		}
		int size=timecourse.size();
		double[] hours=new double[size*2-1];
		double[] intensities=new double[size*2-1];
		int i=0;
		for(String[] row:timecourse){
			double seconds=Double.parseDouble(row[secondsColumn]);
			double intensity=Double.parseDouble(row[LEDsColumn])*Double.parseDouble(row[pwmColumn]);
			if(i==0){
				hours[i]=seconds/3600.0;
				intensities[i]=intensity;
			}else{
				hours[2*i-1]=(seconds-0.1)/3600.0;
				intensities[2*i-1]=intensities[2*i-2];
				hours[2*i]=(seconds)/3600.0;
				intensities[2*i]=intensity;
			}
			i++;
		}
		this.hoursCourse=new Plot("LED Timecourse", "time(hours)", "number of LEDs x pwm (~intensity)", hours, intensities);
		this.hoursCourse.setLineWidth(plotLineWidth);
		PlotWindow pt=this.hoursCourse.show();
		pt.setVisible(true);
	}
	public void textValueChanged(TextEvent e){
		String dubiousTimes=this.genD.getTextArea1().getText();
		String dubiousSettings=this.genD.getTextArea2().getText();
		this.LED_timecourse=getSafeTimecourse(dubiousTimes, dubiousSettings);
		if(this.LED_timecourse.size()>0){
			plotTimecourse(this.LED_timecourse);
		}
		
	}
	
	public void itemStateChanged(ItemEvent e){
		if(e.getSource().equals(this.loadSettingsBox)&&this.loadSettingsBox.getState()){
			if(loadSettingsFromCsv()){
				resetDialog();
			}else{
				loadSettingsBox.setState(false);
			}
		}
		if(e.getSource().equals(this.loadTimecourseBox)&&this.loadTimecourseBox.getState()){
			if(loadTimecourseFromCsv()){
				resetDialog();
			}else{
				loadTimecourseBox.setState(false);
			}
		}
		if(e.getSource().equals(this.replaceImageAnalysisBox)&&this.replaceImageAnalysisBox.getState()){
			IJ.showMessage("Select the external image analysis script. It will be sent filepaths to images");
			this.pathToExternalImageAnalysis = IJ.getFilePath("Select the external image analysis script. It will be sent filepaths to images");
			if(this.pathToExternalImageAnalysis==null)
				replaceImageAnalysisBox.setState(false);
		}
		if(e.getSource().equals(this.sendTimecourseAndRatiosNow)&&this.sendTimecourseAndRatiosNow.getState()){
			sendNow();
			sendTimecourseAndRatiosNow.setState(false);
		}
		
		//callExternalImageAnalysis
		// pathToExternalImageAnalysis
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		if(arg0.getSource()==escapeButton&&!escape){
			IJ.log("Will soon halt this execution");
			this.escape=true;
			escapeButton.setLabel("Undo escape?");
			dialog.setVisible(false);
			dialog.setVisible(true);
		}else if(arg0.getSource()==escapeButton&&escape){
			IJ.log("Execution will continue");
			this.escape=false;
			escapeButton.setLabel("Escape?");
			dialog.setVisible(false);
			dialog.setVisible(true);
		}
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public void sendNow(){
		Vector<TextField> numericTextFields=(Vector<TextField>)this.genD.getNumericFields();
		TextField mediaPumpRatio=numericTextFields.get(0);
		TextField samplePumpRatio=numericTextFields.get(1);
		double mediaRatio=getValue(mediaPumpRatio.getText());
		double sampleRatio=getValue(samplePumpRatio.getText());
		
		Vector<TextField> stringTextFields=(Vector<TextField>)this.genD.getStringFields();
		String comPort=stringTextFields.get(0).getText();
		//this.LED_timecourse
		caller.sendImmediateMessage(mediaRatio, sampleRatio,comPort,null);
		caller.loadTimeCourse(LED_timecourse,comPort);
		IJ.showMessage("pump ratios and LED timecourse were sent");
	}
	
 	public double getValue(String text) {
 		Double d;
 		try {d = new Double(text);}
		catch (NumberFormatException e){
			d = null;
		}
		return d.doubleValue();
	}
	
	public void resetDialog(){
		this.genD.dispose();
		this.genD=null;
		prepareDialog();
	}

	private GenericDialog loadDialog() {
		if(this.genD==null){
			this.genD = new GenericDialog(this.title);
		}
		if (this.numPositions == 0) {
			this.genD.addMessage("WARNING! The stage position list is empty.",
					boldFont);
			this.genD.addMessage("Images will only be acquired at the current position.");
		} else {
			this.genD.addMessage("Images will be acquired at the " + this.numPositions
					+ (this.numPositions < 2 ? " position" : " positions")
					+ " in the stage position list.");
		}
		this.genD.addCheckbox("Load settings for this from CSV (settings are saved automatically)?",loadSettingsFromCsv);
		this.genD.addCheckbox("Load LED time-course from CSV (timecourse is saved automatically)?",loadTimecourseFromCsv);
		this.genD.addCheckbox("Replace default image analysis with call to Beanshell script?",this.callExternalImageAnalysis);
		this.genD.addCheckbox("Reload LED time course settings after analyzing images (enables feedback control)?",this.reloadTimecourse);
		this.genD.addCheckbox("Send current pump ratios and LED timecourse to microcontroller now (useful for calibrating pumps and testing LED)?)",this.reloadTimecourse);
		this.genD.addStringField("Microcontroller Port", this.port);

		this.genD.addSlider("Media pump ratio on", 0.001, 1.0, this.mediaPumpRatio);
		this.genD.addSlider("Sample pump ratio on", 0.001, 1.0, this.samplePumpRatio);
		this.genD.addSlider("Background samples", 0, 7, this.backgroundImages);
		
		this.genD.addNumericField(
				"Delay before image is taken (time for cells to settle)",
				this.settlingDelay, 0, 8, "seconds");
		this.genD.addMessage("Time (seconds)      LEDs on,PWM");
		String seconds = null;
		String numLEDS_pwm = null;
		// post
		if (LED_timecourse.size() > 0) {
			seconds = LED_timecourse.get(0)[0];
			numLEDS_pwm = LED_timecourse.get(0)[1] + ","
					+ LED_timecourse.get(0)[2];
		}
		// fence-post,fence-post ...
		for (int i = 1; i < LED_timecourse.size(); i++) {
			seconds = seconds + "\n" + LED_timecourse.get(i)[0];
			numLEDS_pwm = numLEDS_pwm + "\n" + LED_timecourse.get(i)[1] + ","
					+ LED_timecourse.get(i)[2];
		}

		this.genD.addTextAreas(seconds, numLEDS_pwm, 10, 10);

		this.genD.addChoice("Find ROIs from images in this group", this.groupOptions,
				this.roiGroup);
		this.genD.addChoice("Find ROIs from images in this preset",
				this.presetOptions, this.roiPreset);
		this.genD.addChoice("Measure intensity from ROIs in this group",
				this.groupOptions, this.intensityGroup);
		this.genD.addChoice("Measure intensity from ROIs in this preset",
				this.presetOptions, this.intensityPreset);
		this.genD.addChoice("Between image acquisitions, remain in this group",
				this.groupOptions, this.darkGroup);
		this.genD.addChoice("Between image acquisitions, remain at this preset",
				this.presetOptions, this.darkPreset);
		this.genD.addNumericField(
				"Minimum sampling pump on time between acquisitions",
				this.minSetDelay, 0, 8, "minutes");
		this.genD.addNumericField(
				"Number of iterations of acquiring and analyzing images",
				this.iterations, 0, 8, "iterations");
		this.genD.addNumericField("Min cell area", this.paParams[0], 3);
		this.genD.addNumericField("Max cell area", this.paParams[1], 3);
		this.genD.addSlider("Min circularity (4pi(area/perimeter^2)", 0.01, 1, this.paParams[2]);
		this.genD.addSlider("Max circularity (4pi(area/perimeter^2)", 0.01, 1, this.paParams[3]);
		this.genD.addNumericField("Max ratio of major axis to minor axis",this.maxEccentricity,3);
		this.genD.setOKLabel("PROCEED: Select directories to save data and scripts if applicable");
		return this.genD;
	}
	
	private void loadListeners(GenericDialog gd){
		@SuppressWarnings("unchecked")
		Vector<Checkbox> checkboxes=(Vector<Checkbox>)gd.getCheckboxes();
		loadSettingsBox=checkboxes.get(0);
		loadSettingsBox.addItemListener(this);
		loadTimecourseBox=checkboxes.get(1);
		loadTimecourseBox.addItemListener(this);
		replaceImageAnalysisBox=checkboxes.get(2);
		replaceImageAnalysisBox.addItemListener(this);
		callFeedbackScriptBox=checkboxes.get(3);
		callFeedbackScriptBox.addItemListener(this);
		sendTimecourseAndRatiosNow=checkboxes.get(4);
		sendTimecourseAndRatiosNow.addItemListener(this);
		//addItemListener
		TextArea ta1=gd.getTextArea1();
		ta1.addTextListener(this);
		TextArea ta2=gd.getTextArea2();
		ta2.addTextListener(this);		
	}

	private void readDialogInputs(GenericDialog genD) {
		this.loadSettingsFromCsv=genD.getNextBoolean();
		this.loadTimecourseFromCsv=genD.getNextBoolean();
		this.callExternalImageAnalysis=genD.getNextBoolean();
		this.reloadTimecourse = genD.getNextBoolean();
		//discard the checkbox for whether or not the messages were sent immediately to the microcontroller
		genD.getNextBoolean();
		this.port = genD.getNextString();
		// saveImgenDir=genD.getNextString();
		this.mediaPumpRatio = genD.getNextNumber();
		this.samplePumpRatio = genD.getNextNumber();
		this.backgroundImages = (int) genD.getNextNumber();
		this.settlingDelay = genD.getNextNumber();
		
		String tempTimecourse = genD.getNextText();
		String tempLEDcourse = genD.getNextText();
		this.LED_timecourse.clear();
		this.LED_timecourse=getSafeTimecourse(tempTimecourse, tempLEDcourse);
		
		this.roiGroup = genD.getNextChoice();
		this.roiPreset = genD.getNextChoice();
		this.intensityGroup = genD.getNextChoice();
		this.intensityPreset = genD.getNextChoice();
		this.darkGroup = genD.getNextChoice();
		this.darkPreset = genD.getNextChoice();
		this.minSetDelay = genD.getNextNumber();
		this.iterations = (int) genD.getNextNumber();
		this.paParams[0]=this.genD.getNextNumber();
		this.paParams[1]=this.genD.getNextNumber();
		this.paParams[2]=this.genD.getNextNumber();
		this.paParams[3]=this.genD.getNextNumber();
		this.maxEccentricity=this.genD.getNextNumber();

		acqGroups = new String[] { roiGroup, intensityGroup };
		acqPresets = new String[] { roiPreset, intensityPreset };
		
		plotTimecourse(this.LED_timecourse);
	}
	
	   public List<String[]> getSafeTimecourse(String dubiousTimeString,String dubiousLEDSettingString){
		   List<String[]> safeTimecourse=new LinkedList<String[]>();
		   String[] first={"0","0","0"};
		   safeTimecourse.add(first);
		   double prevTime=0;
			String[] dubiousTimes = dubiousTimeString.replaceAll("\r", "").split("\n");
			String[] dubiousLEDSettings = dubiousLEDSettingString.replaceAll("\r", "").split("\n");
		   for(int i=0;i<dubiousTimes.length&&i<dubiousLEDSettings.length;i++){
			   String[] safeRow=new String[3];
			   Integer seconds=null;
			   Integer numLEDs=null;
			   Integer pwm=null;
			   if(dubiousTimes[i]!=null){
				   try{
					   seconds=Integer.parseInt(dubiousTimes[i]);
					   if(seconds<0)
						   seconds=seconds*-1;
					   if(seconds<prevTime){
						   seconds=null;
					   }else{
						   prevTime=seconds;
					   }
				   }catch(NumberFormatException e){
					   seconds=null;
				   }
			   }
			   
			   String[] dubiousLEDSettingsRow=dubiousLEDSettings[i].split(",");
			   if(dubiousLEDSettingsRow.length>=2
					   &&dubiousLEDSettingsRow[0]!=null
					   &&dubiousLEDSettingsRow[1]!=null){
				   try{
					   numLEDs=Integer.parseInt(dubiousLEDSettingsRow[0]);
					   pwm=Integer.parseInt(dubiousLEDSettingsRow[1]);
				   }catch(NumberFormatException e){
					   numLEDs=null;
					   pwm=null;
				   }
			   	}
			   if(seconds!=null
					   &&numLEDs!=null
					   &&pwm!=null){
				   if(numLEDs<0)
					   numLEDs=numLEDs*-1;
				   if(numLEDs>64)
					   numLEDs=64;
				   if(pwm<0)
					   pwm=pwm*-1;
				   if(pwm>15)
					   pwm=15;
				   safeRow[secondsColumn]=seconds.toString();
				   safeRow[LEDsColumn]=numLEDs.toString();
				   safeRow[pwmColumn]=pwm.toString();
				   safeTimecourse.add(safeRow);
			   }
		   }
		   
		   
		   return safeTimecourse;
	   }

	
	 public boolean loadSettingsFromCsv(){ 
		 //File settings from CSV if available
		 JFileChooser chooser = new JFileChooser();
		 chooser.setDialogTitle("Select the \".csv\" file containing experiment settings");
		 FileNameExtensionFilter filter2 = new FileNameExtensionFilter( "comma seperated values", "csv");
		 chooser.setFileFilter(filter2); 
		 if(chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) { 
			 this.pathToSettings=chooser.getSelectedFile().getAbsolutePath();
			 return loadSettingsFromCsv(this.pathToSettings);
		 	}else{
		 		return false;
		 	}
		 }
	 
	 public boolean loadSettingsFromCsv(String path){
		 try { 
			 CSVReader settings_csv = new	 CSVReader(new BufferedReader(new FileReader(path)));
			 settings_csv.readNext(); //Ignore this, since the next line should always be true
			 this.loadSettingsFromCsv=true;
			 settings_csv.readNext(); //Ignore this, since we have the path
			 this.pathToSettings=path;
			 this.loadTimecourseFromCsv=Boolean.getBoolean(settings_csv.readNext()[1]);
			 this.pathToTimecourse=settings_csv.readNext()[1];
			 this.callExternalImageAnalysis=Boolean.getBoolean(settings_csv.readNext()[1]);
			 this.pathToExternalImageAnalysis=settings_csv.readNext()[1];
			 this.reloadTimecourse=Boolean.getBoolean(settings_csv.readNext()[1]);
			 this.port=settings_csv.readNext()[1];
			 this.mediaPumpRatio=Double.parseDouble(settings_csv.readNext()[1]);
			 this.samplePumpRatio=Double.parseDouble(settings_csv.readNext()[1]);
			 this.backgroundImages=Integer.parseInt(settings_csv.readNext()[1]);
			 this.settlingDelay=Double.parseDouble(settings_csv.readNext()[1]);
			 this.roiGroup=settings_csv.readNext()[1];
			 this.roiPreset=settings_csv.readNext()[1];
			 this.intensityGroup=settings_csv.readNext()[1];
			 this.intensityPreset=settings_csv.readNext()[1];
			 this.darkGroup=settings_csv.readNext()[1];
			 this.darkPreset=settings_csv.readNext()[1];
			 this.minSetDelay=Double.parseDouble(settings_csv.readNext()[1]);
			 this.iterations=Integer.parseInt(settings_csv.readNext()[1]);
			 this.paParams[0]=Double.parseDouble(settings_csv.readNext()[1]);
			 this.paParams[1]=Double.parseDouble(settings_csv.readNext()[1]);
			 this.paParams[2]=Double.parseDouble(settings_csv.readNext()[1]);
			 this.paParams[3]=Double.parseDouble(settings_csv.readNext()[1]);
			 this.maxEccentricity=Double.parseDouble(settings_csv.readNext()[1]);
		 } catch (FileNotFoundException e) {
			 e.printStackTrace();
			 return false;
		 } catch (IOException e) {
				 e.printStackTrace();
				 return false;
		 }
		 return true;
	 }
	 
	 
	 public void saveSettingsToCsv(){ 
			 FileWriter settings;
			try {
				 settings = new FileWriter(this.saveMetaDir+"Settings"+this.curTime+".csv"); 
				 settings.write("Load settings from CSV?,"+Boolean.toString(this.loadSettingsFromCsv)+LINESEPARATOR);
				 settings.write("Path to settings,"+this.pathToSettings+LINESEPARATOR);
				 settings.write("Load LED time-course from CSV?,"+Boolean.toString(this.loadTimecourseFromCsv)+LINESEPARATOR);
				 settings.write("Path to LED time-course,"+this.pathToTimecourse+LINESEPARATOR);
				 settings.write("Replace default image analysis with call to Beanshell script?,"+Boolean.toString(this.callExternalImageAnalysis)+LINESEPARATOR);
				 settings.write("Path to external script,"+this.pathToExternalImageAnalysis+LINESEPARATOR);
				 settings.write("Reload LED timecourse after analyzing images?,"+Boolean.toString(this.reloadTimecourse)+LINESEPARATOR);
				 settings.write("Port,"+port+LINESEPARATOR);
				 settings.write("Media Pump Ratio,"+Double.toString(mediaPumpRatio)+LINESEPARATOR);
				 settings.write("Sample Pump Ratio,"+Double.toString(samplePumpRatio)+LINESEPARATOR);
				 settings.write("Background Images,"+Integer.toString(backgroundImages)+LINESEPARATOR);
				 settings.write("Settling Delay,"+Double.toString(settlingDelay)+LINESEPARATOR);
				 settings.write("ROI Group,"+roiGroup+LINESEPARATOR);
				 settings.write("ROI Preset,"+roiPreset+LINESEPARATOR);
				 settings.write("Measure Intensity Group,"+intensityGroup+LINESEPARATOR);
				 settings.write("Measure Intensity Preset,"+intensityPreset+LINESEPARATOR);
				 settings.write("Dark Group,"+darkGroup+LINESEPARATOR);
				 settings.write("Dark Preset,"+darkPreset+LINESEPARATOR);
				 settings.write("Minimum delay between sets (min),"+Double.toString(minSetDelay)+LINESEPARATOR);
				 settings.write("Iterations,"+Integer.toString(iterations)+LINESEPARATOR);
				 settings.write("Min cell area,"+Double.toString(this.paParams[0])+LINESEPARATOR);
				 settings.write("Max cell area,"+Double.toString(this.paParams[1])+LINESEPARATOR);
				 settings.write("Min circularity,"+Double.toString(this.paParams[2])+LINESEPARATOR);
				 settings.write("Max circularity,"+Double.toString(this.paParams[3])+LINESEPARATOR);
				 settings.write("Max eccentricity,"+Double.toString(this.maxEccentricity)+LINESEPARATOR);
				 settings.close();
			} catch (IOException e) {
				e.printStackTrace();
				}
			 
		 }
	 
	 public boolean loadTimecourseFromCsv(){ 
		 //File settings from CSV if available
		 JFileChooser chooser = new JFileChooser();
		 chooser.setDialogTitle("Select the \".csv\" file containing timecourse [seconds, LEDs on, PWM]");
		 FileNameExtensionFilter filter2 = new FileNameExtensionFilter( "comma seperated values", "csv");
		 chooser.setFileFilter(filter2); 
		 if(chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) { 
			 this.pathToTimecourse=chooser.getSelectedFile().getAbsolutePath();
			 return loadTimecourseFromCsv(this.pathToTimecourse);
		 	}else{
		 		return false;
		 	}
		 }
	 
	 public boolean loadTimecourseFromCsv(String path){
		 try { 
			 CSVReader timecourse_csv = new	 CSVReader(new BufferedReader(new FileReader(path)));
			 @SuppressWarnings("unchecked")
			List<String[]> timeCourse=timecourse_csv.readAll();
			 this.LED_timecourse.clear();
			 int rowCounter=0;
			 for(String[] row:timeCourse){
				 if(rowCounter<valsRow){
					 //iterate past the header. valsRow is the first row of values for the timecourse
					 rowCounter++;
				 }else{
					 this.LED_timecourse.add(row);
				 }
			 }
			
			 } catch (FileNotFoundException e) {
				 e.printStackTrace();
				 return false;
			 } catch (IOException e) {
				 e.printStackTrace();
				 return false;
			 }
		 return true;
	 }
	 
	 public void saveTimecourseToCsv(){ 
		 FileWriter timecourse;
		try {
			 timecourse = new FileWriter(this.saveMetaDir+"timecourse"+this.curTime+".csv");
			 String[] headers=new String[3];
			 headers[secondsColumn]="Time (seconds)";
			 headers[LEDsColumn]="number of LEDs";
			 headers[pwmColumn]="PWM of LEDs";
			 String headerString=Arrays.toString(headers);
			 timecourse.write(headerString.replace("[", "").replace("]", "")+LINESEPARATOR);
			 //IJ.log("wrote: "+Arrays.toString(headers).replaceFirst("[", "").replaceAll("]", ""));  //maybe bad code
			 for(String[] vals:this.LED_timecourse){
				 timecourse.write(Arrays.toString(vals).replace("[", "").replace("]", "")+LINESEPARATOR);
			 }
			 timecourse.close();
		} catch (IOException e) {
			e.printStackTrace();
			}
		 
	 }
	 
	public String toString() {
		String timecourse = "";
		for (int i=0;i<LED_timecourse.size();i++){
			timecourse= timecourse+Arrays.toString(LED_timecourse.get(i))+"\n\r";
		}
		return " Port: " + port
				+ "\n\r mediaPumpRatio: " + Double.toString(mediaPumpRatio)
				+ "\n\r samplePumpRatio: " + Double.toString(samplePumpRatio)
				+ "\n\r Background Images to take: "
				+ Integer.toString(backgroundImages) + "\n\r Settling delay: "
				+ Double.toString(settlingDelay) + "\n\r ROI Group: "
				+ roiGroup + "\n\r ROI Preset: " + roiPreset
				+ "\n\r Intensity Group: " + intensityGroup
				+ "\n\r Intensity Preset: " + intensityPreset
				+ "\n\r Dark Group: " + darkGroup + "\n\r Dark Preset: "
				+ darkPreset + "\n\r Analysis Option: "
				+ Boolean.toString(callExternalImageAnalysis) + "\n\r Feedback? "
				+ "\n\r Reload Timecourse Each Iteration? "
				+ Boolean.toString(reloadTimecourse)
				+ "\n\r Minimum delay between iterations: "
				+ Double.toString(minSetDelay) + "\n\r Itrations: "
				+ Integer.toString(iterations) + "\n\r analysisOptions: "
				+ Arrays.toString(groupOptions) + "\n\r preset Options"
				+ Arrays.toString(presetOptions)
				+ "\n\r save image directory: " + saveImgDir
				+ "\n\r save meta-data directory: " + saveMetaDir
				+ "\n\r Image analysis script: " + imageAnalysisScript
				+ "\n\r Feedback Script: " + feedbackScript
				+ "\n\r Acquisition Groups: " + Arrays.toString(acqGroups)
				+ "\n\r Acquisition Presets: " + Arrays.toString(acqPresets)
				+ "\n\r Background Operations " + Arrays.toString(bgOperations)
				+ "\n\r LED time course \n\r[Seconds, number of LEDs, pwm]\n\r"+timecourse;

	}

	/**
	 * @return the lED_timecourse
	 */
	public List<String[]> getLED_timecourse() {
		return LED_timecourse;
	}

	/**
	 * @param lED_timecourse
	 *            the lED_timecourse to set
	 */
	public void setLED_timecourse(List<String[]> lED_timecourse) {
		LED_timecourse = lED_timecourse;
	}

	/**
	 * @return the port
	 */
	public String getPort() {
		return port;
	}

	/**
	 * @return the interval
	 */
	public double getInterval() {
		return interval;
	}
	
	public double[] getPaParams() {
		return paParams;
	}
	
	public String getpathToExternalImageAnalysis(){
		return pathToExternalImageAnalysis;
	}
	
	public double getMaxEccentricity() {
		return maxEccentricity;
	}

	/**
	 * @return the mediaPumpRatio
	 */
	public double getMediaPumpRatio() {
		return mediaPumpRatio;
	}

	/**
	 * @return the samplePumpRatio
	 */
	public double getSamplePumpRatio() {
		return samplePumpRatio;
	}

	/**
	 * @return the backgroundImages
	 */
	public int getBackgroundImages() {
		return backgroundImages;
	}

	/**
	 * @return the settlingDelay
	 */
	public double getSettlingDelay() {
		return settlingDelay;
	}

	/**
	 * @return the roiGroup
	 */
	public String getRoiGroup() {
		return roiGroup;
	}

	/**
	 * @return the roiPreset
	 */
	public String getRoiPreset() {
		return roiPreset;
	}

	/**
	 * @return the intensityGroup
	 */
	public String getIntensityGroup() {
		return intensityGroup;
	}

	/**
	 * @return the intensityPreset
	 */
	public String getIntensityPreset() {
		return intensityPreset;
	}

	/**
	 * @return the darkGroup
	 */
	public String getDarkGroup() {
		return darkGroup;
	}

	/**
	 * @return the darkPreset
	 */
	public String getDarkPreset() {
		return darkPreset;
	}
	
	public boolean isEscaped(){
		return escape;
	}


	/**
	 * @return the reloadTimecourse
	 */
	public boolean isReloadTimecourse() {
		return reloadTimecourse;
	}

	/**
	 * @return the minSetDelay
	 */
	public double getMinSetDelay() {
		return minSetDelay;
	}

	/**
	 * @return the iterations
	 */
	public int getIterations() {
		return iterations;
	}


	public boolean isCallExternalImageAnalysis(){
		return callExternalImageAnalysis;
	}



	/**
	 * @return the acqGroups
	 */
	public String[] getAcqGroups() {
		return acqGroups;
	}

	/**
	 * @return the acqPresets
	 */
	public String[] getAcqPresets() {
		return acqPresets;
	}

	/**
	 * @return the bgOperations
	 */
	public String[] getBgOperations() {
		return bgOperations;
	}

	/**
	 * @return the saveImgDir
	 */
	public String getSaveImgDir() {
		return saveImgDir;
	}

	/**
	 * @return the saveMetaDir
	 */
	public String getSaveMetaDir() {
		return saveMetaDir;
	}

	/**
	 * @return the imageAnalysisScript
	 */
	public String getImageAnalysisScript() {
		return imageAnalysisScript;
	}

	/**
	 * @return the feedbackScript
	 */
	public String getFeedbackScript() {
		return feedbackScript;
	}

	public String getImageTypeSuffix() {
		return imageTypeSuffix;
	}

	public String getImageTypeName() {
		return imageTypeName;
	}
}
