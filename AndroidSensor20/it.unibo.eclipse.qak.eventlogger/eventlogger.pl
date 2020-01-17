%====================================================================================
% eventlogger description   
%====================================================================================
mqttBroker("192.168.1.247", "1883").
context(ctxeventlogger, "localhost",  "MQTT", "0").
 qactor( eventlogger1, ctxeventlogger, "it.unibo.eventlogger1.Eventlogger1").
  qactor( eventlogger2, ctxeventlogger, "it.unibo.eventlogger2.Eventlogger2").
