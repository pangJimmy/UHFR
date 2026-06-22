## 荣睿的aar包更改
- 删除原厂aar包中的cn.pda.serialport.SerialPort
- 更改原厂aar包中的com.rfid.serialport.SerialPort导入的so文件名称，System.loadLibrary("serial_port")改成System.loadLibrary("rr_serial_port")
- 更改原厂提供过来的libserial_port.so文件名为librr_serial_port.so