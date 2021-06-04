package object chapter3 {
  case class DeviceIoTDataDomain (battery_level: Long, c02_level: Long,
                            cca2: String, cca3: String, cn: String, device_id: Long,
                            device_name: String, humidity: Long, ip: String, latitude: Double,
                            lcd: String, longitude: Double, scale:String, temp: Long,
                            timestamp: Long)

  case class DeviceTempByCountry(temp: Long, device_name: String, device_id: Long,
                                 cca3: String)
}
