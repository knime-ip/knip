
[![Join the chat at https://gitter.im/knime-ip/knip](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/knime-ip/knip?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


KNIME Image Processing Extension
====

[![Join the chat at https://gitter.im/knime-ip/knip](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/knime-ip/knip?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

The KNIME Image Processing Extension adds new nodes to KNIME (www.knime.org) to e.g. read more than 100 different kinds of images (thanks to the Bio-Formats API), apply well known methods for preprocessing, image segmentation and classification. Most of the included nodes operate on multi-dimensional image data (e.g. videos, 3D images, multi-channel images or even a combination of them), which is made possible by the ImgLib2-API. In addition several nodes are included to calculate image features (e.g. zernike-, texture- or histogram features) for segmented images (e.g. a single cell). These feature vectors can then be used to apply machine learning methods in order to train and apply a classifier. 

The KNIME Image Processing Extension currently provides about 90 nodes for (pre)-processing, filtering, segmentation, feature extraction, various views, ....

For more information how to install/use KNIME Image Processing see:
http://tech.knime.org/community/image-processing

Example Workflows can be found at:
http://tech.knime.org/community/image-processing#exampleworkflows

KNIME Image Processing Webinar on YouTube (03/25/2014):
http://www.youtube.com/watch?v=MqSIyqmm3EU&feature=youtu.be&a 

Package Organization
====

* org.knime.knip.core: Logic/Algorithms/DataStructures/Views independent of KNIME
* org.knime.knip.base: KNIME Image Processing Nodes wrapping core and providing dedicated KNIME Image Processing functionality (NodeModels etc).
* org.knime.knip.feature: Eclipse feature for org.knime.knip.core and org.knime.knip.base
* org.knime.knip.testing: KNIME Image Processing Testing nodes, e.g. for regressions tests.
* org.knime.knip.testing.feature: Eclipse feature for KNIME Image Processing Testings nodes.
* org.knime.knip.io: Image Reader/Image Writer for KNIME Image Processing. 
* org.knime.knip.update: Eclipse update site for KNIME Image Processing and depedencies.
* org.knime.knip.tracking: TrackMate Tracking integration.

For Developers:
====
See [this repository](https://github.com/knime-ip/knip-sdk-setup) for instructions.
