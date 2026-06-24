## 荣睿的aar包更改
- 删除原厂aar包中的cn.pda.serialport.SerialPort
- 更改原厂aar包中的com.rfid.serialport.SerialPort导入的so文件名称，System.loadLibrary("serial_port")改成System.loadLibrary("rr_serial_port")
- 更改原厂提供过来的libserial_port.so文件名为librr_serial_port.so
## 国芯的jar包和so文件更改
- 更改原厂reader(1).jar中com.gxwl.device.reader.dal.SerialPort导入的so文件名称，System.loadLibrary("SerialPort")改为System.loadLibrary("gx_SerialPort")
- 更改原厂提供过来的libSerialPort.so文件名为libgx_SerialPort.so