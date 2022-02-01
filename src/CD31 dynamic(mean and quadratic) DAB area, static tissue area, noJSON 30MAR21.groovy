import qupath.lib.scripting.QP
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import qupath.opencv.ml.pixel.OpenCVPixelClassifier

/* Dynamic Detection of DAB Area and Static Tissue Area for Human Placenta Stained with CD31
Qupath script to dynamically adjust the DAB threshold to correct for batch variation in staining intensity
Developed in Qupath 0.2.3
Author alan.kerby@manchester.ac.uk

The "mean" protocol can serve as a start point to develop a "meanWithFunction" protocol
The "meanWithFunction" has been optimised for CD31 (DAKO M0823) on human placenta. Please undertake thorough validation to use under other conditions.
The "baselineThreshold" could be adjusted to develop a novel protocol

Create annotations, select protocol, and run
Please set one protocol option to "true" and the other option to "false" */
boolean mean = false
boolean meanWithFunction = true
def baselineThreshold = 0.3 //default 0.3

// check setting and annotations
if (mean == meanWithFunction) {
    print "Please set one option to true and the other option to false"
    } else if (QP.annotationObjects.size() == 0) {
        print QP.getProjectEntry().getImageName() + " has no annotations"
        } else {
    if (QP.detectionObjects.size() > 0) {
        QP.clearDetections()
        QP.fireHierarchyUpdate()
        QP.removeObjects(QP.getAnnotationObjects().findAll { it.getLevel() > 1 }, true)
    }
    QP.selectAnnotations()
    QP.mergeSelectedAnnotations()
    def myAnnotation = QP.getAnnotationObjects()
    myAnnotation[0].setPathClass(QP.getPathClass("My Annotations"))

// calculate dynamic threshold
    QP.runPlugin('qupath.imagej.detect.cells.PositiveCellDetection', '{"detectionImageBrightfield": "Optical density sum",  "requestedPixelSizeMicrons": 0.5,  "backgroundRadiusMicrons": 8.0,  "medianRadiusMicrons": 0.0,  "sigmaMicrons": 1.5,  "minAreaMicrons": 10.0,  "maxAreaMicrons": 400.0,  "threshold": 0.1,  "maxBackground": 2.0,  "watershedPostProcess": true,  "excludeDAB": false,  "cellExpansionMicrons": 5.0,  "includeNuclei": true,  "smoothBoundaries": true,  "makeMeasurements": true,  "thresholdCompartment": "Nucleus: DAB OD mean",  "thresholdPositive1": ' + baselineThreshold + ',  "thresholdPositive2": 0.4,  "thresholdPositive3": 0.6,  "singleThreshold": true}')
    def cells = QP.getCellObjects().findAll { cell -> cell.getPathClass() == QP.getPathClass("Positive") }
    def mean_dab_list = cells.collect { cell -> QP.measurement(cell, 'Cell: DAB OD mean') } as double[]
    def mean_dab_stats = new DescriptiveStatistics(mean_dab_list)
    def mean_dab = mean_dab_stats.getMean()
    def func_dab = (-0.7132) + (4.432 * mean_dab) + (-3.633 * (mean_dab * mean_dab))
    if (mean) {
        dyn = mean_dab
    } else {
        dyn = func_dab
    }
    QP.clearDetections()

//create tissue area annotations
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
    def Tissue_Area = GsonTools.getInstance().fromJson(tissue_area, OpenCVPixelClassifier)
    createAnnotationsFromPixelClassifier(Tissue_Area, 100, 100)

//create dab area annotation
    def dab_area = """
{
  "pixel_classifier_type": "OpenCVPixelClassifier",
  "metadata": {
    "inputPadding": 0,
    "inputResolution": {
      "pixelWidth": {
        "value": 0.971189591078068,
        "unit": "µm"
      },
      "pixelHeight": {
        "value": 0.971189591078068,
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
      "0": {},
      "1": {
        "name": "DAB Area",
        "colorRGB": -65281
      }
    }
  },
  "op": {
    "type": "data.op.channels",
    "colorTransforms": [
      {
        "stains": {
          "name": "H-DAB default",
          "stain1": {
            "r": 0.6511078297640718,
            "g": 0.7011930397459234,
            "b": 0.2904942598947397,
            "name": "Hematoxylin",
            "isResidual": false
          },
          "stain2": {
            "r": 0.2691668699565374,
            "g": 0.5682411699082456,
            "b": 0.7775931898744414,
            "name": "DAB",
            "isResidual": false
          },
          "stain3": {
            "r": 0.6330435352304447,
            "g": -0.7128599063057326,
            "b": 0.3018056269931407,
            "name": "Residual",
            "isResidual": true
          },
          "maxRed": 255.0,
          "maxGreen": 255.0,
          "maxBlue": 255.0
        },
        "stainNumber": 2
      }
    ],
    "op": {
      "type": "op.constant",
      "thresholds": [
       $dyn
       ]
    }
  }
}
"""
    def Dab_Area = GsonTools.getInstance().fromJson(dab_area, OpenCVPixelClassifier)
    QP.createAnnotationsFromPixelClassifier(Dab_Area, 0.0, 0.0)

// add info
    print "mean dab " + mean_dab
    print "quad dab " + func_dab
    print 'dynDAB ' + dyn
    QP.getProjectEntry().putMetadataValue("dynDAB", dyn as String)
    QP.getAllObjects().each {
        it.getMeasurementList().putMeasurement("dynDAB", dyn as double)
    }
}
