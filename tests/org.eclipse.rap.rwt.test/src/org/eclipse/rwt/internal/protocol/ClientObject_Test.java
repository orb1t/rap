/*******************************************************************************
* Copyright (c) 2011 EclipseSource and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    EclipseSource - initial API and implementation
*******************************************************************************/
package org.eclipse.rwt.internal.protocol;

import static org.eclipse.rwt.internal.resources.TestUtil.assertArrayEquals;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.rap.rwt.testfixture.*;
import org.eclipse.rap.rwt.testfixture.Message.*;
import org.eclipse.rwt.internal.lifecycle.JavaScriptResponseWriter;
import org.eclipse.rwt.internal.service.ContextProvider;
import org.eclipse.rwt.internal.service.IServiceStateInfo;
import org.eclipse.rwt.lifecycle.WidgetUtil;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.json.JSONArray;
import org.json.JSONException;


public class ClientObject_Test extends TestCase {

  private Shell shell;
  private String shellId;
  private IClientObject clientObject;

  @Override
  protected void setUp() throws Exception {
    Fixture.setUp();
    Fixture.fakeResponseWriter();
    Display display = new Display();
    shell = new Shell( display );
    shellId = WidgetUtil.getId( shell );
    clientObject = ClientObjectFactory.getForWidget( shell );
  }

  @Override
  protected void tearDown() throws Exception {
    Fixture.tearDown();
  }

  public void testCreate() {
    clientObject.create( "rwt.widgets.Shell" );

    CreateOperation operation = ( CreateOperation )getMessage().getOperation( 0 );
    assertEquals( shellId, operation.getTarget() );
    assertEquals( "rwt.widgets.Shell", operation.getType() );
  }

  public void testCreateIncludesSetProperties() {
    clientObject.create( "rwt.widgets.Shell" );
    clientObject.setProperty( "foo", 23 );

    Message message = getMessage();
    assertEquals( 1, message.getOperationCount() );
    assertTrue( message.getOperation( 0 ) instanceof CreateOperation );
    assertEquals( new Integer( 23 ), message.getOperation( 0 ).getProperty( "foo" ) );
  }

  public void testSetProperty() {
    clientObject.setProperty( "key", ( Object )"value" );
    clientObject.setProperty( "key2", 2 );
    clientObject.setProperty( "key3", 3.5 );
    clientObject.setProperty( "key4", true );
    clientObject.setProperty( "key5", "aString" );

    SetOperation operation = ( SetOperation )getMessage().getOperation( 0 );
    assertEquals( shellId, operation.getTarget() );
    assertEquals( "value", operation.getProperty( "key" ) );
    assertEquals( new Integer( 2 ), operation.getProperty( "key2" ) );
    assertEquals( new Double( 3.5 ), operation.getProperty( "key3" ) );
    assertEquals( Boolean.TRUE, operation.getProperty( "key4" ) );
    assertEquals( "aString", operation.getProperty( "key5" ) );
  }

  public void testSetPropertyForIntArray() throws JSONException {
    clientObject.setProperty( "key", new int[]{ 1, 2, 3 } );

    SetOperation operation = ( SetOperation )getMessage().getOperation( 0 );
    JSONArray result = ( JSONArray )operation.getProperty( "key" );
    assertEquals( 3, result.length() );
    assertEquals( 1, result.getInt( 0 ) );
    assertEquals( 2, result.getInt( 1 ) );
    assertEquals( 3, result.getInt( 2 ) );
  }

  public void testCreatePropertyGetStyle() {
    clientObject.create( "rwt.widgets.Shell"  );
    clientObject.setProperty( "style", new String[] { "PUSH", "BORDER" } );

    CreateOperation operation = ( CreateOperation )getMessage().getOperation( 0 );
    assertArrayEquals( new String[] { "PUSH", "BORDER" }, operation.getStyles() );
  }

  public void testDestroy() {
    clientObject.destroy();

    DestroyOperation operation = ( DestroyOperation )getMessage().getOperation( 0 );
    assertEquals( shellId, operation.getTarget() );
  }

  public void testAddListener() {
    clientObject.addListener( "selection" );
    clientObject.addListener( "fake" );

    ListenOperation operation = ( ListenOperation )getMessage().getOperation( 0 );
    assertEquals( shellId, operation.getTarget() );
    assertTrue( operation.listensTo( "selection" ) );
    assertTrue( operation.listensTo( "fake" ) );
  }

  public void testRemoveListener() {
    clientObject.removeListener( "selection" );
    clientObject.removeListener( "fake" );
    clientObject.addListener( "fake2" );

    ListenOperation operation = ( ListenOperation )getMessage().getOperation( 0 );
    assertEquals( shellId, operation.getTarget() );
    assertFalse( operation.listensTo( "selection" ) );
    assertFalse( operation.listensTo( "fake" ) );
    assertTrue( operation.listensTo( "fake2" ) );
  }

  public void testCall() {
    clientObject.call( "method", null );

    CallOperation operation = ( CallOperation )getMessage().getOperation( 0 );
    assertEquals( shellId, operation.getTarget() );
    assertEquals( "method", operation.getMethodName() );
  }

  public void testCallTwice() {
    clientObject.call( "method", null );
    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put( "key1", "a" );
    properties.put( "key2", new Integer( 3 ) );

    clientObject.call( "method2", properties );

    CallOperation operation = ( CallOperation )getMessage().getOperation( 1 );
    assertEquals( shellId, operation.getTarget() );
    assertEquals( "method2", operation.getMethodName() );
    assertEquals( "a", operation.getProperty( "key1" ) );
    assertEquals( new Integer( 3 ), operation.getProperty( "key2" ) );
  }

  public void testExecuteScript() {
    clientObject.executeScript( "text/javascript", "var x = 5;" );

    ExecuteScriptOperation operation = ( ExecuteScriptOperation )getMessage().getOperation( 0 );
    assertEquals( shellId, operation.getTarget() );
    assertEquals( "text/javascript", operation.getScriptType() );
    assertEquals( "var x = 5;", operation.getScript() );
  }

  private Message getMessage() {
    closeProtocolWriter();
    String markup = Fixture.getAllMarkup();
    return new Message( markup );
  }

  private void closeProtocolWriter() {
    IServiceStateInfo stateInfo = ContextProvider.getStateInfo();
    JavaScriptResponseWriter writer = stateInfo.getResponseWriter();
    writer.finish();
  }
}