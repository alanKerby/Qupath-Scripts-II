## Qupath Scripts for Dynamic Image Analysis of CD31 and Syncytial Nuclear Aggregates (SNA) in Human Placenta

### Description
Histological examination of the placenta significantly contributes to diagnosis in adverse birth outcomes. One challenge in image analysis is variation in staining intensity caused by batch variation. We investigated if dynamic threshold image analysis methods may increase accuracy. Placenta samples were stained for endothelial cells and syncytial nuclear aggregates and analysed in Qupath software. Dynamically setting the threshold resulted in data more similar to manual method data. The method is simple and effective at modelling the dynamic interpretation of variation in staining intensity achieved by manual methods. We anticipate dynamic methods could be used to enhance placental diagnosis.

### CD31
The CD31 script processes a positive cell detection with a low baseline DAB threshold (0.3), then the mean of the mean DAB intensity for each DAB positive cell is calculated and used as the DAB threshold for an area detection “pixel classifier”. A quadratic function (-3.633x2 + 4.432 x – 0.7132) can be applied to the calculated threshold. An area detection pixel classifier with an average channel (RGB) static threshold detects tissue area. CD31 detection is calculated as a percentage of the tissue area. 

### SNA
The script processes a cell detection with a low baseline haematoxylin threshold (0.03), then for all nuclei above a specific mean haematoxylin intensity (0.07) the mean of the mean haematoxylin intensity for each nuclei is calculated and used as the threshold for the SNA detection. A function (1.248x – 0.04072) can be applied to the calculated threshold. An area detection pixel classifier with an average channel (RGB) static threshold detects tissue area. SNA detection was calculated as SNA per mm2 of tissue area.

### Useage
The scripts were developed in Qupath 0.2.3. Slides were imaged using Panoramic 250 slide scanner (3D Histech). Placenta samples were from live birth and stillbirth cases and were stained with CD31 antibody (DAKO M0823)/hematoxylin or hematoylin/eosin. Under these conditions the CD31 script with the quadratic function ("meanWithFunction") and the SNA script without a function ("mean") were optimal. Simply create annotations (ROI) and run the script and read/export the data. Further guidance on how to do this can be found in the [Qupath Docs](https://qupath.readthedocs.io/en/stable/index.html). 

### Development
Testing is required to use the scripts under other conditions. The scripts without a function ("mean") are a good starting point from which a function can be added as required. A detailed methodology for developing dynamic scripts will be made available, and is currently under review at [Placenta](https://www.journals.elsevier.com/placenta).

### Authors and Acknowledgment
Developed by Dr Alan Kerby under the supervision of Professor Alexander Heazell at the Maternal and Fetal Research Centre, University of Manchester, UK.

### License
[MIT](https://choosealicense.com/licenses/mit/)
