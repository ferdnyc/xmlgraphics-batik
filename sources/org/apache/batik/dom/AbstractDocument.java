/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.dom;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.lang.reflect.Method;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.WeakHashMap;

import org.apache.batik.dom.events.DocumentEventSupport;

import org.apache.batik.dom.traversal.TraversalSupport;

import org.apache.batik.i18n.Localizable;
import org.apache.batik.i18n.LocalizableSupport;

import org.apache.batik.util.SoftDoublyIndexedTable;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.w3c.dom.events.DocumentEvent;
import org.w3c.dom.events.Event;

import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;

/**
 * This class implements the {@link org.w3c.dom.Document} interface.
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 * @version $Id$
 */
public abstract class AbstractDocument
    extends    AbstractParentNode
    implements Document,
               DocumentEvent,
               DocumentTraversal,
               Localizable {

    /**
     * The error messages bundle class name.
     */
    protected final static String RESOURCES =
        "org.apache.batik.dom.resources.Messages";

    /**
     * The localizable support for the error messages.
     */
    protected transient LocalizableSupport localizableSupport =
        new LocalizableSupport(RESOURCES);

    /**
     * The DOM implementation.
     */
    protected transient DOMImplementation implementation;

    /**
     * The traversal support.
     */
    protected transient TraversalSupport traversalSupport;

    /**
     * The DocumentEventSupport.
     */
    protected transient DocumentEventSupport documentEventSupport;

    /**
     * Whether the event dispatching must be done.
     */
    protected transient boolean eventsEnabled;

    /**
     * The ElementsByTagName lists.
     */
    protected transient WeakHashMap elementsByTagNames;

    /**
     * Creates a new document.
     */
    protected AbstractDocument() {
    }

    /**
     * Creates a new document.
     */
    protected AbstractDocument(DOMImplementation impl) {
	implementation = impl;
    }

    /**
     * Implements {@link org.apache.batik.i18n.Localizable#setLocale(Locale)}.
     */
    public void setLocale(Locale l) {
	localizableSupport.setLocale(l);
    }

    /**
     * Implements {@link org.apache.batik.i18n.Localizable#getLocale()}.
     */
    public Locale getLocale() {
        return localizableSupport.getLocale();
    }

    /**
     * Implements {@link
     * org.apache.batik.i18n.Localizable#formatMessage(String,Object[])}.
     */
    public String formatMessage(String key, Object[] args)
        throws MissingResourceException {
        return localizableSupport.formatMessage(key, args);
    }

    /**
     * Tests whether the event dispatching must be done.
     */
    public boolean getEventsEnabled() {
	return eventsEnabled;
    }

    /**
     * Sets the eventsEnabled property.
     */
    public void setEventsEnabled(boolean b) {
	eventsEnabled = b;
    }

    /**
     * <b>DOM</b>: Implements {@link org.w3c.dom.Node#getNodeName()}.
     * @return "#document".
     */
    public String getNodeName() {
	return "#document";
    }

    /**
     * <b>DOM</b>: Implements {@link org.w3c.dom.Node#getNodeType()}.
     * @return {@link org.w3c.dom.Node#DOCUMENT_NODE}
     */
    public short getNodeType() {
	return DOCUMENT_NODE;
    }

    /**
     * <b>DOM</b>: Implements {@link org.w3c.dom.Document#getDoctype()}.
     */
    public DocumentType getDoctype() {
	for (Node n = getFirstChild(); n != null; n = n.getNextSibling()) {
	    if (n.getNodeType() == DOCUMENT_TYPE_NODE) {
		return (DocumentType)n;
	    }
	}
	return null;
    }

    /**
     * Sets the document type node.
     */
    public void setDoctype(DocumentType dt) {
	if (dt != null) {
	    appendChild(dt);
	    ((ExtendedNode)dt).setReadonly(true);
	}
    }

    /**
     * <b>DOM</b>: Implements {@link org.w3c.dom.Document#getImplementation()}.
     * @return {@link #implementation}
     */
    public DOMImplementation getImplementation() {
	return implementation;
    }

    /**
     * <b>DOM</b>: Implements {@link
     * org.w3c.dom.Document#getDocumentElement()}.
     */
    public Element getDocumentElement() {
	for (Node n = getFirstChild(); n != null; n = n.getNextSibling()) {
	    if (n.getNodeType() == ELEMENT_NODE) {
		return (Element)n;
	    }
	}
	return null;
    }

    /**
     * <b>DOM</b>: Implements {@link
     * org.w3c.dom.Document#importNode(Node,boolean)}.
     */
    public Node importNode(Node importedNode, boolean deep)
        throws DOMException {
        if (importedNode instanceof AbstractNode) {
            AbstractNode an = (AbstractNode)importedNode;
            return (deep)
                ? an.deepExport(an.cloneNode(false), this)
                : an.export(an.cloneNode(false), this);
        } else {
            Node result;
            switch (importedNode.getNodeType()) {
            case ELEMENT_NODE:
                Element e = createElementNS(importedNode.getNamespaceURI(),
                                            importedNode.getNodeName());
                result = e;
                if (e.hasAttributes()) {
                    NamedNodeMap attr = importedNode.getAttributes();
                    int len = attr.getLength();
                    for (int i = 0; i < len; i++) {
                        Attr a = (Attr)attr.item(i);
                        if (a.getSpecified()) {
                            e.setAttributeNodeNS((Attr)importNode(a, true));
                        }
                    }
                }
                break;
                
            case ATTRIBUTE_NODE:
                result = createAttributeNS(importedNode.getNamespaceURI(),
                                           importedNode.getNodeName());
                break;

            case TEXT_NODE:
                result = createTextNode(importedNode.getNodeValue());
                deep = false;
                break;

            case CDATA_SECTION_NODE:
                result = createCDATASection(importedNode.getNodeValue());
                deep = false;
                break;

            case ENTITY_REFERENCE_NODE:
                result = createEntityReference(importedNode.getNodeName());
                break;

            case PROCESSING_INSTRUCTION_NODE:
                result = createProcessingInstruction
                    (importedNode.getNodeName(),
                     importedNode.getNodeValue());
                deep = false;
                break;

            case COMMENT_NODE:
                result = createComment(importedNode.getNodeValue());
                deep = false;
                break;

            default:
                throw createDOMException(DOMException.NOT_SUPPORTED_ERR,
                                         "import.node",
                                         new Object[] {});
            }

            if (deep) {
                for (Node n = importedNode.getFirstChild();
                     n != null;
                     n = n.getNextSibling()) {
                    result.appendChild(importNode(n, true));
                }
            }

            return result;
        }
    }

    /**
     * <b>DOM</b>: Implements {@link org.w3c.dom.Node#cloneNode(boolean)}.
     */
    public Node cloneNode(boolean deep) {
        Document n = (Document)newNode();
        copyInto(n);
        if (deep) {
            for (Node c = getFirstChild();
                 c != null;
                 c = c.getNextSibling()) {
                n.appendChild(n.importNode(c, deep));
            }
        }
        return n;
    }

    /**
     * Returns an ElementsByTagName object from the cache, if any.
     */
    public ElementsByTagName getElementsByTagName(Node n,
                                                  String ns,
                                                  String ln) {
        if (elementsByTagNames == null) {
            return null;
        }
        SoftDoublyIndexedTable t;
        t = (SoftDoublyIndexedTable)elementsByTagNames.get(n);
        if (t == null) {
            return null;
        }
        return (ElementsByTagName)t.get(ns, ln);
    }

    /**
     * Puts an ElementsByTagName object in the cache.
     */
    public void putElementsByTagName(Node n, String ns, String ln,
                                     ElementsByTagName l) {
        if (elementsByTagNames == null) {
            elementsByTagNames = new WeakHashMap(11);
        }
        SoftDoublyIndexedTable t;
        t = (SoftDoublyIndexedTable)elementsByTagNames.get(n);
        if (t == null) {
            elementsByTagNames.put(n, t = new SoftDoublyIndexedTable());
        }
        t.put(ns, ln, l);
    }

    // DocumentEvent /////////////////////////////////////////////////////////

    /**
     * <b>DOM</b>: Implements {@link
     * org.w3c.dom.events.DocumentEvent#createEvent(String)}.
     */
    public Event createEvent(String eventType) throws DOMException {
        if (documentEventSupport == null) {
            documentEventSupport =
                ((AbstractDOMImplementation)implementation).
                    createDocumentEventSupport();
        }
	return documentEventSupport.createEvent(eventType);
    }

    // DocumentTraversal /////////////////////////////////////////////////////

    /**
     * <b>DOM</b>: Implements {@link
     * DocumentTraversal#createNodeIterator(Node,int,NodeFilter,boolean)}.
     */
    public NodeIterator createNodeIterator(Node root,
                                           int whatToShow, 
                                           NodeFilter filter, 
                                           boolean entityReferenceExpansion)
        throws DOMException {
        if (traversalSupport == null) {
            traversalSupport = new TraversalSupport();
        }
        return traversalSupport.createNodeIterator(this, root, whatToShow,
                                                   filter,
                                                   entityReferenceExpansion);
    }

    /**
     * <b>DOM</b>: Implements {@link
     * DocumentTraversal#createTreeWalker(Node,int,NodeFilter,boolean)}.
     */
    public TreeWalker createTreeWalker(Node root, 
                                       int whatToShow, 
                                       NodeFilter filter, 
                                       boolean entityReferenceExpansion)
        throws DOMException {
        return TraversalSupport.createTreeWalker(this, root, whatToShow,
                                                 filter,
                                                 entityReferenceExpansion);
    }

    /**
     * Detaches the given node iterator from this document.
     */
    public void detachNodeIterator(NodeIterator it) {
        traversalSupport.detachNodeIterator(it);
    }

    /**
     * Notifies this document that a node will be removed.
     */
    public void nodeToBeRemoved(Node node) {
        if (traversalSupport != null) {
            traversalSupport.nodeToBeRemoved(node);
        }
    }

    /**
     * Returns the current document.
     */
    protected AbstractDocument getCurrentDocument() {
	return this;
    }

    /**
     * Exports this node to the given document.
     * @param n The clone node.
     * @param d The destination document.
     */
    protected Node export(Node n, Document d) {
	throw createDOMException(DOMException.NOT_SUPPORTED_ERR,
				 "import.document",
				 new Object[] {});
    }

    /**
     * Deeply exports this node to the given document.
     * @param n The clone node.
     * @param d The destination document.
     */
    protected Node deepExport(Node n, Document d) {
	throw createDOMException(DOMException.NOT_SUPPORTED_ERR,
				 "import.document",
				 new Object[] {});
    }

    /**
     * Copy the fields of the current node into the given node.
     * @param n a node of the type of this.
     */
    protected Node copyInto(Node n) {
	super.copyInto(n);
	AbstractDocument ad = (AbstractDocument)n;
	ad.implementation = implementation;
        ad.localizableSupport = new LocalizableSupport(RESOURCES);
	return n;
    }

    /**
     * Deeply copy the fields of the current node into the given node.
     * @param n a node of the type of this.
     */
    protected Node deepCopyInto(Node n) {
	super.deepCopyInto(n);
	AbstractDocument ad = (AbstractDocument)n;
	ad.implementation = implementation;
        ad.localizableSupport = new LocalizableSupport(RESOURCES);
	return n;
    }

    /**
     * Checks the validity of a node to be inserted.
     */
    protected void checkChildType(Node n) {
	short t = n.getNodeType();
	switch (t) {
	case ELEMENT_NODE:
	case PROCESSING_INSTRUCTION_NODE:
	case COMMENT_NODE:
	case DOCUMENT_TYPE_NODE:
	case DOCUMENT_FRAGMENT_NODE:
	    break;
	default:
	    throw createDOMException(DOMException.HIERARCHY_REQUEST_ERR,
				     "child.type",
				     new Object[] { new Integer(getNodeType()),
						    getNodeName(),
		                                    new Integer(t),
						    n.getNodeName() });
	}
	if ((t == ELEMENT_NODE && getDocumentElement() != null) ||
	    (t == DOCUMENT_TYPE_NODE && getDoctype() != null)) {
	    throw createDOMException(DOMException.HIERARCHY_REQUEST_ERR,
				     "child.type",
				     new Object[] { new Integer(getNodeType()),
						    getNodeName(),
		                                    new Integer(t),
						    n.getNodeName() });
	}
    }

    // Serializable /////////////////////////////////////////////////

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();

        s.writeObject(implementation.getClass().getName());
    }

    private void readObject(ObjectInputStream s) 
        throws IOException, ClassNotFoundException {
        s.defaultReadObject();

        localizableSupport = new LocalizableSupport(RESOURCES);

        Class c = Class.forName((String)s.readObject());

        try {
            Method m = c.getMethod("getDOMImplementation", null);
            implementation = (DOMImplementation)m.invoke(null, null);
        } catch (Exception e) {
            try {
                implementation = (DOMImplementation)c.newInstance();
            } catch (Exception ex) {
            }
        }
    }
}
