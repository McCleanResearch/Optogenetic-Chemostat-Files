package analyzeBioreactorImage2;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.plugin.PlugIn;

import java.util.*;

import util.opencsv.CSVReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.awt.Color;
import java.awt.Font;

import org.tc33.jheatchart.HeatChart;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.chart.renderer.xy.XYBlockRenderer;

import java.text.SimpleDateFormat;

import javax.swing.JPanel; 

import org.jfree.chart.ChartPanel; 
import org.jfree.chart.JFreeChart; 
import org.jfree.chart.axis.NumberAxis; 
import org.jfree.chart.plot.XYPlot; 
import org.jfree.data.xy.XYZDataset; 
import org.jfree.chart.renderer.GrayPaintScale; 
import org.jfree.chart.renderer.PaintScale; 
import org.jfree.ui.ApplicationFrame; 
import org.jfree.ui.RefineryUtilities; 

import ij.gui.Plot;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.Math;
public class Analyze_BioreactorData implements PlugIn {
	
	private String csvPath;
	private List<String[]> allVals;
	private List<Double[]> histVals;
	private double[][] arrayOfHistograms;
	private int histValIndex=6;
	private int histBinIndex;
	private Double maxHistVal;
	private Double minHistVal;
	private Integer numBins;
	private String saveDir;
	private String heatMapImagePath;
	private String title;
	private String xAxis;
	private String yAxis;
	private Font tr;
	private Font trb;
	private double range;
	private double binWidth;
	private String heatMapPlotPath;
	private int xFrequency;
	private int yFrequency;
	private String yAxisExp;
	private int scalar;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-z");
	private double maxBinSize;
	private double[] lnBinEdges;
	private double[] binEdges;
	private double xSpacing;
	
	
	@SuppressWarnings("unchecked")
	private boolean setup(){
		if(!IJ.showMessageWithCancel("","Ensure that JHeatChart.jar is in the Fiji/Jars folder, \n\r then select the CSV file with your results"))
			return false;
		this.csvPath = IJ.getFilePath("CSV file with image analysis results");
		FileReader file;
		try {
			file = new FileReader(this.csvPath);
			CSVReader csv=new CSVReader(file);
			this.allVals=csv.readAll();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			IJ.showMessage("File not found. Cannot continue");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			IJ.showMessage("Input/Output Exception. Cannot continue");
			e.printStackTrace();
		}
		if(!IJ.showMessageWithCancel("","Select the directory in which to save the results"))
			return false;
		this.saveDir = IJ.getDir("Directory in which to save the results");
		
		int maxIndex=allVals.get(0).length-1;
		if(maxIndex<6){
			histValIndex=maxIndex;
		}
		GenericDialog gd = new GenericDialog("Plot heatmap (each column is normalized)");
		gd.addChoice("Values to histogram (i.e. Max Fluorescence)", allVals.get(0), allVals.get(0)[histValIndex]);
		gd.addChoice("Values to form histogram bins (i.e. Set)", allVals.get(0), allVals.get(0)[maxIndex]);
		gd.addNumericField("Max fluorescence value to bin (will be converted to log value)", 175, 1);
		gd.addNumericField("Min fluorescence value to bin (will be converted to log value)", 25, 1);
		gd.addNumericField("Number of histogram bins per column of heatmap", 25, 1);
		gd.addStringField("Title", "Yeast in light", 20);
		gd.addStringField("x-Axis label", "Time", 20);
		gd.addStringField("y-Axis label", "Fluorescence", 20);
		gd.addNumericField("Font scalar", 15, 0);
		gd.addNumericField("x-Axis tick frequency (2 would mean every other X value)", 15, 0);
		gd.addNumericField("Delay between each image set (minutes)",10,5);
		gd.addNumericField("y-Axis tick frequency (1 would mean a tick for every bin)", 8, 0);
		gd.showDialog();
		if (gd.wasCanceled()) {
			gd.dispose();
			return false;
		} else {
			this.histValIndex=gd.getNextChoiceIndex();
			this.histBinIndex=gd.getNextChoiceIndex();
			this.maxHistVal=new Double(Math.log(gd.getNextNumber()));
			double min=gd.getNextNumber();
			if(min<1)
				min=1;
			this.minHistVal=new Double(Math.log(min));
			this.numBins=new Integer((int) gd.getNextNumber());
			this.title=gd.getNextString().replace("\\", "_").replace("/", "_");
			this.xAxis=gd.getNextString();
			this.yAxisExp=gd.getNextString();
			this.yAxis="Ln("+this.yAxisExp+")";
			this.range=this.maxHistVal-this.minHistVal;
			binWidth=range/(double)this.numBins;
			this.scalar=(int) gd.getNextNumber();
			this.tr = new Font("SansSerif", Font.PLAIN, (5*scalar));
			this.trb = new Font("SansSerif", Font.BOLD, (7*scalar));
			this.heatMapPlotPath=this.saveDir+this.title+sdf.format(new Date())+"_heatMap_Plot.png";
			this.xFrequency=(int)gd.getNextNumber();
			this.xSpacing=gd.getNextNumber();
			this.yFrequency=(int)gd.getNextNumber();
			this.heatMapImagePath=this.saveDir+this.title+sdf.format(new Date())+"_heatMap_Image.txt";
		}

		return true;
	}
	@Override
	public void run(String arg0) {
		if(!setup()){
			return;
		};
		IJ.log("\\Clear"); //empties the Log 
		//Each row has all the vals in a set
		this.histVals=getHistVals();
		this.arrayOfHistograms=getArrayOfHists(this.histVals, this.numBins,this.maxHistVal,this.minHistVal);
		printHistArrayInfo(this.arrayOfHistograms,this.maxHistVal,this.minHistVal);
		double[][] normHistArr=normalizeHistogram(this.arrayOfHistograms);
		//This gets the values in an order that matches the image representation
		//of the heatmap
		double[][] heatMapVals=reOrderVals(normHistArr);
		//double[] binCenters=getBinCenters(this.numBins,this.maxHistVal,this.minHistVal);
		double[] times=getDoubleArr(0,this.arrayOfHistograms.length,1);
		double[] percentilesWanted={95,75,50,25,5};
		double[][] percentiles=getPercentile(this.histVals,percentilesWanted);
		double[][] expPercentiles=getExponentialVals(percentiles);
		Plot[] plots=new Plot[percentilesWanted.length];
		for(int i=0;i<percentilesWanted.length;i++){
			plots[i]=new Plot(this.title+" "+Double.toString(percentilesWanted[i])+"th percentile", this.xAxis, this.yAxis, times,percentiles[i]);
			ImagePlus img=plots[i].makeHighResolution(this.title+" "+Double.toString(percentilesWanted[i])+"th percentile", 4, false, false);
			IJ.save(img, this.saveDir+this.title.replace("\\", "_").replace("/", "_")+" "+Double.toString(percentilesWanted[i])+"th percentile.tif");
			plots[i].show();
		}
		
		Plot[] expPlots=new Plot[percentilesWanted.length];
		for(int i=0;i<percentilesWanted.length;i++){
			expPlots[i]=new Plot(this.title+" "+Double.toString(percentilesWanted[i])+"th percentile (exponent)", this.xAxis, this.yAxisExp, times,expPercentiles[i]);
			ImagePlus img=plots[i].makeHighResolution(this.title+" "+Double.toString(percentilesWanted[i])+"th percentile (exponent)", 4, false, false);
			IJ.save(img, this.saveDir+this.title.replace("\\", "_").replace("/", "_")+" "+Double.toString(percentilesWanted[i])+"th percentile (exponent).tif");
			expPlots[i].show();
		}
		IJ.log(toString());;
		IJ.selectWindow("Log");
		IJ.saveAs("Text", this.saveDir+"LOG_"+this.title);
		
		saveResultsAsTSV(heatMapVals,this.heatMapImagePath);
		ImagePlus mapFromTSV = IJ.openImage(this.heatMapImagePath);
		mapFromTSV.show();
		//IJ.run(mapFromTSV, "8-bit", "");
		IJ.run(mapFromTSV, "Fire", "");
		IJ.save(mapFromTSV, this.saveDir+this.title+sdf.format(new Date())+"FireLUT_Image.tif");
		saveHeatMapWithPlot(heatMapVals,this.heatMapPlotPath);
		ImagePlus mapWithAxis = IJ.openImage(this.heatMapPlotPath);
		IJ.run(mapWithAxis, "32-bit", "");
		IJ.run(mapWithAxis, "Divide...", "value=255");
		IJ.setMinAndMax(mapWithAxis, 0.0, this.maxBinSize/255.0);
		IJ.run(mapWithAxis, "Fire", "");
		int originalWidth=mapWithAxis.getWidth();
		int originalHeight=mapWithAxis.getHeight();
		IJ.run(mapWithAxis, "Canvas Size...", "width="+Integer.toString(originalWidth+300)+" height="+Integer.toString(originalHeight)+" position=Center-Left");
		mapWithAxis.setRoi(originalWidth+50,120,1,1);
		IJ.run(mapWithAxis, "Calibration Bar...", "location=[At Selection] fill=White label=Black number=5 decimal=4 font=12 zoom=3 overlay");
		Font font = new Font("SansSerif", Font.PLAIN, 36);
		Roi roi = new TextRoi(originalWidth+50, 0, " Normalized\n Frequency", font);
		roi.setStrokeColor(new Color((float)1.00,(float) 1.00,(float) 1.00));
		mapWithAxis.getOverlay().add(roi);
		mapWithAxis.show();
		//IJ.save(mapWithAxis, this.heatMapPlotPath);
		
		//Plot(this.title, this.xAxis, this.yAxis, double[] xValues, double[] yValues, int flags)
		//plotWithJFreeChart(heatMapVals);
		
	}
	
	
	//Returns percentiles for each data point. For example, if
	//percentiles =[50], then it will return an array containing 1 array. The inner
	//array will contain the 50th percentile data point for set/timepoint.
	public double[][] getPercentile(List<Double[]> listOfArrs,double[] percentiles){
		int listSize=listOfArrs.size();
		double[][] percentileArr=new double[percentiles.length][listSize];
		int i=0;
		for(Double[] arrOfDoubles:listOfArrs){
			double[] arr=ArrayUtils.toPrimitive(arrOfDoubles);
			DescriptiveStatistics histStats=new DescriptiveStatistics(arr);
			for(int j=0;j<percentiles.length;j++){
				percentileArr[j][i]=histStats.getPercentile(percentiles[j]);
			}
			i++;
		}
		
		return percentileArr;
	}
	
	public double[][] getExponentialVals(double[][] vals){
		int rows=vals.length;
		int columns=vals[0].length;
		double[][] expVals=new double[rows][columns];
		for(int i=0;i<rows;i++)
			for(int j=0;j<columns;j++)
				expVals[i][j]=Math.exp(vals[i][j]);
		return expVals;
	}
	
	public double[] getBinCenters(int numBins,double maxVal,double minVal){
		double binWidth=(maxVal-minVal)/(double)numBins;
		double[] lnBinCenters=new double[numBins];
		for(double i=0;i<numBins;i++){
			lnBinCenters[(int)i]=minVal+binWidth* (i+0.5);
		}
		return lnBinCenters;
	}
	
	public double[] getDoubleArr(double startVal,double maxVal,double increment){
		int vals=(int) ((maxVal-startVal)/increment);
		double[] newArr=new double[vals];
		for(double i=0;i<vals;i++){
			newArr[(int)i]=startVal+i*increment;
		}
		return newArr;
	}
	
	//This accepts a array of arrays that represents an array of histogram data
	//It returns a new array of arrays the orders the data as it should be represented
	//in a heatmap image.
	public double[][] reOrderVals(double[][] vals){
		double[][] orderedHistVals=new double[vals[0].length][vals.length];
		int maxHistBin=vals[0].length-1;
		int k=0;
		for(int i=maxHistBin;i>=0;i--){
			int j=0;
			for(double[] hist:vals){
				orderedHistVals[k][j]=hist[i];
				j++;
			}
			k++;
		}
		return orderedHistVals;
	}
	
	
	public void plotWithJFreeChart(double[][] vals){
		DefaultXYZDataset xyz=new DefaultXYZDataset();
		xyz.addSeries(1, vals);
		JFreeChart chart=createChart(xyz);
		ApplicationFrame frame=new ApplicationFrame(this.title);
		JPanel chartPanel=new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270)); 
	    frame.setContentPane(chartPanel); 
	    frame.pack(); 
        RefineryUtilities.centerFrameOnScreen(frame); 
        frame.setVisible(true); 
	}
	
    private static JFreeChart createChart(XYZDataset dataset) { 
        NumberAxis xAxis = new NumberAxis("X"); 
        xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits()); 
        xAxis.setLowerMargin(0.0); 
        xAxis.setUpperMargin(0.0); 
        NumberAxis yAxis = new NumberAxis("Y"); 
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits()); 
        yAxis.setLowerMargin(0.0); 
        yAxis.setUpperMargin(0.0); 
        XYBlockRenderer renderer = new XYBlockRenderer(); 
        PaintScale scale = new GrayPaintScale(0.0, 255.0); 
        renderer.setPaintScale(scale); 
        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer); 
        plot.setBackgroundPaint(Color.lightGray); 
        plot.setDomainGridlinesVisible(false); 
        plot.setRangeGridlinePaint(Color.white); 
        JFreeChart chart = new JFreeChart("XYBlockChartDemo1", plot); 
        chart.removeLegend(); 
        chart.setBackgroundPaint(Color.white); 
        return chart; 
    } 
	
	//This takes in an array of histograms (array of arrays). It normalizes
	//each row in the array of arrays to have a sum of 1, and then it scales it
	//to have a sum of 255. It also records the largest bin occupancy so that
    //the image could be rescaled to better display the range of bin occupancies
	public double[][] normalizeHistogram(double [][] vals){
		int rows=vals.length;
		int columns=vals[0].length;
		this.maxBinSize=0.0;
		double[][] normHist=new double[rows][columns];
		for(int i=0;i<rows;i++){
			double[] row=vals[i];
			double sum=0;
			for(double val:row)
				sum=sum+val;
			IJ.log("sum is: "+Double.toString(sum));
			for(int j=0;j<columns;j++){
				IJ.log("The fraction is: "+Double.toString(row[j]/sum));
				normHist[i][j]=255.0*row[j]/sum;
				if(row[j]/sum>this.maxBinSize)
					this.maxBinSize=row[j]/sum;
			}
		}
		this.maxBinSize=this.maxBinSize*255.0;
		return normHist;
	}
	
	//This multiplies the input values by the input scalar*255 (255 is used for the
	//0-255 intensity levels in a PNG image), then savesthem as TSV
	public void saveResultsAsTSV(double[][] vals, String filePath){
		try{
			FileWriter writer = new FileWriter(filePath, true);
			for(int i=0;i<vals.length;i++){
				double[] hist=vals[i];
				if(i!=0)
					writer.write("\n\r");
				for(int j=0;j<hist.length;j++){
					if(j!=0)
						writer.write("\t");
					writer.write(Double.toString(hist[j]));		
				}
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void saveHeatMapWithPlot(double[][] vals,String filePath){
		HeatChart map = new HeatChart(vals);
		// Step 2: Customize the chart.
		map.setTitle(this.title);
		map.setXAxisLabel(this.xAxis);
		map.setYAxisLabel(this.yAxis);
		Color low=new Color(0,0,0);
		Color high=new Color((float)(this.maxBinSize/255.0),(float)(this.maxBinSize/255.0),(float)(this.maxBinSize/255.0));
		map.setLowValueColour(low);
		map.setHighValueColour(high);
		map.setXAxisValuesFrequency(this.xFrequency);
		map.setYAxisValuesFrequency(this.yFrequency);
		Double[] yVals=new Double[this.lnBinEdges.length-1];
		int j=lnBinEdges.length-1;
		for(int i=0;i<yVals.length;i++){
			double midBinVal=(lnBinEdges[j-1]+lnBinEdges[j])/2;
			//Now, round the values to 4 sig figs
			int truncator=(int) (midBinVal*10000)+5;
			double truncatedResult=((double)truncator)/10000.0;
			yVals[i]=new Double(truncatedResult);
			j--;
		}
		Double[] xVals=new Double[arrayOfHistograms.length];
		for(double i=0;i<xVals.length;i++)
			xVals[(int)i]=new Double(i*this.xSpacing);
		map.setYValues(yVals);
		map.setXValues(xVals);
		map.setAxisLabelsFont(this.tr);
		map.setTitleFont(this.trb);
		map.setAxisValuesFont(this.tr);
	
		// Step 3: Output the chart to a file.
		try {
			File file=new File(filePath);
			map.saveToFile(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//This function returns a list of double arrays. 
	//Each item in the list corresponds to a set. Each array of Doubles corresponds to the data
	//to be histogramed in that set
	public List<Double[]> getHistVals(){
		List<LinkedList<Double>> listOfVals=new LinkedList<LinkedList<Double>>();
		LinkedList<Double> singleVals=new LinkedList<Double>();
		int setCounter=0;
		//Iterator<LinkedList<Double>> listOfValsIterator=listOfVals.iterator();
		boolean firstRow=true;
		for(String[] row:this.allVals){
			if(firstRow){ //if headers
				firstRow=false;
			}else{  //headers headers. Values that can be converted to Double
				Double histVal=Double.parseDouble(row[this.histValIndex]);
				histVal=new Double(Math.log(histVal));
				Double setVal=Double.parseDouble(row[this.histBinIndex]);
				while(setVal>setCounter){
					setCounter++;
					listOfVals.add(singleVals);
					singleVals=new LinkedList<Double>();
				}
				singleVals.add(histVal);
			}
		}
		List<Double[]> listOfValArrs=new LinkedList<Double[]>();
		for(LinkedList<Double> row:listOfVals){
			listOfValArrs.add(row.toArray(new Double[0]));
		}
		return listOfValArrs;
	}
	
	//This returns an array of arrays of doubles. The first array has a list entry for each data set
	//The second array has a histogram for each data set
	/*
	 * [[histogram for set 1],
	 * [histogram for set 2],
	 * ...]
	 */
	public double[][] getArrayOfHists(List<Double[]> listOfDoubles, int bins, double max, double min){		
		double[][] arrayOfHists=new double[listOfDoubles.size()][bins];
		int counter=0;
		for(Double[] setVals:listOfDoubles){
			for(Double val:setVals){
				if(val<min)
					val=min;
				if(val>=max)
					val=max-0.1;
				int binVal=(int) ((val-min)/binWidth);
				arrayOfHists[counter][binVal]++;
			}
			counter++;
		}
		return arrayOfHists;
	}
	
	//THis assumes that the input ,matrix has at least one array
	//This also returns the edges
	public void printHistArrayInfo(double[][] arrOfHists,double maxVal,double minVal){
		int numBins=arrOfHists[0].length;
		double binWidth=(maxVal-minVal)/(double)numBins;
		int numHists=arrOfHists.length;
		int totalCount=0;
		int[] countPerSet=new int[numHists];
		int[] countPerBin=new int[numBins];
		lnBinEdges=new double[numBins+1];
		binEdges=new double[numBins+1];
		for(int i=0;i<numHists;i++)
			for(int j=0;j<numBins;j++){
				double val=arrOfHists[i][j];
				totalCount+=val;
				countPerSet[i]+=val;
				countPerBin[j]+=val;
			}
		for(double i=0;i<=numBins;i++){
			lnBinEdges[(int)i]=minVal+binWidth* i;
			binEdges[(int)i]=Math.exp(minVal+binWidth* i);
		}
		
		IJ.log("In total, there were "+Integer.toString(totalCount)+" data points across "+Integer.toString(numHists)+" data sets (aka timepoints)");
		IJ.log("The bin edges were: \n\r"+Arrays.toString(lnBinEdges));
		IJ.log("The exponential value of the bin edges were: \n\r"+Arrays.toString(binEdges));
		IJ.log("The count inside each bin was: \n\r"+Arrays.toString(countPerBin));
		IJ.log("The count for each data set was: \n\r"+Arrays.toString(countPerSet));
	
	}
	
	public String toString(){
		String response="\n\r\n\rCSV file of source data: \n\r   "+this.csvPath+
				"\n\r\n\rDirectory to save results in: \n\r"+this.saveDir+
				"\n\r\n\rValue to histogram (ie Max Fluorescence: \n\r"+this.allVals.get(0)[this.histValIndex]+
				"\n\r\n\rValue to form histogram bins (ie Set): \n\r"+this.allVals.get(0)[this.histBinIndex]+
				"\n\r\n\rMaximum histogram value to bin: \n\r"+this.maxHistVal+
				"\n\r\n\rMinimum histogram value to bin: \n\r"+this.minHistVal+
				"\n\r\n\rNumber of bins per column of heatmap: \n\r"+this.numBins+
				"\n\r\n\rTitle: \n\r"+this.title+
				"\n\r\n\rX-axis title: \n\r"+this.xAxis+
				"\n\r\n\rY-axis title: \n\r"+this.yAxis+
				"\n\r\n\rFont scalar: \n\r"+this.scalar+
				"\n\r\n\rX-Axis frequency: \n\r"+this.xFrequency+
				"\n\r\n\rY-axis frequency: \n\r"+this.yFrequency+
				"\n\r\n\rHeat map image path: \n\r"+this.heatMapImagePath+
				"\n\r\n\r Heat map plot path: \n\r"+this.heatMapPlotPath+
				"\n\r\n\r*******IMPORTANT*******\n\r"
				+ "A bin containing "+Double.toString(this.maxBinSize*100.0/255.0)+"% of the data (the most in this data set) will be displayed\n\r"
						+ " as having an intensity of "+Double.toString(this.maxBinSize)+" on an 8-bit image. A bin containing no data\n\r"
						+ "is always displayed as having an intensity of 0. The gradient between the min and max \n\r"
						+ "is linear."
						+ "\n\r*****************************\n\r";
		return response;
	}

}
