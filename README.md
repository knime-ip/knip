KNIME Image Processing Extension
====

The KNIME Image Processing Extension adds new nodes to KNIME (www.knime.org) to e.g. read more than 100 different kinds of images (thanks to the Bio-Formats API), apply well known methods for the preprocessing and to perform image segmentation and classification. Most of the included nodes operate on multi-dimensional image data (e.g. videos, 3D images, multi-channel images or even a combination of them), which is made possible by the internally used ImgLib2-API. In addition several nodes are included to calculate image features (e.g. zernike-, texture- or histogram features) for segmented images (e.g. a single cell). These feature vectors can then be used to apply machine learning methods in order to train and apply a classifier. 

The KNIME Image Processing Extension currently provides about 90 nodes for (pre)-processing, filtering, segmentation, feature extraction, various views, ....

Installation and Development
====
You have to use the KNIME Development Kit to develop plugins for KNIME Image Processing

1. Download current version of KNIME Development Kit (http://www.knime.org/downloads/overview)
2. Clone Repository using your favourite Git-Client
3. In File -> Import -> Existing Projects Into Workspace
4. Follow steps from http://tech.knime.org/developers
