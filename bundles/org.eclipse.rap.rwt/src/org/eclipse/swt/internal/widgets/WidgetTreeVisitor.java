/*******************************************************************************
 * Copyright (c) 2015 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.swt.internal.widgets;

import org.eclipse.swt.widgets.Widget;


public interface WidgetTreeVisitor {

  /**
   * Visit a widget.
   *
   * @param widget the widget that is visited
   * @return whether children and sub-widgets should be visited
   */
  public boolean visit( Widget widget );

}
