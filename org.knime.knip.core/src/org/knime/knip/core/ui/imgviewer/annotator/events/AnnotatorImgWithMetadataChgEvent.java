package org.knime.knip.core.ui.imgviewer.annotator.events;

import net.imagej.ImgPlusMetadata;
import net.imglib2.img.Img;
import net.imglib2.type.Type;

import org.knime.knip.core.ui.imgviewer.annotator.RowColKey;
import org.knime.knip.core.ui.imgviewer.events.ImgWithMetadataChgEvent;

public class AnnotatorImgWithMetadataChgEvent<T extends Type<T>> extends ImgWithMetadataChgEvent<T> {

    private final RowColKey m_key;

    public AnnotatorImgWithMetadataChgEvent(final Img<T> img,
                                            final ImgPlusMetadata imageMetaData, final RowColKey key) {
        super(img, imageMetaData);
        m_key = key;
    }

    public RowColKey getKey() {
        return m_key;
    }

}