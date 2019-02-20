/**
 *  Withings Scale (v.0.0.1)
 *
 * MIT License
 *
 * Copyright (c) 2019 fison67@nate.com
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
 
import groovy.json.JsonSlurper
import java.text.DateFormat

metadata {
	definition (name: "Withings Scale", namespace: "fison67", author: "fison67") {
      	capability "Sensor"
        capability "Refresh"	
        capability "Temperature Measurement"		
        
        attribute "status", "number"
        attribute "weight", "number"
        attribute "fat_free_mass", "number"
        attribute "fat_ratio", "number"
        attribute "body_temperature", "number"
        attribute "skin_temperature", "number"
        
        attribute "lastCheckin", "Date"
	}


	simulator {
	}
    
    preferences {
  //      input name: "pollingTime", title:"Polling Time[Hour]" , type: "number", required: true, defaultValue: 1, description:"Polling Hour", range: "1..12"
	}

	tiles {
		multiAttributeTile(name:"status", type: "generic", width: 6, height: 4){
			tileAttribute ("device.status", key: "PRIMARY_CONTROL") {
               attributeState("status", label:'${currentValue}', backgroundColor: "#ffffff")
            }
            
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'\nLast Update: ${currentValue}')
            }
		}
        
         
        valueTile("weight_label", "", decoration: "flat", width: 3, height: 1) {
            state "default", label:'Weight'
        }   
        valueTile("weight", "device.weight", width: 3, height: 1, unit: "") {
            state("val", label:'${currentValue}', defaultState: true
            )
        }
        valueTile("fat_free_mass_label", "", decoration: "flat", width: 3, height: 1) {
            state "default", label:'Fat Free Mass'
        }    
        valueTile("fat_free_mass", "device.fat_free_mass", width: 3, height: 1, unit: "") {
            state("val", label:'${currentValue}', defaultState: true
            )
        }
        valueTile("fat_ratio_label", "", decoration: "flat", width: 3, height: 1) {
            state "default", label:'Fat Ratio'
        }    
        valueTile("fat_ratio", "device.fat_ratio", width: 3, height: 1, unit: "") {
            state("val", label:'${currentValue}', defaultState: true
            )
        }
        valueTile("body_temperature_label", "", decoration: "flat", width: 3, height: 1) {
            state "default", label:'Body Temperature'
        }    
        valueTile("body_temperature", "device.body_temperature", width: 3, height: 1, unit: "") {
            state("val", label:'${currentValue}', defaultState: true
            )
        }
        valueTile("skin_temperature_label", "", decoration: "flat", width: 3, height: 1) {
            state "default", label:'Skin Temperature'
        }    
        valueTile("skin_temperature", "device.skin_temperature", width: 3, height: 1, unit: "") {
            state("val", label:'${currentValue}', defaultState: true
            )
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh", icon:"st.secondary.refresh"
        }
        
	}

}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def setID(id){
	state._device_id = id
}

def refresh(){
	log.debug "Refresh"
    _getData()
}

def _getData(){
	def accessToken = parent.getAccountAccessToken("user.metrics")    
   	def dateInfo = getDateArray(5)
    def first = dateInfo[0], end = dateInfo[1]
    def params = [
    	uri: "https://wbsapi.withings.net/measure?action=getmeas&access_token=${accessToken}&category=1&startdate=${first}&enddate=${end}"
    ]
    log.debug "URL >> ${params}" 
    httpGet(params) { resp ->
        def result =  new JsonSlurper().parseText(resp.data.text)
        if(result.status == 0){
        	log.debug result
            
            def type1Val, type5Val, type6Val, type8Val, type12Val,type71Val, type73Val
            def type1Check = false, type5Check = false, type6Check = false, type8Check = false, type12Check = false, type71Check = false, type73Check = false
            def list = result.body.measuregrps
            list.each { item ->
            	def subList = item.measures
                subList.each { subItem ->
                    if(subItem.type == 1 && type1Check == false){
                        type1Val = (subItem.value / 100).round(2)
                        type1Check = true
                    }else if(subItem.type == 5 && type5Check == false){
                        type5Val = (subItem.value / 100).round(2)
                        type5Check = true
                    }else if(subItem.type == 6 && type6Check == false){
                        type6Val = (subItem.value / 100).round(2)
                        type6Check = true
                    }else if(subItem.type == 8 && type8Check == false){
                        type8Val = (subItem.value / 100).round(2)
                        type8Check = true
                    }else if(subItem.type == 12 && type12Check == false){
                        type12Val = (subItem.value / 100).round(2)
                        type12Check = true
                    }else if(subItem.type == 71 && type71Check == false){
                        type71Val = (subItem.value / 100).round(2)
                        type71Check = true
                    }else if(subItem.type == 73 && type73Check == false){
                        type73Val = (subItem.value / 100).round(2)
                        type73Check = true
                    }
            	}
            }
            
            log.debug "Weight >> ${type1Val}"
            log.debug "Fat Free Mass (kg) >> ${type5Val}"
            log.debug "Fat Ratio (%) >> ${type6Val}"
            log.debug "Fat Mass Weight (kg) >> ${type8Val}"
            log.debug "Temperature >> ${type12Val}"
            log.debug "Body Temperature >> ${type71Val}"
            log.debug "Skin Temperature >> ${type73Val}"
            
			
        	sendEvent(name: "status", value: type1Val)
        	sendEvent(name: "weight", value: type1Val)
        	sendEvent(name: "fat_free_mass", value: type5Val)
        	sendEvent(name: "fat_ratio", value: type6Val)
        	sendEvent(name: "temperature", value: type12Val)
        	sendEvent(name: "body_temperature", value: type71Val)
        	sendEvent(name: "skin_temperature", value: type73Val)
            
            
        }else{
        	log.debug result
            parent.getAccessTokenByRefreshToken("user.metrics")
        }
        def time = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
        sendEvent(name: "lastCheckin", value: time)
    }
    
}

def getTypeName(type){
	def name = ""
	switch(type){
    case 1:
    	name = "Weight"
    	break
    case 4:
    	name = "Height"
    	break
    case 5:
    	name = "Lean Mass"
    	break
    case 6:
    	name = "Fat Mass"
    	break
    case 7:
    	name = "Lean Mass"
    	break
    case 8:
    	name = "Fat Mass"
    	break
    case 9:
    	name = "Diastolic Blood Pressure"
    	break
    case 10:
    	name = "Systolic Blood Pressure"
    	break
    case 11:
    	name = "Heart Rate"
    	break
    case 12:
    	name = "Temperature"
    	break
    case 13:
    	name = "Humidity"
    	break
    case 15:
    	name = "Noise"
    	break
    case 18:
    	name = "Weight Objective Speed"
    	break
    case 19:
    	name = "Breastfeeding"
    	break
    case 20:
    	name = "Bottle"
    	break
    case 22:
    	name = "BMI"
    	break
    case 35:
    	name = "CO2"
    	break
    case 36:
    	name = "Steps"
    	break
    case 37:
    	name = "Elevation"
    	break
    case 38:
    	name = "Calories"
    	break
    case 39:
    	name = "Intensity"
    	break
    case 40:
    	name = "Distance"
    	break
    case 41:
    	name = "Descent"
    	break
    case 42:
    	name = "Activity Type"
    	break
    case 43:
    	name = "Duration"
    	break
    case 44:
    	name = "Sleep State"
    	break
    case 46:
    	name = "User Event"
    	break
    case 47:
    	name = "Meal Calories"
    	break
    case 48:
    	name = "Active Calories"
    	break
    case 49:
    	name = "Idle Calories"
    	break
    case 50:
    	name = "Inactive Duration"
    	break
    case 51:
    	name = "Light Activity"
    	break
    case 52:
    	name = "Moderate Activity"
    	break
    case 53:
    	name = "Intense Activity"
    	break
    case 54:
    	name = "SpO2"
    	break
    case 56:
    	name = "Ambient light"
    	break
    case 57:
    	name = "Respiratory rate"
    	break
    case 58:
    	name = "Air Quality"
    	break
    case 60:
    	name = "PIM movement"
    	break
    case 61:
    	name = "Maximum movement"
    	break
    case 66:
    	name = "Pressure"
    	break
    case 71:
    	name = "Body Temperature"
    	break
    case 72:
    	name = "GPS Speed"
    	break
    case 73:
    	name = "Skin Temperature"
    	break
    case 76:
    	name = "Muscle Mass"
    	break
    case 77:
    	name = "Water Mass"
    	break
    case 87:
    	name = "Active Calories"
    	break
    case 88:
    	name = "Bone Mass"
    	break
    case 91:
    	name = "Pulse Wave Velocity"
    	break
    case 93:
    	name = "Muscle Mass"
    	break
    case 94:
    	name = "Bone Mass"
    	break
    case 95:
    	name = "Hydration"
    	break
    case 96:
    	name = "Horizontal Radius"
    	break
    case 97:
    	name = "Altitude"
    	break
    case 98:
    	name = "Latitude"
    	break
    case 99:
    	name = "Longitude"
    	break
    }
}

def updated() {
	unschedule()
//    log.debug "Request data every ${pollingTime} hour " 
    schedule("* * * * * ?", _getData)
}

def getDateArray(day){
 	def first
    def end
    def now = new Date()
    use (groovy.time.TimeCategory) {
        first =  (int)((now - day.days).getTime() / 1000)
        end =  (int)((now + day.days).getTime() / 1000)
    }
    return [first, end]
}
