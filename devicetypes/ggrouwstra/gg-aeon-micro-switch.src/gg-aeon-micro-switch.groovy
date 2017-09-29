metadata {
	// Automatically generated. Make future change here.
	definition (name: "GG - Aeon Micro Switch", namespace: "ggrouwstra", author: "Garrette Grouwstra") {
		capability "Energy Meter"
		capability "Actuator"
		capability "Switch"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
        capability "Power Meter"

		command "reset"

		fingerprint deviceId: "0x1001", inClusters: "0x25,0x32,0x27,0x2C,0x2B,0x70,0x85,0x56,0x72,0x86", outClusters: "0x82"
	}

	// simulator metadata
	simulator {
		status "on":  "command: 2003, payload: FF"
		status "off": "command: 2003, payload: 00"
for (int i = 0; i <= 10000; i += 1000) {
			status "power  ${i} W": new physicalgraph.zwave.Zwave().meterV1.meterReport(
				scaledMeterValue: i, precision: 3, meterType: 4, scale: 2, size: 4).incomingMessage()
		}
		for (int i = 0; i <= 100; i += 10) {
			status "energy  ${i} kWh": new physicalgraph.zwave.Zwave().meterV1.meterReport(
				scaledMeterValue: i, precision: 3, meterType: 0, scale: 0, size: 4).incomingMessage()
		}

		// reply messages
		reply "2001FF,delay 100,2502": "command: 2503, payload: FF"
		reply "200100,delay 100,2502": "command: 2503, payload: 00"

	}

	// tile definitions
	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
			state "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
		}
		valueTile("power", "device.power", decoration: "flat") {
			state "default", label:'${currentValue} W'
		}
		valueTile("energy", "device.energy", decoration: "flat") {
			state "default", label:'${currentValue} kWh1'
		}
		standardTile("reset", "device.energy", inactiveLabel: false, decoration: "flat") {
			state "default", label:'reset kWh', action:"reset"
		}
		standardTile("configure", "device.power", inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}
		standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main "switch"
		details(["switch","power","energy","reset","configure","refresh"])
	}
}

def parse(String description) {
	def result = null
	def cmd = zwave.parse(description, [0x20: 1, 0x32: 1])
	if (cmd) {
		log.debug cmd
		result = createEvent(zwaveEvent(cmd))
	}
	return result
}


def zwaveEvent(physicalgraph.zwave.commands.meterv2.MeterReport cmd) {
	if (cmd.scale == 0) {
		[name: "energy", value: cmd.scaledMeterValue, unit: "kWh"]
	} else if (cmd.scale == 1) {
		[name: "energy", value: cmd.scaledMeterValue, unit: "kVAh"]
	}
	else {
    	[ name: "power", value: Math.round(cmd.scaledMeterValue), unit: "W" ]
    }

}
def zwaveEvent(physicalgraph.zwave.commands.meterv3.MeterReport cmd) {
        def map = null
        if (cmd.meterType == 1) {
                if (cmd.scale == 0) {
                        map = [name: "energy", value: cmd.scaledMeterValue, unit: "kWh"]
                } else if (cmd.scale == 1) {
                        map = [name: "energy", value: cmd.scaledMeterValue, unit: "kVAh"]
                } else if (cmd.scale == 2) {
                        map = [name: "power", value: cmd.scaledMeterValue, unit: "W"]
                } else {
                        map = [name: "electric", value: cmd.scaledMeterValue]
                        map.unit = ["pulses", "V", "A", "R/Z", ""][cmd.scale - 3]
                }
        } else if (cmd.meterType == 2) {
                map = [name: "gas", value: cmd.scaledMeterValue]
                map.unit =      ["m^3", "ft^3", "", "pulses", ""][cmd.scale]
        } else if (cmd.meterType == 3) {
                map = [name: "water", value: cmd.scaledMeterValue]
                map.unit = ["m^3", "ft^3", "gal"][cmd.scale]
        }
        if (map) {
                if (cmd.previousMeterValue && cmd.previousMeterValue != cmd.meterValue) {
                        map.descriptionText = "${device.displayName} ${map.name} is ${map.value} ${map.unit}, previous: ${cmd.scaledPreviousMeterValue}"
                }
                createEvent(map)
        } else {
                null
        }
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd)
{
	[
		name: "switch", value: cmd.value ? "on" : "off", type: "physical"
	]
}
def zwaveEvent(physicalgraph.zwave.commands.meterv1.MeterReport cmd) {
	def map = []

	if (cmd.scale == 0) {
    	map = [ name: "energy", value: cmd.scaledMeterValue, unit: "kWh" ]
    }
    else if (cmd.scale == 2) {
    	map = [ name: "power", value: Math.round(cmd.scaledMeterValue), unit: "W" ]
    }

    map
}

def zwaveEvent(int endPoint, physicalgraph.zwave.commands.meterv1.MeterReport cmd) {
	// MeterReport(deltaTime: 1368, meterType: 1, meterValue: [0, 3, 29, 17], precision: 3, previousMeterValue: [0, 3, 29, 17], rateType: 1, reserved02: false, scale: 0, scaledMeterValue: 204.049, scaledPreviousMeterValue: 204.049, size: 4)
	 log.debug "EndPoint $endPoint, MeterReport $cmd"
    def map = []

    if (cmd.scale == 0) {
    	map = [ name: "energy" + endPoint, value: cmd.scaledMeterValue, unit: "kWh" ]
    }
    else if (cmd.scale == 2) {
    	map = [ name: "power" + endPoint, value: Math.round(cmd.scaledMeterValue), unit: "W" ]
    }

    map
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd)
{
	[
		name: "switch", value: cmd.value ? "on" : "off", type: "digital"
	]
}
def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
	log.debug "Configuration Report for parameter ${cmd.parameterNumber}: Value is ${cmd.configurationValue}, Size is ${cmd.size}"
}
def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	[:]
    log.debug "Capture All $cmd"
}

def on() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	])
}

def off() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0x00).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	])
}

def poll() {
	delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.meterV2.meterGet().format(),
        zwave.meterV2.meterGet(scale:0).format(),
		zwave.meterV2.meterGet(scale:2).format()
	])
}

def refresh() {
	delayBetween([
                zwave.switchBinaryV1.switchBinaryGet().format(),
                zwave.meterV2.meterGet().format(),
                zwave.meterV2.meterGet(scale: 0).format(),      // get kWh
                zwave.meterV2.meterGet(scale: 2).format(),  // Get Watts
                zwave.basicV1.basicGet().format(),
				zwave.switchBinaryV1.switchBinaryGet().format(),

        ], 1200)
}

def reset() {
	return [
		zwave.meterV2.meterReset().format(),
		zwave.meterV2.meterGet().format()
	]
}

def configure() {
log.debug "Seting Configuration to switch"
	delayBetween([
        zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 3, size: 1).format(),   // Current Overload Protection
		zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: 4).format(),   // combined power in watts
		zwave.configurationV1.configurationSet(parameterNumber: 102, size: 4, scaledConfigurationValue: 8).format(),   // combined energy in kWh
		zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: 2).format(), // every 2 sec
		zwave.configurationV1.configurationSet(parameterNumber: 112, size: 4, scaledConfigurationValue: 120).format(), // every 2 min
        zwave.configurationV1.configurationSet(parameterNumber: 113, size: 4, scaledConfigurationValue: 120).format(), // every 2 min
		zwave.configurationV1.configurationSet(parameterNumber: 102, size: 4, scaledConfigurationValue: 0).format(),
		zwave.configurationV1.configurationSet(parameterNumber: 103, size: 4, scaledConfigurationValue: 0).format(),    // no third report
        zwave.configurationV1.configurationSet(parameterNumber: 80, size: 1, scaledConfigurationValue: 2).format(),   // Instant reporting
		zwave.configurationV1.configurationSet(parameterNumber: 113, size: 4, scaledConfigurationValue: 300).format() // every 5 min


	])
    def result = zwave.configurationV1.configurationGet(parameterNumber: 80).format()
    log.debug "Parameter 80 $result"
}