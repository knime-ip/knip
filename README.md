KNIME Image Processing Extension (KNIP)
====

These image processing nodes add new image types to KNIME (www.knime.org) and the corresponding nodes to read more than 100 different kinds of images (thanks to the Bio-Formats API), to apply well known methods for the preprocessing and to perform image segmentation. It replaces the outdated Image Processing plugin that was previously hosted on KNIME Labs. Most of the included nodes operate on multi-dimensional image data (e.g. videos, 3D images, multi-channel images or even a combination of them), which is made possible by the internally used ImgLib2-API. In addition several nodes are included to calculate image features (e.g. zernike-, texture- or histogram features) for segmented images (e.g. a single cell). These feature vectors can then be used to apply machine learning methods in order to train and apply a classifier. 

The KNIME Image Processing Extension currently provides about 90 nodes for (pre)-processing, filtering, segmentation, feature extraction, various views, ....
