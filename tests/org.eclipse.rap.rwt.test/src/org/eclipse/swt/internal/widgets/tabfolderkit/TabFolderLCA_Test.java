/*******************************************************************************
 * Copyright (c) 2007, 2012 Innoopract Informationssysteme GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Innoopract Informationssysteme GmbH - initial API and implementation
 *    EclipseSource - ongoing development
 ******************************************************************************/
package org.eclipse.swt.internal.widgets.tabfolderkit;

import static org.eclipse.rap.rwt.lifecycle.WidgetUtil.getId;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.rap.rwt.graphics.Graphics;
import org.eclipse.rap.rwt.internal.protocol.ClientMessageConst;
import org.eclipse.rap.rwt.lifecycle.IWidgetAdapter;
import org.eclipse.rap.rwt.lifecycle.PhaseId;
import org.eclipse.rap.rwt.lifecycle.WidgetUtil;
import org.eclipse.rap.rwt.testfixture.Fixture;
import org.eclipse.rap.rwt.testfixture.Message;
import org.eclipse.rap.rwt.testfixture.Message.CreateOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.widgets.Props;
import org.eclipse.swt.internal.widgets.controlkit.ControlLCATestUtil;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.mockito.ArgumentCaptor;

public class TabFolderLCA_Test extends TestCase {

  private Display display;
  private Shell shell;
  private TabFolder folder;
  private TabFolderLCA lca;

  @Override
  protected void setUp() throws Exception {
    Fixture.setUp();
    display = new Display();
    shell = new Shell( display );
    folder = new TabFolder( shell, SWT.NONE );
    lca = new TabFolderLCA();
    Fixture.fakeNewRequest( display );
  }

  @Override
  protected void tearDown() throws Exception {
    Fixture.tearDown();
  }

  public void testControlListeners() throws IOException {
    ControlLCATestUtil.testActivateListener( folder );
    ControlLCATestUtil.testFocusListener( folder );
    ControlLCATestUtil.testMouseListener( folder );
    ControlLCATestUtil.testKeyListener( folder );
    ControlLCATestUtil.testTraverseListener( folder );
    ControlLCATestUtil.testMenuDetectListener( folder );
    ControlLCATestUtil.testHelpListener( folder );
  }

  public void testPreserveValues() {
    Fixture.markInitialized( display );
    //control: enabled
    Fixture.preserveWidgets();
    IWidgetAdapter adapter = WidgetUtil.getAdapter( folder );
    assertEquals( Boolean.TRUE, adapter.getPreserved( Props.ENABLED ) );
    Fixture.clearPreserved();
    folder.setEnabled( false );
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( folder );
    assertEquals( Boolean.FALSE, adapter.getPreserved( Props.ENABLED ) );
    Fixture.clearPreserved();
    folder.setEnabled( true );
    //visible
    folder.setSize( 10, 10 );
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( folder );
    assertEquals( Boolean.TRUE, adapter.getPreserved( Props.VISIBLE ) );
    Fixture.clearPreserved();
    folder.setVisible( false );
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( folder );
    assertEquals( Boolean.FALSE, adapter.getPreserved( Props.VISIBLE ) );
    Fixture.clearPreserved();
    //menu
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( folder );
    assertEquals( null, adapter.getPreserved( Props.MENU ) );
    Fixture.clearPreserved();
    Menu menu = new Menu( folder );
    MenuItem item = new MenuItem( menu, SWT.NONE );
    item.setText( "1 Item" );
    folder.setMenu( menu );
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( folder );
    assertEquals( menu, adapter.getPreserved( Props.MENU ) );
    Fixture.clearPreserved();
    //bound
    Rectangle rectangle = new Rectangle( 10, 10, 30, 50 );
    folder.setBounds( rectangle );
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( folder );
    assertEquals( rectangle, adapter.getPreserved( Props.BOUNDS ) );
    Fixture.clearPreserved();
    //foreground background font
    Color background = Graphics.getColor( 122, 33, 203 );
    folder.setBackground( background );
    Color foreground = Graphics.getColor( 211, 178, 211 );
    folder.setForeground( foreground );
    Font font = Graphics.getFont( "font", 12, SWT.BOLD );
    folder.setFont( font );
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( folder );
    assertEquals( background, adapter.getPreserved( Props.BACKGROUND ) );
    assertEquals( foreground, adapter.getPreserved( Props.FOREGROUND ) );
    assertEquals( font, adapter.getPreserved( Props.FONT ) );
    Fixture.clearPreserved();
    //tooltiptext
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( folder );
    assertEquals( null, folder.getToolTipText() );
    Fixture.clearPreserved();
    folder.setToolTipText( "some text" );
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( folder );
    assertEquals( "some text", folder.getToolTipText() );
  }

  public void testSelectionWithoutListener() {
    TabItem item0 = new TabItem( folder, SWT.NONE );
    Control control0 = new Button( folder, SWT.PUSH );
    item0.setControl( control0 );
    TabItem item1 = new TabItem( folder, SWT.NONE );
    Control control1 = new Button( folder, SWT.PUSH );
    item1.setControl( control1 );

    fakeWidgetSelected( folder, item1 );
    Fixture.readDataAndProcessAction( display );

    assertEquals( 1, folder.getSelectionIndex() );
    assertFalse( control0.getVisible() );
    assertTrue( control1.getVisible() );
  }

  public void testSelectionWithListener() {
    Fixture.fakePhase( PhaseId.PROCESS_ACTION );
    TabItem item0 = new TabItem( folder, SWT.NONE );
    Control control0 = new Button( folder, SWT.PUSH );
    item0.setControl( control0 );
    TabItem item1 = new TabItem( folder, SWT.NONE );
    Control control1 = new Button( folder, SWT.PUSH );
    item1.setControl( control1 );
    SelectionListener listener = mock( SelectionListener.class );
    folder.addSelectionListener( listener );

    fakeWidgetSelected( folder, item1 );
    Fixture.readDataAndProcessAction( display );

    assertEquals( 1, folder.getSelectionIndex() );
    assertFalse( control0.getVisible() );
    assertTrue( control1.getVisible() );
    ArgumentCaptor<SelectionEvent> captor = ArgumentCaptor.forClass( SelectionEvent.class );
    verify( listener ).widgetSelected( captor.capture() );
    SelectionEvent event = captor.getValue();
    assertSame( item1, event.item );
    assertSame( folder, event.widget );
    assertTrue( event.doit );
    assertEquals( 0, event.x );
    assertEquals( 0, event.y );
    assertEquals( 0, event.width );
    assertEquals( 0, event.height );
    assertEquals( 0, event.detail );
    assertNull( event.text );
  }

  public void testRenderCreate() throws IOException {
    lca.renderInitialization( folder );

    Message message = Fixture.getProtocolMessage();
    CreateOperation operation = message.findCreateOperation( folder );
    assertEquals( "rwt.widgets.TabFolder", operation.getType() );
    Object[] styles = operation.getStyles();
    assertTrue( Arrays.asList( styles ).contains( "TOP" ) );
  }

  public void testRenderCreateOnBottom() throws IOException {
    folder = new TabFolder( shell, SWT.BOTTOM );

    lca.renderInitialization( folder );

    Message message = Fixture.getProtocolMessage();
    CreateOperation operation = message.findCreateOperation( folder );
    assertEquals( "rwt.widgets.TabFolder", operation.getType() );
    Object[] styles = operation.getStyles();
    assertTrue( Arrays.asList( styles ).contains( "BOTTOM" ) );
  }

  public void testRenderParent() throws IOException {
    lca.renderInitialization( folder );

    Message message = Fixture.getProtocolMessage();
    CreateOperation operation = message.findCreateOperation( folder );
    assertEquals( WidgetUtil.getId( folder.getParent() ), operation.getParent() );
  }

  public void testRenderInitialSelectionWithoutItems() throws IOException {
    lca.render( folder );

    Message message = Fixture.getProtocolMessage();
    CreateOperation operation = message.findCreateOperation( folder );
    assertTrue( operation.getPropertyNames().indexOf( "selection" ) == -1 );
  }

  public void testRenderInitialSelectionWithItems() throws IOException {
    TabItem item = new TabItem( folder, SWT.NONE );
    new TabItem( folder, SWT.NONE );
    new TabItem( folder, SWT.NONE );

    lca.render( folder );

    Message message = Fixture.getProtocolMessage();
    CreateOperation operation = message.findCreateOperation( folder );
    assertEquals( WidgetUtil.getId( item ), operation.getProperty( "selection" ) );
  }

  public void testRenderSelection() throws IOException {
    new TabItem( folder, SWT.NONE );
    TabItem item = new TabItem( folder, SWT.NONE );
    new TabItem( folder, SWT.NONE );

    folder.setSelection( 1 );
    lca.renderChanges( folder );

    Message message = Fixture.getProtocolMessage();
    assertEquals( WidgetUtil.getId( item ), message.findSetProperty( folder, "selection" ) );
  }

  public void testRenderSelectionUnchanged() throws IOException {
    new TabItem( folder, SWT.NONE );
    new TabItem( folder, SWT.NONE );
    new TabItem( folder, SWT.NONE );
    Fixture.markInitialized( display );
    Fixture.markInitialized( folder );

    folder.setSelection( 1 );
    Fixture.preserveWidgets();
    lca.renderChanges( folder );

    Message message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( folder, "selection" ) );
  }

  public void testReadSelection() {
    TabItem item = new TabItem( folder, SWT.NONE );
    folder.setSelection( new TabItem[ 0 ] );

    fakeWidgetSelected( folder, item );
    Fixture.readDataAndProcessAction( folder );

    assertSame( item, folder.getSelection()[ 0 ] );
  }

  private void fakeWidgetSelected( TabFolder folder, TabItem item ) {
    Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put( ClientMessageConst.EVENT_PARAM_ITEM, getId( item ) );
    Fixture.fakeNotifyOperation( getId( folder ),
                                 ClientMessageConst.EVENT_SELECTION,
                                 parameters );
  }
}
