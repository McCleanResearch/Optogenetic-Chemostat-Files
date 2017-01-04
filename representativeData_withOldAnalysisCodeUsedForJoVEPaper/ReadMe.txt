Cameron Stewart
March 9th, 2016
Research Intern at UW-Madison, Department of Biomedical Engineering
PI: Megan McClean

This zip file contains 100 "Results.csv" files. Each of these contain the measurements of yeast that were aquired in the same time period
(the last image was acquired within roughly a minute of the first). Each row of the file corresponds to a single ROI, and the columns correspond to:
The area (in pixels) of the ROI, mean intensity, the standard deviation of the pixel intensities, Minimum intensity, Maximum intensity, Median intensity

For measuring the area, note that 9.316 pixels corresponds to 1 micrometer.

The "times.csv" contains a single column of time in minutes since the start of the experiment. Row 1 corresponds to the time that "Results1.csv" was acquired,
Row 2 corresponds to the time that "Results2.csv" was acquired, and so on.

The "intensity.csv" file contains 2 columns. The first contains the time in minutes and the second contains the intensity of light absorbed or diffracted by yeast in the
culturing vessel in microWatts per centimeter squared. This was calculated as the difference in intensity measured by a photodiode at the lid of the culturing vessel filled
with sterile media and the intensity measured with yeast (collected within 45 minutes of the end of the experiment, when media was no longer flowing into the culturing vessel).

The Python script may be useful for analyzing the data. The script will work if the value of "rootDir" is filepath of this extracted folder. If you
need a Python IDE, I recommend Enthought Canopy Express (their free version).