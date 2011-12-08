/*******************************************************************************
 * Copyright (c) 2008, 2011 Innoopract Informationssysteme GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Innoopract Informationssysteme GmbH - initial API and implementation
 *     EclipseSource - ongoing development
 ******************************************************************************/

/**
 * Asynchronous key event mechanism. The usual program flow is:
 * * cancel the event
 * * send the event to server (asynchronously)
 * * if the doit-flag was left to true while processing the server-side
 *   key event, the reponse calls a function that re-dispatches the event.
 *   Otherwise nothing interesting happens. The event was already cancelled.
 *
 * Now for the nightmare part:
 * Certain keys on certain input elements are considered "untrusted" by
 * Gecko-based browsers. Untrusted key events are not processed if they
 * were created programmatically.
 * For example the text input element does not accept programmatically
 * created arrow keys.
 *
 * As described above, it is crucial to be able to first cancel an event and
 * later on re-dispatch it. The only "solution" that is left of now  is to
 * filter out these key events and let them pass without giving the server a
 * chance to process them.
 */
qx.Class.define( "org.eclipse.rwt.AsyncKeyEventUtil",
{
  type : "singleton",
  extend : qx.core.Object,

  /* Within this file, an EventInfo data structure is used. It is declared
   * as follows (pseudo-syntax, borrowed from Java):
   * EventInfo {
       HTMLElement target,
       String type,
       boolean bubbles,
       HTMLElement view,
       boolean ctrlKey,
       boolean altKey,
       boolean shiftKey,
       boolean metaKey,
       int keyCode,
       int charCode,
       boolean isChar
   * }
   */

  construct : function() {
    this.base( arguments );
    this._pendingEventInfo = null;
    this._allowIntercept = true;
    this._bufferedEvents = new Array();
    this._keyEventRequestRunning = false;
    var req = org.eclipse.swt.Request.getInstance();
    req.addEventListener( "received", this._onRequestReceived, this );
  },

  destruct : function() {
    var req = org.eclipse.swt.Request.getInstance();
    req.removeEventListener( "received", this._onRequestReceived, this );
  },

  members : {
    _untrustedKeyCodes : qx.core.Variant.select( "qx.client", {
      "gecko" : [
        37,  // Left
        38,  // Up
        39,  // Right
        40,  // Down
        35,  // End
        36,  // Home
        45,  // Insert
        46,  // Delete
        112, // F1
        113, // F2
        114, // F...
        115,
        116,
        117,
        118,
        119,
        120,
        121,
        122,
        123  // F12
      ],
      "default" : []
    } ),

    intercept : function( eventType, keyCode, charCode, domEvent ) {
      var keyUtil = org.eclipse.rwt.KeyEventUtil.getInstance();
      var result = false;
      if( this._allowIntercept ) {
        var control = keyUtil._getTargetControl();
        var hasKeyListener = keyUtil._hasKeyListener( control );
        var hasTraverseListener = keyUtil._hasTraverseListener( control );
        var isTraverseKey = false;
        if( hasTraverseListener ) {
          isTraverseKey = keyUtil._isTraverseKey( keyCode );
        }
        if( hasKeyListener || ( hasTraverseListener && isTraverseKey ) ) {
          if( !this._isUntrustedKey( keyCode, domEvent ) ) {
            if( this._keyEventRequestRunning ) {
              this._bufferedEvents.push( this._getEventInfo( domEvent ) );
              this._cancelDomEvent( domEvent );
              result = true;
            } else if( keyUtil._isRelevantEvent( eventType, keyCode ) ) {              
              this._pendingEventInfo = this._getEventInfo( domEvent );
              this._sendKeyDown( control, keyCode, charCode, domEvent );
              this._cancelDomEvent( domEvent );
              result = true;
            }
          }
        }
      }
      return result;
    },

    cancelEvent : function() {
      this._pendingEventInfo = null;
    },

    allowEvent : function() {
      if( this._pendingEventInfo !== null ) {
        // [rst] switch suspend off while re-dispatching event
        //       See https://bugs.eclipse.org/bugs/show_bug.cgi?id=261532
        var suspendBuffer = org.eclipse.swt.EventUtil.getSuspended();
        org.eclipse.swt.EventUtil.setSuspended( false );
        this._allowIntercept = false;      
        this._redispatchKeyEvent( this._pendingEventInfo );
        this._allowIntercept = true;      
        org.eclipse.swt.EventUtil.setSuspended( suspendBuffer );
      }
    },

    _isUntrustedKey : function( keyCode, domEvent  ) {
      var result = false;
      if( qx.core.Variant.isSet( "qx.client", "gecko" ) ) {
        var tagName = domEvent.target.tagName.toLowerCase();
        if( tagName == "input" || tagName == "textarea" ) {
          // Check for CTRL key fixes bug 282837
          if( domEvent.ctrlKey ) {
            result = true;
          } else {
            for( var i = 0; !result && i < this._untrustedKeyCodes.length; i++ ) {
              if( this._untrustedKeyCodes[ i ] === keyCode ) {
                result = true;
              }
            }
          }
        }
      }
      return result;
    },
    
    _cancelDomEvent : function( domEvent ) {
      if( qx.core.Variant.isSet( "qx.client", "mshtml" ) ) {
        domEvent.returnValue = false;
        domEvent.cancelBubble = true;
      } else {
        domEvent.preventDefault();
        domEvent.stopPropagation();
      }
    },

    _sendKeyDown : function( widget, keyCode, charCode, domEvent ) {
      var keyUtil = org.eclipse.rwt.KeyEventUtil.getInstance();
	    keyUtil._attachKeyDown( widget, keyCode, charCode, domEvent );
      this._keyEventRequestRunning = true;
      org.eclipse.swt.Request.getInstance()._sendImmediate( true );
    },

    _onRequestReceived : function( evt ) {
      if( this._keyEventRequestRunning ) {
        this._keyEventRequestRunning = false;
        this._checkBufferedEvents();
      }
    },

    _getEventInfo : function( event ) {
      result = {
        target: event.target || event.srcElement,
        type: event.type,
        bubbles: event.bubbles,
        view: event.view,
        ctrlKey: event.ctrlKey,
        altKey:  event.altKey,
        shiftKey: event.shiftKey,
        keyCode: event.keyCode,
        isChar: event.isChar,
        pageX: event.pageX,
        pageY: event.pageY
      };
      result.charCode = event.type === "keypress" ? event.charCode : 0;
      return result;
    },

    _checkBufferedEvents : function() {
      while( this._bufferedEvents.length > 0 && !this._keyEventRequestRunning )
      {
        var size = this._bufferedEvents.length;
        var oldEvent = this._bufferedEvents.shift();
        this._redispatchKeyEvent( oldEvent );
      }
    },

    _redispatchKeyEvent : function( eventInfo ) {
      if( qx.core.Variant.isSet( "qx.client", "gecko" ) ) {
        var newEvent = document.createEvent( "KeyboardEvent" );
        newEvent.initKeyEvent( eventInfo.type,
                               eventInfo.bubbles,
                               true, // cancelable
                               eventInfo.view,
                               eventInfo.ctrlKey,
                               eventInfo.altKey,
                               eventInfo.shiftKey,
                               eventInfo.metaKey,
                               eventInfo.keyCode,
                               eventInfo.charCode );
        eventInfo.target.dispatchEvent( newEvent );
      } else if( qx.core.Variant.isSet( "qx.client", "webkit" ) ) {
        var newEvent = document.createEvent( "Events" );
        newEvent.initEvent( eventInfo.type, eventInfo.bubbles, true );
        newEvent.view = eventInfo.view;
        newEvent.ctrlKey = eventInfo.ctrlKey;
        newEvent.altKey = eventInfo.altKey;
        newEvent.metaKey = eventInfo.metaKey;
        newEvent.keyCode = eventInfo.keyCode;
        eventInfo.target.dispatchEvent( newEvent );
      } else if( qx.core.Variant.isSet( "qx.client", "mshtml" ) ) {
        var newEvent = document.createEventObject();
        newEvent.type = eventInfo.type;
        newEvent.cancelable = true;
        if( eventInfo.view ) {
          newEvent.view = eventInfo.view;
        }
        newEvent.ctrlKey = eventInfo.ctrlKey;
        newEvent.altKey = eventInfo.altKey;
        newEvent.metaKey = eventInfo.metaKey;
        newEvent.keyCode = eventInfo.keyCode;
        eventInfo.srcElement.fireEvent( newEvent );
      } else {
        throw new Error( "Redispatching key events not supported" );
      }
    }

  }
} );
