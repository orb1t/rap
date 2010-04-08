/*******************************************************************************
 * Copyright (c) 2007, 2009 Innoopract Informationssysteme GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Innoopract Informationssysteme GmbH - initial API and implementation
 *     EclipseSource - ongoing development
 ******************************************************************************/
package org.eclipse.rwt.internal.theme;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.*;

import org.eclipse.rwt.internal.service.ServletLog;
import org.w3c.dom.*;
import org.xml.sax.*;


/**
 * Reader for theme definition files. These are the "*.theme.xml" files
 * that define themeable properties of a certain widget.
 */
public final class ThemeDefinitionReader {

  private static final String ELEM_ROOT = "theme";

  private static final String ELEM_ELEMENT = "element";

  private static final String ELEM_PROPERTY = "property";

  private static final String ELEM_STYLE = "style";

  private static final String ELEM_STATE = "state";
  
  private static final String ATTR_NAME = "name";

  private static final String THEME_DEF_SCHEMA = "themedef.xsd";

  private static final String JAXP_SCHEMA_LANGUAGE
    = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

  private static final String W3C_XML_SCHEMA
    = "http://www.w3.org/2001/XMLSchema";

  private final InputStream inputStream;
  private final String fileName;
  private final Collection cssElements;

  /**
   * An instance of this class reads theme definitions from an XML resource.
   *
   * @param inputStream input stream from a theme definition XML
   * @param fileName the file name to refer to in (error) messages
   */
  public ThemeDefinitionReader( final InputStream inputStream,
                                final String fileName )
  {
    if( inputStream == null ) {
      throw new NullPointerException( "null argument" );
    }
    this.inputStream = inputStream;
    this.fileName = fileName;
    this.cssElements = new ArrayList();
  }

  /**
   * Reads a theme definition from the specified stream. The stream is kept open
   * after reading.
   *
   * @throws IOException if a I/O error occurs
   * @throws SAXException if a parse error occurs
   */
  public void read() throws SAXException, IOException {
    Document document;
    document = parseThemeDefinition( inputStream );
    Node root = document.getElementsByTagName( ELEM_ROOT ).item( 0 );
    NodeList childNodes = root.getChildNodes();
    for( int i = 0; i < childNodes.getLength(); i++ ) {
      Node node = childNodes.item( i );
      if( node.getNodeType() == Node.ELEMENT_NODE ) {
        if( ELEM_ELEMENT.equals( node.getNodeName() ) ) {
          readElement( node );
        }
      }
    }
  }

  /**
   * Returns the CSS element names in the definition.
   */
  public IThemeCssElement[] getThemeCssElements() {
    IThemeCssElement[] result = new IThemeCssElement[ cssElements.size() ];
    cssElements.toArray( result );
    return result;
  }

  private void readElement( final Node node ) {
    String name = getAttributeValue( node, ATTR_NAME );
    ThemeCssElement themeElement = new ThemeCssElement( name );
    cssElements.add( themeElement );
    NodeList childNodes = node.getChildNodes();
    for( int i = 0; i < childNodes.getLength(); i++ ) {
      Node childNode = childNodes.item( i );
      if( childNode.getNodeType() == Node.ELEMENT_NODE ) {
        if( ELEM_ELEMENT.equals( childNode.getNodeName() ) ) {
          readElement( childNode );
        } else if( ELEM_PROPERTY.equals( childNode.getNodeName() ) ) {
          themeElement.addProperty( getAttributeValue( childNode, ATTR_NAME ) );
        } else if( ELEM_STYLE.equals( childNode.getNodeName() ) ) {
          themeElement.addStyle( getAttributeValue( childNode, ATTR_NAME ) );
        } else if( ELEM_STATE.equals( childNode.getNodeName() ) ) {
          themeElement.addState( getAttributeValue( childNode, ATTR_NAME ) );
        }
      }
    }
  }

  private Document parseThemeDefinition( final InputStream is )
    throws SAXException, IOException
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware( true );
    ClassLoader loader = ThemeDefinitionReader.class.getClassLoader();
    final URL schema = loader.getResource( THEME_DEF_SCHEMA );
    factory.setValidating( schema != null );
    try {
      factory.setAttribute( JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA );
    } catch( final IllegalArgumentException iae ) {
      // XML-Processing does not support JAXP 1.2 or greater
      factory.setNamespaceAware( false );
      factory.setValidating( false );
    }
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
    } catch( final ParserConfigurationException e ) {
      String message = "Failed to initialize parser for theme definition files";
      throw new RuntimeException( message, e );
    }
//    builder.setEntityResolver( new EntityResolver() {
//      public InputSource resolveEntity( final String publicID,
//                                        final String systemID )
//        throws IOException, SAXException
//      {
//        InputSource result = null;
//        if( schema != null && systemID.endsWith( THEME_DEF_SCHEMA ) ) {
//          URLConnection connection = schema.openConnection();
//          connection.setUseCaches( false );
//          result = new InputSource( connection.getInputStream() );
//        }
//        return result;
//      }
//    } );
    builder.setErrorHandler( new ThemeDefinitionErrorHandler() );
    return builder.parse( is );
  }

  private static String getAttributeValue( final Node node, final String name )
  {
    String result = null;
    NamedNodeMap attributes = node.getAttributes();
    if( attributes != null ) {
      Node namedItem = attributes.getNamedItem( name );
      if( namedItem != null ) {
        result = namedItem.getNodeValue();
      }
    }
    return result;
  }

  private class ThemeDefinitionErrorHandler implements ErrorHandler {
    public void error( final SAXParseException spe ) throws SAXException {
      String msg
        = "Error parsing theme definition " + getPosition( spe ) + ":";
      ServletLog.log( msg, null );
      ServletLog.log( spe.getMessage(), null );
    }

    public void fatalError( final SAXParseException spe ) throws SAXException {
      String
        msg = "Fatal error parsing theme definition " + getPosition( spe ) + ":";
      ServletLog.log( msg, null );
      ServletLog.log( spe.getMessage(), null );
    }

    public void warning( final SAXParseException spe ) throws SAXException {
      String msg
        = "Warning parsing theme definition " + getPosition( spe ) + ":";
      ServletLog.log( msg, null );
      ServletLog.log( spe.getMessage(), null );
    }

    private String getPosition( final SAXParseException spe ) {
      String result
        = "in file '" 
        + fileName 
        + "' at line " 
        + spe.getLineNumber() 
        + ", col " 
        + spe.getColumnNumber();
      return result;
    }
  }
}
