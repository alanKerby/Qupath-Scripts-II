/* Dynamic Detection of Syncytial Nuclear Aggregates (SNA) and Static Tissue Area for Human Placenta Stained with H&E
Qupath script to dynamiclly adjust the hematoxylin threshold to correct for batch variation in staining intensity
Developed in Qupath 0.2.3
Author alan.kerby@manchester.ac.uk

The "mean" protocol can serve as a start point to develop a novel "meanWithFunction" protocol
The "meanWithFunction" has been optimised for H&E on human placenta. Please undertake thorough validation to use under other conditions.
The "baselineThreshold", "hematoxylinIntenstyCutoff", "snaIntensityCutoff", and  "circularityCutoff" could be adjusted to develop a novel protocol

Create annotations, set protocol, and run
Please set one protocol option to "true" and the other option to "false" */
boolean mean = false;
boolean meanWithFunction = true;

def baselineThreshold = 0.03 //default 0.03
def hematoxylinIntenstyCutoff = 0.07 //default 0.07
def snaIntensityCutoff = 0.03 //default 0.03
def circularityCutoff = 0.4 //default 0.4

import qupath.lib.scripting.QP
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics

// check setting and annotations
if (mean == meanWithFunction) {
    print "Please set one option to true and the other option to false"
} else if (QP.annotationObjects.size() == 0) {
    print QP.getProjectEntry().getImageName() + " has no annotations"
} else {
    if (QP.detectionObjects.size() > 0) {
        QP.clearDetections()
        QP.fireHierarchyUpdate();
        QP.removeObjects(QP.getAnnotationObjects().findAll { it.getLevel() > 1 }, true)
    }
    QP.selectAnnotations();
    QP.mergeSelectedAnnotations();
    def MyAnno = QP.getAnnotationObjects()
    MyAnno[0].setPathClass(QP.getPathClass("My Annotations"))

// calculate dynamic threshold
    QP.runPlugin('qupath.imagej.detect.cells.WatershedCellDetection', '{"detectionImageBrightfield": "Hematoxylin OD",  "requestedPixelSizeMicrons": 0.5,  "backgroundRadiusMicrons": 8.0,  "medianRadiusMicrons": 0.0,  "sigmaMicrons": 1.5,  "minAreaMicrons": 10.0,  "maxAreaMicrons": 400.0,  "threshold": ' + baselineThreshold + ',  "maxBackground": 2.0,  "watershedPostProcess": true,  "cellExpansionMicrons": 5.0,  "includeNuclei": true,  "smoothBoundaries": true,  "makeMeasurements": true}');
    def cells = QP.getCellObjects().findAll { QP.measurement(it, "Nucleus: Hematoxylin OD mean") > hematoxylinIntenstyCutoff }
    def mean_cell_list = cells.collect { cell -> QP.measurement(cell, 'Nucleus: Hematoxylin OD mean') } as double[]
    def mean_stats = new DescriptiveStatistics(mean_cell_list)
    def mean_dab = mean_stats.getMean()
    def func_dab = (1.248 * mean_dab) - 0.04072
    if (mean) {
        dyn = mean_dab
    } else {
        dyn = func_dab
    }
    QP.clearDetections();

// create SNA detections
    QP.runPlugin('qupath.imagej.detect.cells.WatershedCellDetection', '{"detectionImageBrightfield": "Hematoxylin OD",  "requestedPixelSizeMicrons": 0.5,  "backgroundRadiusMicrons": 8.0,  "medianRadiusMicrons": 0.0,  "sigmaMicrons": 4.0,  "minAreaMicrons": 125.0,  "maxAreaMicrons": 0.0,  "threshold": '+ dyn +',  "maxBackground": 2.0,  "watershedPostProcess": true,  "cellExpansionMicrons": 5.0,  "includeNuclei": true,  "smoothBoundaries": true,  "makeMeasurements": true}');
    QP.resetDetectionClassifications()
    def preSNA = QP.getCellObjects()
    def SNA = preSNA.findAll { QP.measurement(it, "Nucleus: Hematoxylin OD mean") > snaIntensityCutoff && QP.measurement(it, "Nucleus: Circularity") > circularityCutoff }
    SNA.each { it.setPathClass(QP.getPathClass('SNA positive')) }
    QP.fireHierarchyUpdate()

//create tissue area annotation
    def tissue_area = """
{
  "pixel_classifier_type": "OpenCVPixelClassifier",
  "metadata": {
    "inputPadding": 0,
    "inputResolution": {
      "pixelWidth": {
        "value": 1.942379182156136,
        "unit": "µm"
      },
      "pixelHeight": {
        "value": 1.942379182156136,
        "unit": "µm"
      },
      "zSpacing": {
        "value": 1.0,
        "unit": "z-slice"
      },
      "timeUnit": "SECONDS",
      "timepoints": []
    },
    "inputWidth": 512,
    "inputHeight": 512,
    "inputNumChannels": 3,
    "outputType": "CLASSIFICATION",
    "outputChannels": [],
    "classificationLabels": {
      "0": {
        "name": "Tissue Area",
        "colorRGB": -16711936
      },
      "1": {
        "name": "Ignore*",
        "colorRGB": -3355444
      }
    }
  },
  "op": {
    "type": "data.op.channels",
    "colorTransforms": [
      {
        "combineType": "MEAN"
      }
    ],
    "op": {
      "type": "op.constant",
      "thresholds": [
        230.0
      ]
    }
  }
}
"""
    def Tissue_Area = GsonTools.getInstance().fromJson(tissue_area, qupath.opencv.ml.pixel.OpenCVPixelClassifier)
    QP.createAnnotationsFromPixelClassifier(Tissue_Area, 100, 100)

// add info
    print "mean dab " + mean_dab
    print "quad dab " + func_dab
    print 'dynDAB ' + dyn
    QP.getProjectEntry().putMetadataValue("dynDAB", dyn as String)
    QP.getAllObjects().each {
        it.getMeasurementList().putMeasurement("dynT", dyn)
    }
}