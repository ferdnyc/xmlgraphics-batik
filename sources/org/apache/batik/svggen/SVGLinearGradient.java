/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.svggen;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.batik.ext.awt.g2d.GraphicContext;

/**
 * Utility class that converts a Java GradientPaint into an
 * SVG linear gradient element
 *
 * @author <a href="mailto:vincent.hardy@eng.sun.com">Vincent Hardy</a>
 * @version $Id$
 */
public class SVGLinearGradient extends AbstractSVGConverter {
    /**
     * @param generatorContext used to build Elements
     */
    public SVGLinearGradient(SVGGeneratorContext generatorContext) {
        super(generatorContext);
    }

    /**
     * Converts part or all of the input GraphicContext into
     * a set of attribute/value pairs and related definitions
     *
     * @param gc GraphicContext to be converted
     * @return descriptor of the attributes required to represent
     *         some or all of the GraphicContext state, along
     *         with the related definitions
     * @see org.apache.batik.svggen.SVGDescriptor
     */
    public SVGDescriptor toSVG(GraphicContext gc) {
        Paint paint = gc.getPaint();
        return toSVG((GradientPaint)paint);
    }

    /**
     * @param gradient the GradientPaint to be converted
     * @return a description of the SVG paint and opacity corresponding
     *         to the gradient Paint. The definiton of the
     *         linearGradient is put in the linearGradientDefsMap
     */
    public SVGPaintDescriptor toSVG(GradientPaint gradient) {
        // Reuse definition if gradient has already been converted
        SVGPaintDescriptor gradientDesc =
            (SVGPaintDescriptor)descMap.get(gradient);

        Document domFactory = generatorContext.domFactory;

        if (gradientDesc == null) {
            Element gradientDef =
                domFactory.createElementNS(SVG_NAMESPACE_URI,
                                           SVG_LINEAR_GRADIENT_TAG);
            gradientDef.setAttributeNS(null, SVG_GRADIENT_UNITS_ATTRIBUTE,
                                       SVG_USER_SPACE_ON_USE_VALUE);

            //
            // Process gradient vector
            //
            Point2D p1 = gradient.getPoint1();
            Point2D p2 = gradient.getPoint2();
            gradientDef.setAttributeNS(null, SVG_X1_ATTRIBUTE,
                                       "" + doubleString(p1.getX()));
            gradientDef.setAttributeNS(null, SVG_Y1_ATTRIBUTE,
                                       "" + doubleString(p1.getY()));
            gradientDef.setAttributeNS(null, SVG_X2_ATTRIBUTE,
                                       "" + doubleString(p2.getX()));
            gradientDef.setAttributeNS(null, SVG_Y2_ATTRIBUTE,
                                       "" + doubleString(p2.getY()));

            //
            // Spread method
            //
            String spreadMethod = VALUE_PAD;
            if(gradient.isCyclic())
                spreadMethod = VALUE_REFLECT;
            gradientDef.setAttributeNS
                (null, SVG_SPREAD_METHOD_ATTRIBUTE, spreadMethod);

            //
            // First gradient stop
            //
            Element gradientStop =
                domFactory.createElementNS(SVG_NAMESPACE_URI, SVG_STOP_TAG);
            gradientStop.setAttributeNS(null, SVG_OFFSET_ATTRIBUTE,
                                      VALUE_ZERO_PERCENT);

            SVGPaintDescriptor colorDesc = SVGColor.toSVG(gradient.getColor1());
            gradientStop.setAttributeNS(null, SVG_STOP_COLOR_ATTRIBUTE,
                                      colorDesc.getPaintValue());
            gradientStop.setAttributeNS(null, SVG_STOP_OPACITY_ATTRIBUTE,
                                      colorDesc.getOpacityValue());

            gradientDef.appendChild(gradientStop);

            //
            // Second gradient stop
            //
            gradientStop =
                domFactory.createElementNS(SVG_NAMESPACE_URI, SVG_STOP_TAG);
            gradientStop.setAttributeNS(null, SVG_OFFSET_ATTRIBUTE,
                                      VALUE_HUNDRED_PERCENT);

            colorDesc = SVGColor.toSVG(gradient.getColor2());
            gradientStop.setAttributeNS(null, SVG_STOP_COLOR_ATTRIBUTE,
                                        colorDesc.getPaintValue());
            gradientStop.setAttributeNS(null, SVG_STOP_OPACITY_ATTRIBUTE,
                                        colorDesc.getOpacityValue());

            gradientDef.appendChild(gradientStop);

            //
            // Gradient ID
            //
            gradientDef.
                setAttributeNS(null, ATTR_ID,
                               generatorContext.idGenerator.
                               generateID(ID_PREFIX_LINEAR_GRADIENT));

            //
            // Build Paint descriptor
            //
            StringBuffer paintAttrBuf = new StringBuffer(URL_PREFIX);
            paintAttrBuf.append(SIGN_POUND);
            paintAttrBuf.append(gradientDef.getAttributeNS(null, ATTR_ID));
            paintAttrBuf.append(URL_SUFFIX);

            gradientDesc = new SVGPaintDescriptor(paintAttrBuf.toString(),
                                                  VALUE_OPAQUE,
                                                  gradientDef);

            //
            // Update maps so that gradient can be reused if needed
            //
            descMap.put(gradient, gradientDesc);
            defSet.add(gradientDef);
        }

        return gradientDesc;
    }
}
