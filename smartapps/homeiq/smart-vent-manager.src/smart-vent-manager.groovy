/*
 *  Home IQ Smart Vent Manager
 *
 *  Copyright 2017 Cory Plock, Yves Racine
 *  Smartapp commissioned by Keen Home inc.
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *
 *  Software Distribution is restricted and shall be done only with Developer's written approval.
 */
 
definition(
	name: "${get_APP_NAME()}",
	namespace: "homeiq",
	author: "Cory Plock",
	description: "Even better control of Smart Vents",
	category: "My Apps",
	iconUrl: getHomeIQLogo(),
	iconX2Url: getLargeHomeIQLogo()
)

def get_APP_VERSION() {return "1.0"}

preferences {

	page(name: "generalSetupPage")
	page(name: "roomsSetupPage")
	page(name: "configDisplayPage")
	page(name: "NotificationsPage")
	page(name: "roomsSetup")
}


def generalSetupPage() {
	dynamicPage(name: "generalSetupPage", uninstall:true, nextPage: roomsSetupPage,refreshAfterSelection:true) {
		section("ABOUT") {
			paragraph  image:"${getHomeIQLogo()}","${get_APP_NAME()} is the SmartApp that fine tunes the temperature in rooms using Smart Vents"
			paragraph "Version ${get_APP_VERSION()}" 
			paragraph "Copyright© 2018 HomeIQ.pro"
		} /* end section about */  
                
		section("Main thermostat") {
			input (name:"thermostat", type: "capability.thermostat", title: "Which main thermostat?",required:true)
            input (name:"disableAutomation", type: "bool", title: "Disable automation?", required: false, defaultValue: false, submitOnChange: true)
		}

        section("Rooms count") {
			input (name:"roomsCount", title: "Rooms count (max=${get_MAX_ROOMS()})?", type: "number")
		}
		section {
			href(name: "toRoomPage", title: "Room Setup", page: "roomsSetupPage", description: "Tap to configure", 
				image: "${getStandardImagePath()}/vents/vent-open-icn@2x.png")
			href(name: "toNotificationsPage", title: "Notification & Other Options Setup", page: "NotificationsPage", description: "Tap to configure",
				 image: "${getCustomImagePath()}notification.png"
				)
			href(name: "toConfigDisplayPage", title: "Configuration Display", page: "configDisplayPage",
				image: "${getCustomImagePath()}configuration.jpg"
				)
		}
		section("Disable or Modify the safeguards [default=some safeguards are implemented to avoid damaging your HVAC by closing too many vents]") {
			input (name:"fullyCloseVentsFlag", title: "Bypass all safeguards & allow closing the vents totally?", type:"bool",required:false)
			input (name:"minVentLevelInZone", title: "Safeguard's Minimum Vent Level", type:"number", required: false, description: "[default=10%]")
			input (name:"maxVentTemp", title: "Safeguard's Maximum Vent Temp", type:"number", required: false, description: "[default= 131F/55C]")
			input (name:"minVentTemp", title: "Safeguard's Minimum Vent Temp", type:"number", required: false, description: "[default= 45F/7C]")
		}       
		section("What do I use for the Master on/off switch to enable/disable smartapp processing? [optional]") {
			input (name:"powerSwitch", type:"capability.switch", required: false,description: "Optional")
		}
	}
}

def roomsSetupPage() {

	dynamicPage(name: "roomsSetupPage", title: "Room Setup", uninstall: false, nextPage: NotificationsPage) {
		section("Press each room slot below to complete setup") {
			for (int i = 1; ((i <= settings.roomsCount) && (i <= get_MAX_ROOMS())); i++) {
				href(name: "toRoomPage$i", page: "roomsSetup", params: [indiceRoom: i], required:false, description: roomHrefDescription(i), title: roomHrefTitle(i), state: roomPageState(i) )
			}
		}            
		section {
			href(name: "toGeneralSetupPage", title: "Back to General Setup Page", page: "generalSetupPage")
		}
	}
}        

// @indiceRoom	room indice in settings
def roomPageState(indiceRoom) {

	if (settings."roomName${indiceRoom}" != null) {
		return 'complete'
	} else {
		return 'incomplete'
	}
  
}

// @indiceRoom	room indice in settings
def roomHrefTitle(indiceRoom) {
	def title = "Room ${indiceRoom}"
	return title
}

// @indiceRoom	room indice in settings
def roomHrefDescription(indiceRoom) {
	def description ="Room no ${indiceRoom} "

	if (settings."roomName${indiceRoom}" !=null) {
		description = settings."roomName${indiceRoom}" // CP changed from +=		    	
	}
	return description
}

// @params	parameters to the setup page
def roomsSetup(params) {
	def indiceRoom=0    

	// Assign params to indiceRoom.  Sometimes parameters are double nested.
	if (params?.indiceRoom || params?.params?.indiceRoom) {

		if (params.indiceRoom) {
			indiceRoom = params.indiceRoom
		} else {
			indiceRoom = params.params.indiceRoom
		}
	}    
 
	indiceRoom=indiceRoom.intValue()

	dynamicPage(name: "roomsSetup", title: "Rooms Setup", uninstall:false, nextPage: zonesSetupPage) {

		section("Room ${indiceRoom} Setup") {
			input "roomName${indiceRoom}", title: "Room Name", "string"
		}
        
		section("Room ${indiceRoom}-desired room temperature when heating ") {
			input "desiredHeatTemp${indiceRoom}", type:"decimal", title: "Heat setpoint [default = ${getDefaultHeatSetpoint('F')}F/${getDefaultHeatSetpoint('C')}C]", 
				required: false, defaultValue:settings."desiredHeatTemp${indiceRoom}"
		}
		section("Room ${indiceRoom}-desired room temperature when cooling") {
			input "desiredCoolTemp${indiceRoom}", type:"decimal", title: "Cool setpoint [default = ${getDefaultCoolSetpoint('F')}F/${getDefaultCoolSetpoint('C')}C]", 
				required: false, defaultValue:settings."desiredCoolTemp${indiceRoom}"
		}

        section("Room ${indiceRoom}-TempSensor [optional]") {
			input "tempSensor${indiceRoom}", title: "Temp sensor for better temp adjustment", "capability.temperatureMeasurement", 
				required: false, description: "Optional"

		}
		section("Room ${indiceRoom}-Vents Setup [optional]")  {
			for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
				input "ventSwitch${j}${indiceRoom}", title: "Vent switch no ${j} in room", "capability.switch", 
					required: false, description: "Optional"
				input "ventLevel${j}${indiceRoom}", title: "set vent no ${j}'s level in room [optional, range 0-100]", "number", 
						required: false, description: "blank:determined by smartapp"
			}           
		}      
        
		section("Room ${indiceRoom}-Motion Detection parameters [optional]") {
			input "motionSensor${indiceRoom}", title: "Motion sensor (if any) to detect if room is occupied", "capability.motionSensor", 
				required: false, description: "Optional"
			input "needOccupiedFlag${indiceRoom}", title: "Will do vent adjustement only when Occupied [default=false]", "bool",  
				required: false, description: "Optional"
			input "residentsQuietThreshold${indiceRoom}", title: "Threshold in minutes for motion detection [default=15 min]", "number", 
				required: false, description: "Optional"
		}
		section {
			href(name: "toGeneralSetupPage", title: "Back to General Setup Page", page: "generalSetupPage")
		}
	}
}


def configDisplayPage() {
	def fullyCloseVents = (settings.fullyCloseVentsFlag) ?: false 	
	float desiredRoomTemp,target_temp    
	def key 
	def scale=getTemperatureScale()     
	String bypassSafeguardsString= (fullyCloseVents)?'true':'false'                             
	float currentTempAtTstat =(scale=='C')?21:70 	// set a default value 
	String mode, operatingState
	int nbClosedVents=0, nbOpenVents=0
	def MIN_OPEN_LEVEL_IN_ZONE=(settings.minVentLevelInZone!=null)?((minVentLevelInZone>=0 && minVentLevelInZone <100)?minVentLevelInZone:10):10
	def MAX_TEMP_VENT_SWITCH = (settings.maxVentTemp)?:(scale=='C')?55:131  //Max temperature inside a ventSwitch
	def MIN_TEMP_VENT_SWITCH = (settings.minVentTemp)?:(scale=='C')?7:45   //Min temperature inside a ventSwitch

	if (settings.thermostat) { 
		currentTempAtTstat = thermostat.currentTemperature.toFloat().round(1) 
		mode = thermostat.currentThermostatMode.toString()
		operatingState=thermostat.currentThermostatOperatingState
	}         

	if (detailedNotif) {
		log.debug "configDisplayPage>About to display Current Configuration"
	}        
	dynamicPage(name: "configDisplayPage", title: "Current Configuration", nextPage: NotificationsPage,submitOnChange: true) {
       
		section("General") {
			if (settings.thermostat) {                	
				def heatingSetpoint,coolingSetpoint
				switch (mode) { 
					case 'cool':
 						coolingSetpoint = thermostat?.currentCoolingSetpoint
						target_temp= coolingSetpoint.toFloat()                        
					break                        
					case 'auto': 
 						coolingSetpoint = thermostat?.currentCoolingSetpoint
					case 'heat':
					case 'emergency heat':
					case 'auto': 
					case 'off': 
						try {                    
		 					heatingSetpoint = thermostat?.currentHeatingSetpoint
						} catch (e) {
							log.debug("ConfigDisplayPage>not able to get heatingSetpoint from $thermostat, exception $e")                      
						}                        
						heatingSetpoint=  (heatingSetpoint)? heatingSetpoint: (scale=='C')? getDefaultHeatSetpoint('C') : getDefaultHeatSetpoint('F')   
						if (mode=='auto') {                        
							float average= ((coolingSetpoint + heatingSetpoint)/2).toFloat().round(1)
							if (currentTempAtTstat > average) {
								target_temp =coolingSetpoint.toFloat()                   
							} else {
								target_temp =heatingSetpoint.toFloat()                   
							}
						} else {
							target_temp=  heatingSetpoint  
						}                        
                            
					break                        
					default:
						log.warn "ConfigDisplayPage>invalid mode $mode"
					break                        
				}                        
				paragraph " Thermostat mode: $mode\n Thermostat State: $operatingState"
				if (coolingSetpoint)  { 
					paragraph " Cooling Setpoint: ${coolingSetpoint}$scale"
				}                        
				if (heatingSetpoint)  { 
					paragraph " Heating Setpoint: ${heatingSetpoint}$scale"
				}    
			} /* if thermostat */
			paragraph " Safeguards\n  BypassSafeguards: ${bypassSafeguardsString}" +
					"\n  MinVentLevel: ${MIN_OPEN_LEVEL_IN_ZONE}%" +
					"\n  MinVentTemp: ${MIN_TEMP_VENT_SWITCH}${scale}" +
					"\n  MaxVentTemp: ${MAX_TEMP_VENT_SWITCH}${scale}" 
                    
		} /* end Section General */
		for (int indiceRoom =1; ((indiceRoom <= settings.roomsCount) && (indiceRoom <= get_MAX_ROOMS())); indiceRoom++) {
			key  = "roomName$indiceRoom"
			def roomName = settings[key]

            key  = "desiredHeatTemp$indiceRoom"
			def desiredHeatTemp = settings[key]
            
            
//			log.debug("configDisplayPage>looping thru all rooms,now room=${roomName},indiceRoom=${indiceRoom}, desiredHeatTemp=${desiredHeatTemp}") 	
			def desiredHeatRoomTemp=(desiredHeatTemp)? desiredHeatTemp.toFloat().round(1): (scale=='C')? getDefaultHeatSetpoint('C') : getDefaultHeatSetpoint('F')            

			key  = "desiredCoolTemp$indiceRoom"
			def desiredCoolTemp = settings[key]

//			log.debug("configDisplayPage>looping thru all rooms,now room=${roomName},indiceRoom=${indiceRoom}, desiredCoolTemp=${desiredCoolTemp}") 	
  			def desiredCoolRoomTemp=(desiredCoolTemp)? desiredCoolTemp.toFloat().round(1): (scale=='C') ? getDefaultCoolSetpoint('C') : getDefaultCoolSetpoint('F')            


            key = "needOccupiedFlag$indiceRoom" 
			def needOccupied = (settings[key]) ?: false 
			if (detailedNotif) {                
				log.debug("configDisplayPage>looping thru all rooms,now room=${roomName},indiceRoom=${indiceRoom}, needOccupied=${needOccupied}") 	
			}                
			key = "motionSensor${indiceRoom}" 
			def motionSensor = (settings[key])  
			key = "tempSensor${indiceRoom}" 
			def tempSensor = (settings[key])  
			def tempAtSensor =getSensorTemp(indiceRoom)			 
			if (tempAtSensor == null) { 
				tempAtSensor= currentTempAtTstat				             
			} 
			section("Room - ${roomName}") { 
				paragraph " Desired Heat Room Temp: ${desiredHeatRoomTemp}${scale}" 
				paragraph " Desired Cool Room Temp: ${desiredCoolRoomTemp}${scale}" 
				if (tempSensor) {                             
					paragraph " TempSensor: $tempSensor"  
				}                                 
				if (tempAtSensor) { 
					float temp_diff = (tempAtSensor.toFloat() - desiredCoolRoomTemp).round(1)  
					paragraph  " CurrentTempInRoom: ${tempAtSensor}${scale}"
					paragraph " Current Cool Temp OffSet: ${temp_diff.round(1)}${scale}"  

                    temp_diff = (tempAtSensor.toFloat() - desiredHeatRoomTemp).round(1)  
					paragraph  "Current Heat Temp OffSet: ${temp_diff.round(1)}${scale}"  

				}
				if (detailedNotif) {                
					log.debug("configDisplayPage>about to display motion parameters") 
				}		 
				if (motionSensor) {      
					def countActiveMotion=isRoomOccupied(motionSensor, indiceRoom)
					String needOccupiedString= (needOccupied)?'true':'false'
					if (!needOccupied) {                                
						paragraph " MotionSensor: $motionSensor" +
							"\n NeedToBeOccupied: ${needOccupiedString}" 
					} else { 
						                  
						key = "residentsQuietThreshold${indiceRoom}"
						def threshold = (settings[key]) ?: 15 // By default, the delay is 15 minutes 
						String thresholdString = threshold   
						key = "occupiedMotionTimestamp${indiceRoom}"
						def lastMotionTimestamp = (state[key])
						String lastMotionInLocalTime                                     
						def isRoomOccupiedString=(countActiveMotion)?'true':'false'                                
						if (lastMotionTimestamp) {                                    
							lastMotionInLocalTime= new Date(lastMotionTimestamp).format("yyyy-MM-dd HH:mm", location.timeZone)
						}						                                    
											                                  
						paragraph " MotionSensor: $motionSensor" +
							"\n IsRoomOccupiedNow: ${isRoomOccupiedString}" + 
							"\n NeedToBeOccupied: ${needOccupiedString}" + 
							"\n OccupiedThreshold: ${thresholdString} minutes"+ 
							"\n LastMotionTime: ${lastMotionInLocalTime}"
						                         
					}
				}                                
				paragraph "** VENTS in $roomName **" 
				for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
					key = "ventSwitch${j}$indiceRoom"
					def ventSwitch = settings[key]
					if (ventSwitch != null) {
						float tempInVent=getTemperatureInVent(ventSwitch)                            
						def switchLevel = getCurrentVentLevel(ventSwitch)							                        
						paragraph "$ventSwitch\n  CurrentVentLevel: ${switchLevel}%" +
							 "\n  CurrentVentTemp: ${tempInVent.round(1)}${scale}"
						if (switchLevel) {                                    
							// compile some stats for the dashboard                    
							if (switchLevel > MIN_OPEN_LEVEL_IN_ZONE) {
								nbOpenVents++                                    
							} else {
								nbClosedVents++                                    
							}                                        
						}
                        
					input "ventLevel${j}${indiceRoom}", title: "  override vent level[optional,range 0-100]", "number", 
						required: false, description: "  blank:determined by smartapp"
					}                                        
                            
				} /* end for switches */                             
			} /* end section rooms */
		} /* end for rooms */
	        
		section {
			href(name: "toGeneralSetupPage", title: "Back to General Setup Page", page: "generalSetupPage")
		}
	
    } /* end dynamic page */                
}



def NotificationsPage() {
	dynamicPage(name: "NotificationsPage", title: "Other Options", install: true) {
		section("Notifications") {
			input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required: false
			input "phone", "phone", title: "Send a Text Message?", required: false
		}
		section("Detailed Notifications") {
			input "detailedNotif", "bool", title: "Detailed Notifications?", required:false
		}
		section([mobileOnly: true]) {
			label title: "Assign a name for this SmartApp", required: false
		}
		section {
			href(name: "toGeneralSetupPage", title: "Back to General Setup Page", page: "generalSetupPage")
		}
	}
}



def installed() {
	initialize()
}

def updated() {
	unsubscribe()
    
    if (settings.disableAutomation == false) {
    	log.info("Initializing and enabling the automation")
        initialize()
     }
     else {
        log.info("Disabling the automation")
     }
}

def offHandler(evt) {
	log.debug "$evt.name: $evt.value"
}

def onHandler(evt) {
	log.debug "$evt.name: $evt.value"
	setZoneSettings()
}

def ventTemperatureHandler(evt) {
	if (detailedNotif) {
	log.debug "vent temperature: $evt.value"
    }
    
	float ventTemp = evt.value.toFloat()
	def scale = getTemperatureScale()
	def MAX_TEMP_VENT_SWITCH = (settings.maxVentTemp)?:(scale=='C')?55:131  //Max temperature inside a ventSwitch
	def MIN_TEMP_VENT_SWITCH = (settings.minVentTemp)?:(scale=='C')?7:45   //Min temperature inside a ventSwitch
	def currentHVACMode = thermostat?.currentThermostatMode.toString()
	currentHVACMode=(currentHVACMode)?:'auto'	// set auto by default
    
    
	if ((currentHVACMode in ['heat','auto', 'emergency heat']) && (ventTemp >= MAX_TEMP_VENT_SWITCH)) {
		if (settings.fullyCloseVentsFlag) {
			// Safeguards are not implemented as requested     
			log.warn "ventTemperatureHandler>vent temperature is not within range ($evt.value>$MAX_TEMP_VENT_SWITCH) ,but safeguards are not implemented as requested"
			return    
		}    
    
		// Open all vents just to be safe
		open_all_vents()
		send("current HVAC mode is ${currentHVACMode}, found one of the vents' value too hot (${evt.value}), opening all vents to avoid any damage")
		return        
	} /* if too hot */           
	if ((currentHVACMode in ['cool','auto']) && (ventTemp <= MIN_TEMP_VENT_SWITCH)) {
		if (settings.fullyCloseVentsFlag) {
			// Safeguards are not implemented as requested     
			log.warn "ventTemperatureHandler>vent temperature is not within range, ($evt.value<$MIN_TEMP_VENT_SWITCH) but safeguards are not implemented as requested"
			return    
		}    
		// Open all vents just to be safe
		open_all_vents()
		send("current HVAC mode is ${currentHVACMode}, found one of the vents' value too cold (${evt.value}), opening all vents to avoid any damage")
		return        
	} /* if too cold */ 
    
	setZoneSettings()
}


def thermostatOperatingHandler(evt) {
	log.info "Thermostat operating state: $evt.value"
	setZoneSettings()    
}

// Generic motion handler for all rooms 
// @indiceRoom	room indice in settings
def motionEvtHandler(evt, indiceRoom) {
	if (evt.value == "active") {
		def key= "roomName${indiceRoom}"    
		def roomName= settings[key]
		key = "occupiedMotionTimestamp${indiceRoom}"       
		state[key]= now()        
		log.debug "Motion at home in ${roomName},occupiedMotionTimestamp=${state[key]}"
		if (detailedNotif) {
			send("motion at home in ${roomName}, occupiedMotionTimestamp=${state[key]}")
		}  
	}
}


def motionEvtHandler1(evt) {
	int i=1
	motionEvtHandler(evt,i)    
}

def motionEvtHandler2(evt) {
	int i=2
	motionEvtHandler(evt,i)    
}

def motionEvtHandler3(evt) {
	int i=3
	motionEvtHandler(evt,i)    
}

def motionEvtHandler4(evt) {
	int i=4
	motionEvtHandler(evt,i)    
}

def motionEvtHandler5(evt) {
	int i=5
	motionEvtHandler(evt,i)    
}

def motionEvtHandler6(evt) {
	int i=6
	motionEvtHandler(evt,i)    
}

def motionEvtHandler7(evt) {
	int i=7
	motionEvtHandler(evt,i)    
}

def motionEvtHandler8(evt) {
	int i=8
	motionEvtHandler(evt,i)    
}

def motionEvtHandler9(evt) {
	int i=9
	motionEvtHandler(evt,i)    
}

def motionEvtHandler10(evt) {
	int i=10
	motionEvtHandler(evt,i)    
}

def motionEvtHandler11(evt) {
	int i=11
	motionEvtHandler(evt,i)    
}

def motionEvtHandler12(evt) {
	int i=12
	motionEvtHandler(evt,i)    
}

def motionEvtHandler13(evt) {
	int i=13
	motionEvtHandler(evt,i)    
}

def motionEvtHandler14(evt) {
	int i=14
	motionEvtHandler(evt,i)    
}

def motionEvtHandler15(evt) {
	int i=15
	motionEvtHandler(evt,i)    
}

def motionEvtHandler16(evt) {
	int i=16
	motionEvtHandler(evt,i)    
}


def initialize() {
	if (powerSwitch) {
		subscribe(powerSwitch, "switch.off", offHandler, [filterEvents: false])
		subscribe(powerSwitch, "switch.on", onHandler, [filterEvents: false])
	}

	subscribe(thermostat, "thermostatOperatingState", thermostatOperatingHandler)
    
	subscribe(app, appTouch)

	// subscribe all vents to check their temperature on a regular basis, and to allow zone settings
    
	for (int indiceRoom =1; ((indiceRoom <= settings.roomsCount) && (indiceRoom <= get_MAX_ROOMS())); indiceRoom++) {
		for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
			def key = "ventSwitch${j}$indiceRoom"
			def vent = settings[key]
			if (vent) {
				subscribe(vent, "temperature", ventTemperatureHandler)
			} /* end if vent != null */
		} /* end for vent switches */
	} /* end for rooms */

	// subscribe all motion sensors to check for active motion in rooms
	// Each room has its own motion handler to save last motion timestamp    	
    
	for (int i = 1;
		((i <= settings.roomsCount) && (i <= get_MAX_ROOMS())); i++) {
		def key = "motionSensor${i}"
		def motionSensor = settings[key]
        
		if (motionSensor) {
			// associate the motionHandler to the list of motionSensors in rooms   	 
			subscribe(motionSensor, "motion", "motionEvtHandler${i}", [filterEvents: false])
		}            
	}        
   

	setZoneSettings()
}



def appTouch(evt) {

	setZoneSettings()
}

def setZoneSettings() {
	def ventSwitchesZoneSet=[]


	if (powerSwitch?.currentSwitch == "off") {
		if (detailedNotif) {
			send("${powerSwitch.name} is off, schedule processing on hold...")
		}
		return
	}

	if (settings.thermostat) {
		// Check the operating State before adjusting the vents again.
		String operatingState = thermostat.currentThermostatOperatingState           
		if (operatingState?.toUpperCase() !='IDLE') {            
			if (detailedNotif) {
				log.debug "setZoneSettings>thermostat ${thermostat}'s Operating State is ${operatingState}: adjusting the vents"
			}                            
			ventSwitchesZoneSet=adjust_vent_settings()
		}                    
	}  else {
		ventSwitchesZoneSet=adjust_vent_settings()
            
	}   
	if (detailedNotif) {
		log.debug "setZoneSettings>list of Vents impacted= ${ventSwitchesZoneSet}"
	}                

}

// @sensor		motionSensor used for motion detection in room
// @indiceRoom	room indice in settings
private def isRoomOccupied(sensor, indiceRoom) {
    // If mode is Night, then consider the room occupied.
    
	if (location.mode == "Night") {
		if (detailedNotif) {
			log.debug "isRoomOccupied>room ${roomName} is considered occupied, ST hello mode ($location.mode) == Night"
		} 
		return true
    
	}    
   
	def key = "residentsQuietThreshold$indiceRoom"
	def threshold = (settings[key]) ?: 15 // By default, the delay is 15 minutes 
	key = "roomName$indiceRoom"
	def roomName = settings[key]
	def t0 = new Date(now() - (threshold * 60 * 1000))
	def recentStates = sensor.statesSince("motion", t0)
	def countActive =recentStates.count {it.value == "active"}
 	if (countActive>0) {
		if (detailedNotif) {
			log.debug "isRoomOccupied>room ${roomName} has been occupied, motion was detected at sensor ${sensor} in the last ${threshold} minutes"
		}            
		return countActive        
 	}
	return false
}

private def verify_presence_based_on_motion_in_rooms() {

	def result=false
	for (int i = 1; ((i <= (settings.roomsCount)) && (i <= get_MAX_ROOMS())); i++) {
		def key = "roomName$i"
		def roomName = settings[key]
		key = "motionSensor$i"
		def motionSensor = settings[key]
		if (motionSensor != null) {

			if (isRoomOccupied(motionSensor,i)) {
				if (detailedNotif) {
					log.debug("verify_presence_based_on_motion>in ${roomName},presence detected, return true")
				}                    
				return true
			}                
		}
	} /* end for */        
	return result
}

// @indiceRoom	room indice in settings
private def getSensorTemp(indiceRoom) {
	def key = "tempSensor$indiceRoom"
	def currentTemp=null
        
	def tempSensor = settings[key]
	if (tempSensor != null) {
		if (detailedNotif) {    
			log.debug("getTempSensor>found sensor ${tempSensor}")
		}            
		if (tempSensor.hasCapability("Refresh")) {
			// do a refresh to get the latest temp value
			try {        
				tempSensor.refresh()
			} catch (e) {
				if (detailedNotif) {
					log.debug("getSensorTemp>not able to do a refresh() on $tempSensor")
				}                    
			}                
		}        
		currentTemp = tempSensor.currentTemperature?.toFloat().round(1)
	}
	return currentTemp
}




private def adjust_vent_settings() {
	def key
	def scale= getTemperatureScale()
	def MIN_OPEN_LEVEL_IN_ZONE=(settings.minVentLevelInZone!=null)?((minVentLevelInZone>=0 && minVentLevelInZone <100)?minVentLevelInZone:10):10
	def coolingSetpoint, heatingSetpoint
	def VENT_TEMP_DIFF=(scale=='C')?2.5:5
	float desiredRoomTemp,  averageSetpoint
	boolean closedAllVentsInZone=true
	int nbVents=0
	def switchLevel    
	def ventSwitchesOnSet=[]
	def fullyCloseVents = (settings.fullyCloseVentsFlag) ?: false
	float currentTempAtTstat =(scale=='C')?21:70
	String mode='auto'

    log.info("In adjust_vent_settings")

	if (settings.thermostat) {
		currentTempAtTstat = thermostat.currentTemperature.toFloat().round(1)
 		mode = thermostat.currentThermostatMode.toString()

         try {                    
				heatingSetpoint = thermostat?.currentHeatingSetpoint
			} catch (e) {
                heatingSetpoint = (scale=='C')?22:71
				log.error("adjust_vent_settings>not able to get heatingSetpoint from $thermostat, exception $e")                      
			}                        
			try {                    
				coolingSetpoint = thermostat?.currentCoolingSetpoint
			} catch (e) {
                coolingSetpoint = (scale=='C')?24:75
				log.error("adjust_vent_settings>not able to get coolingSetpoint from $thermostat, exception $e")                      
			}                        
     }  
 

	for (int indiceRoom =1; ((indiceRoom <= settings.roomsCount) && (indiceRoom <= get_MAX_ROOMS())); indiceRoom++) {

		key = "roomName$indiceRoom"        
		def roomName = settings[key]

        key = "desiredHeatTemp$indiceRoom"
		def targetHeatRoomTemp= settings[key]
		if (!targetHeatRoomTemp) {            
			targetHeatRoomTemp = (scale=='C') ? getDefaultHeatSetpoint('C') : getDefaultHeatSetpoint('F') 			// by default, 22C/72F is the target temp
        }
		targetHeatRoomTemp = targetHeatRoomTemp.toFloat().round(1)
 
        def tStatOperatingState = thermostat.currentThermostatOperatingState
        if (!tStatOperatingState)
           tStatOperatingState = 'error'

         if (detailedNotif) {
		 log.debug("adjust_vent_settings>targetHeatRoomTemp = ${mode},room ${roomName}, targetHeatRoomTemp=${targetHeatRoomTemp}")
         }
         
         key = "desiredCoolTemp$indiceRoom"
		def targetCoolRoomTemp= settings[key]
		if (!targetCoolRoomTemp) {            
			targetCoolRoomTemp = (scale=='C') ? getDefaultCoolSetpoint('C') : getDefaultCoolSetpoint('F') 			// by default, 22C/72F is the target temp
		}
        targetCoolRoomTemp = targetCoolRoomTemp.toFloat().round(1)
		
        if (detailedNotif) {
          log.debug("adjust_vent_settings>thermostat mode = ${mode},targetCoolRoomTemp=${targetCoolRoomTemp}, room ${roomName}, targetCoolRoomTemp=${targetCoolRoomTemp}")
        }
        
        switchLevel =null	// initially set to null for check later
		key = "needOccupiedFlag$indiceRoom"
        
		def needOccupied = (settings[key]) ?: false
		if (detailedNotif) {
			log.debug("adjust_vent_settings>needOccupied, room=${roomName},indiceRoom=${indiceRoom},needOccupied=${needOccupied}")	
		}                

		if (needOccupied) {
			key = "motionSensor$indiceRoom"
			def motionSensor = settings[key]
			if (motionSensor != null) {
				if (!isRoomOccupied(motionSensor, indiceRoom)) {
					switchLevel = (fullyCloseVents)? 0: MIN_OPEN_LEVEL_IN_ZONE // setLevel at a minimum as the room is not occupied.
					if (detailedNotif) {
						log.debug("adjust_vent_settings>room = ${roomName}, not occupied,vents set to mininum level=${switchLevel}")
					}                            
				}
			}
		} 
        
		def tempAtRoomSensor =getSensorTemp(indiceRoom)			
		if (tempAtRoomSensor == null) {
			tempAtRoomSensor = currentTempAtTstat				            
		}
        tempAtRoomSensor = tempAtRoomSensor.toFloat().round(1)


		if ( settings.thermostat == null ) {
        	log.error("adjust_vent_settings>thermostat check -- thermostat not present. This is an error. Check that a thermostat is configured.")
            return ventSwitchesOnSet
        }


           // Difference between sensor versus setpoint for this particular room
		   float temp_cool_diff_at_sensor = (tempAtRoomSensor - targetCoolRoomTemp).round(1)
           float temp_heat_diff_at_sensor = (targetHeatRoomTemp - tempAtRoomSensor).round(1)
                            
           // Difference between thermostat and thermostat setpoint
           float thermostat_cool_diff =   (currentTempAtTstat - coolingSetpoint).round(1)               
           float thermostat_heat_diff =   (heatingSetpoint - currentTempAtTstat).round(1)                            

         if ((settings.thermostat) && (switchLevel==null)) {                
                            
             // Thermostat is in fan only mode.  The whole point is to circulate the air throughout the house, so open all vents
             if ( tStatOperatingState == 'fan only' || tStatOperatingState == 'idle') {

                  desiredRoomTemp = ((targetHeatRoomTemp.toFloat().round(1) + targetCoolRoomTemp.toFloat().round(1))/2).round(1)				
				  switchLevel = ((fullyCloseVents) ? 0 : MIN_OPEN_LEVEL_IN_ZONE).toInteger()   // Leave vents in minimum configuration.  We may open them later on a per vent basis.
                  
 				  // No need to be logging if there's nothing interesting going on
                  //log.info("adjust_vent_settings>Thermostat in fan or idle mode. Setting switchLevel in ${roomName} to 100")
            }
             // If the thermostat is cooling		
            else if (  tStatOperatingState != 'heating' && (mode=='cool' ||  tStatOperatingState == 'cooling' || mode=='auto') && temp_cool_diff_at_sensor > 0.0)  {
                 
                  desiredRoomTemp = targetCoolRoomTemp
                  
                  int reductionFactor = ((thermostat_cool_diff - temp_cool_diff_at_sensor) * 20.0)
                  
                  if (thermostat_cool_diff > 0.0 && temp_cool_diff_at_sensor > 0.0 && thermostat_cool_diff > temp_cool_diff_at_sensor) // long way to cool but we're almost at the setpoint in this particular room. Dampen it up.
                    { switchLevel = getMax(100 - reductionFactor, (fullyCloseVents) ? 0 : MIN_OPEN_LEVEL_IN_ZONE).toInteger() }
                  else { switchLevel = 100 }
                 
                  if (detailedNotif) {
                    log.debug("adjust_vent_settings>thermostat_cool_diff=${thermostat_cool_diff}, temp_cool_diff_at_sensor=${temp_cool_diff_at_sensor}, reduction factor ${((thermostat_cool_diff - temp_cool_diff_at_sensor) * 20.0).toInteger()}")
                  }
                    log.info("adjust_vent_settings>Temperature in ${roomName} is ${tempAtRoomSensor}. Thermostat detected as cooling with setpoint ${targetCoolRoomTemp} not yet reached by ${temp_cool_diff_at_sensor} degrees. Setting switchLevel to ${switchLevel}")
                  
             // If the thermostat is heating
            } else if ( tStatOperatingState != 'cooling' && (mode =='heat' || mode=='emergency heat' || tStatOperatingState == 'heating' || mode=='auto') && temp_heat_diff_at_sensor > 0.0) {

 				   desiredRoomTemp = targetHeatRoomTemp
                   
                   int reductionFactor = ((thermostat_heat_diff - temp_heat_diff_at_sensor) * 20.0)
                   
				   if (thermostat_heat_diff > 0.0 && temp_heat_diff_at_sensor > 0.0 && thermostat_heat_diff > temp_heat_diff_at_sensor) // Long way to heat but we're almost at the setpoint this particular room. Dampen it up.
                   { switchLevel = getMax( 100 -  reductionFactor, (fullyCloseVents) ? 0 : MIN_OPEN_LEVEL_IN_ZONE)  }
                   else { switchLevel = 100 }

                  if (detailedNotif) {
                   log.debug("adjust_vent_settings>thermostat_heat_diff=${thermostat_heat_diff}, temp_heat_diff_at_sensor=${temp_heat_diff_at_sensor}, reduction factor ${((thermostat_cool_diff - temp_cool_diff_at_sensor) * 20.0).toInteger()}")
                  }
                    log.info("adjust_vent_settings>Temperature in ${roomName} is ${tempAtRoomSensor}. Thermostat detected as heating with setpoint ${targetHeatRoomTemp} not yet reached by ${temp_heat_diff_at_sensor} degrees. Setting switchLevel to ${switchLevel}")

            // If the thermostat is doing something other than heating or cooling, close the vent
            } else {

 				  desiredRoomTemp = ((targetHeatRoomTemp.toFloat().round(1) + targetCoolRoomTemp.toFloat().round(1))/2).round(1)
				  switchLevel= (fullyCloseVents) ? 0: MIN_OPEN_LEVEL_IN_ZONE             

                  log.info("adjust_vent_settings>Temperature in ${roomName} is ${tempAtRoomSensor}. Thermostat is neither heating nor cooling or the setpoint has been reached.  Setting switchLevel to ${switchLevel} ")
            }
                        
		} /* end if thermostat */         
                
        // Go through all the vents in the room
		for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
			key = "ventSwitch${j}$indiceRoom"
			def ventSwitch = settings[key]
			def switchOverrideLevel=null
            
			if (ventSwitch != null) {
                float tempInVent=getTemperatureInVent(ventSwitch)
				nbVents++
				key = "ventLevel${j}$indiceRoom"
				switchOverrideLevel = settings[key]
                
                 if ( tStatOperatingState == 'fan only' ) {
                                   // Difference between sensor versus setpoint for this particular room
                        if ( (temp_cool_diff_at_sensor > 0.0 && tempInVent < desiredRoomTemp) || 
                             (temp_heat_diff_at_sensor > 0.0 && tempInVent > desiredRoomTemp)) {
                           switchLevel = 100
                        }                        
                        
                    log.debug("adjust_vent_settings>temp_cool_diff_at_sensor=${temp_cool_diff_at_sensor}, temp_heat_diff_at_sensor=${temp_heat_diff_at_sensor}")
                    log.debug("adjust_vent_settings>Thermostat operating state is 'fan only'. Temperature in ${roomName} is ${tempAtRoomSensor}. Temp at ${ventSwitch} is ${tempInVent}.  Desired room temp is ${desiredRoomTemp}. switchLevel set to ${switchLevel}")
                 }
                 
                
				if (switchOverrideLevel) {                
					if (detailedNotif) {
						log.debug "adjust_vent_settings>room ${roomName},set ${ventSwitch} at switchOverrideLevel =${switchOverrideLevel}%"
					}                            
					switchLevel = ((switchOverrideLevel >= 0) && (switchOverrideLevel<= 100))? switchOverrideLevel:switchLevel                     
				}                    
				if (detailedNotif) {
					log.debug("adjust_vent_settings>room ${roomName}, vent ${ventSwitch}, switchLevel to be set=${switchLevel}")
				}                        
				setVentSwitchLevel(indiceRoom, ventSwitch, switchLevel)  
				def checkSwitchLevel = getCurrentVentLevel(ventSwitch)                
				if ((checkSwitchLevel) && (checkSwitchLevel > MIN_OPEN_LEVEL_IN_ZONE)) {      // make sure that the vents are set to a minimum level, otherwise they are considered to be closed              
					ventSwitchesOnSet.add(ventSwitch)
					closedAllVentsInZone=false
				}                        
			} /* end if switchLevel !=null */                             
		} /* end for ventSwitch */
	} /* end for rooms */

//	If the user does not allow fully closed vents, then set them to a minimum level (defined as safeguard in generalSetup).
	if ((!fullyCloseVents) && (closedAllVentsInZone) && (nbVents)) {
		    	
		switchLevel= MIN_OPEN_LEVEL_IN_ZONE
		ventSwitchesOnSet=control_vent_switches_in_rooms(switchLevel)		    
		if (detailedNotif) {
			log.info("safeguards on:set all ventSwitches at ${switchLevel}% to avoid closing all of them")
		}
	}    
	return ventSwitchesOnSet    
}


private def open_all_vents() {
	def OPEN_LEVEL=100
	control_vent_switches_in_rooms(OPEN_LEVEL)
}

// @ventSwitch vent switch to be used to get temperature
private def getTemperatureInVent(ventSwitch) {
	def temp=null
	try {
		temp = ventSwitch.currentValue("temperature")       
	} catch (any) {
		log.debug("getTemperatureInVent>Not able to retreive current Temperature from ${ventSwitch}")
	}    
	return temp    
}

// @ventSwitch	vent switch to be used to get level
private def getCurrentVentLevel(ventSwitch) {
	def ventLevel=null
	try {
		ventLevel = ventSwitch.currentValue("level")     
	} catch (any) {
		log.debug("getCurrentVentLevel>Not able to current vent level from ${ventSwitch}")
	}    
	return ventLevel   
}

// @indiceRoom	room indice in settings
// @ventSwitch	vent switch to be used to get temperature
// @ventLevel	switchLevel, by default set to 100%
private def setVentSwitchLevel(indiceRoom, ventSwitch, switchLevel=100) {
	def roomName
	int MAX_LEVEL_DELTA=5
	def key
    
	if (indiceRoom) {
		key = "roomName$indiceRoom"
		roomName = settings[key]
	}
	try {
		ventSwitch.setLevel(switchLevel)
		if (roomName) {
			if (detailedNotif) {        
				log.debug("set ${ventSwitch} at level ${switchLevel} in ${roomName} to reach desired temperature")
			}                
		}            
	} catch (e) {
		if (switchLevel >0) {
			ventSwitch.off() // alternate off/on to clear potential obstruction        
			ventSwitch.on()        
			log.warn("setVentSwitchLevel>not able to set ${ventSwitch} at ${switchLevel} (exception $e), trying to turn it on")
			return false                
		} else {
			ventSwitch.on() // alternate on/off to clear potential obstruction             
			ventSwitch.off()        
			log.warn("setVentSwitchLevel>not able to set ${ventSwitch} at ${switchLevel} (exception $e), trying to turn it off")
			return false                
		}
	}    
	int currentLevel=ventSwitch.currentValue("level")    
	def currentStatus=ventSwitch.currentValue("switch")    
	if (currentStatus=="obstructed") {
		ventSwitch.off() // alternate off/on to clear obstruction        
		ventSwitch.on()  
		log.warn ("setVentSwitchLevel>error while trying to send setLevel command, switch ${ventSwitch} is obstructed")
		return false   
	}    
	if ((currentLevel < (switchLevel - MAX_LEVEL_DELTA)) ||  (currentLevel > (switchLevel + MAX_LEVEL_DELTA))) {
		log.warn( "setVentSwitchLevel>error not able to set ${ventSwitch} at ${switchLevel}")
		return false           
	}    
    
	return true    
}

// @ventLevel switchLevel, by default set to 100%
private def control_vent_switches_in_rooms(switchLevel=100) {
	def ventSwitchesOnSet=[]
	for (int indiceRoom =1; ((indiceRoom <= settings.roomsCount) && (indiceRoom <= get_MAX_ROOMS())); indiceRoom++) {
		def key = "roomName$indiceRoom"        
		def roomName = settings[key]
		if (!roomName) { // if no roomName, skip it
			continue        
		}
		for (int j = 1; (j <= get_MAX_VENTS()); j++)  {
	                
			key = "ventSwitch${j}$indiceRoom"
			def ventSwitch = settings[key]
			if (ventSwitch != null) {
				if (detailedNotif) {
					log.debug("About to set ${ventSwitch} in ${roomName}")
				}
				ventSwitchesOnSet.add(ventSwitch)
				setVentSwitchLevel(indiceRoom, ventSwitch, switchLevel)
			}
		} /* end for ventSwitch */
	} /* end for rooms */
	return ventSwitchesOnSet
}


// @msg	message to be used for notification
private send(msg) {

	def message = "${get_APP_NAME()}>${msg}"    
	if (sendPushMessage != "No") {
		sendPush(message)
	}

	if (phone) {
		log.debug("sending text message")
		sendSms(phone, message)
	}
    
	log.debug message
}

private def get_APP_NAME() {
	return "Smart Vent Manager"
}

private def get_MAX_ROOMS() {
	return 16
}


private def get_MAX_VENTS() {
	return 2
}

private def getCustomImagePath() {
    //return "https://www.homeiq.pro"
   return "http://raw.githubusercontent.com/yracine/keenhome.device-type/master/icons/"
}    

private def getStandardImagePath() {
	return "http://cdn.device-icons.smartthings.com"
}

private def getHomeIQLogo() {
   return "https://raw.githubusercontent.com/coryplock/HomeIQ/master/smartapps/homeiq/smart-vent-manager.src/HomeIQLogo.png"
}

private def getLargeHomeIQLogo() {
   return "http://static1.squarespace.com/static/59e8f429e5dd5b22729ff616/t/5a38b58d9140b787273f8700/1513665933754/HiQ17-Logo-Type-DarkBlue.png?format=100w"
}

private def getDefaultHeatSetpoint(scale)
{
   if (scale=='F')
    {  return 71.0; }
   else { return 22.0; }
}

private def getDefaultCoolSetpoint(scale)
{
   if (scale=='F')
    {  return 75.0; }
   else { return 24.0; }
}

private def getMax(n1, n2)
{
   if (n1 > n2)
    {  return n1; }
   else
    {  return n2; }
}