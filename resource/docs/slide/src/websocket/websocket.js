(function() {
  var wsUri;
  var wsConsoleLog;
  var wsConnectBut;
  var wsDisconnectBut;
  var wsMessage;
  var wsSendBut;
  var wsClearLogBut;

  var wsUserDisconnectedFlag;

  function wsHandlePageLoad() {

    wsUri = document.getElementById("wsUri");
    wsToggleTls();
    
    wsConnectBut = document.getElementById("wsConnectBut");
    wsConnectBut.onclick = wsDoConnect;
    
    wsDisconnectBut = document.getElementById("wsDisconnectBut");
    wsDisconnectBut.onclick = wsDoDisconnect;
    
    wsMessage = document.getElementById("wsMessage");

    wsSendBut = document.getElementById("wsSendBut");
    wsSendBut.onclick = wsDoSend;

    wsConsoleLog = document.getElementById("wsConsoleLog");

    wsClearLogBut = document.getElementById("wsClearLogBut");
    wsClearLogBut.onclick = wsClearLog;

    wsUserDisconnectedFlag = false;
    
    wsSetGuiConnected(false);
  }

  function wsToggleTls() {
      wsUri.value = "ws://61.250.201.157:9000/websocket/bleujin";
  }

  function wsDoConnect() {
    if (!window.WebSocket) {
      wsLogToConsole('<span style="color: red;"><strong>Error:</strong>' + 
          'Your browser does not have native support for WebSocket</span>',
          true);
      return;
    }
    wsConnectBut.disabled = true;
    websocket = new WebSocket(wsUri.value);
    websocket.onopen = function(evt) { wsOnOpen(evt) };
    websocket.onclose = function(evt) { wsOnClose(evt) };
    websocket.onmessage = function(evt) { wsOnMessage(evt) };
    websocket.onerror = function(evt) { wsOnError(evt) };
  }

  function wsDoDisconnect() {
    wsUserDisconnectedFlag = true;
    websocket.close()
  }
  
  function wsDoSend() {
    wsLogToConsole("SENT: " + wsMessage.value);
    websocket.send(wsMessage.value);
  }

  function wsLogToConsole(message, ignoreSecureTag) {
    var pre = document.createElement("p");
    pre.style.wordWrap = "break-word";
    pre.innerHTML = "";
    if (!ignoreSecureTag) {
      pre.innerHTML = wsGetSecureTag();
    }
    pre.innerHTML = pre.innerHTML+message;
    wsConsoleLog.appendChild(pre);

    while (wsConsoleLog.childNodes.length > 50) {
      wsConsoleLog.removeChild(wsConsoleLog.firstChild);
    }

    wsConsoleLog.scrollTop = wsConsoleLog.scrollHeight;
  }

  function wsOnOpen(evt) {
    wsLogToConsole("CONNECTED");
    wsSetGuiConnected(true);
  }
  
  function wsOnClose(evt) {
    wsLogToConsole("DISCONNECTED");

    //console.log("wsUserDisconnectedFlag=" + wsUserDisconnectedFlag);
    // If the user tried a regular WebSocket connection it it closed because of
    // an intermediary firewall or proxy server, then display a note advising them
    // to use a secure WebSocket instead.
    //
    if (!wsUserDisconnectedFlag ) {
      wsLogToConsole("NOTE: If the connection failed, check the <strong>" + 
          "Use secure WebSocket (TLS/SSL)</strong> checkbox and try again.",
          true);
    }

    wsUserDisconnectedFlag = false;

    wsSetGuiConnected(false);
  }
  
  function wsOnMessage(evt) {
    wsLogToConsole('<span style="color: blue;">RESPONSE: ' + evt.data+'</span>');
  }

  function wsOnError(evt) {
    wsLogToConsole('<span style="color: red;">ERROR:</span> ' + evt.data);
  }

  function wsSetGuiConnected(isConnected) {
    wsUri.disabled = isConnected;
    wsConnectBut.disabled = isConnected;
    wsDisconnectBut.disabled = !isConnected;
    wsMessage.disabled = !isConnected;
    wsSendBut.disabled = !isConnected;
    labelColor = isConnected ? "#999999" : "black";
  }

  function wsClearLog() {
    while (wsConsoleLog.childNodes.length) {
      wsConsoleLog.removeChild(wsConsoleLog.lastChild);
    }
  }

  function wsGetSecureTag() {
    return '';
  }

  window.addEventListener("load", wsHandlePageLoad, false);

})();
