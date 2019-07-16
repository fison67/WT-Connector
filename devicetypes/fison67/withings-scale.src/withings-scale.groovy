/**
 *  Withings Scale (v.0.0.2)
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
import groovy.transform.Field


@Field 
LANGUAGE_MAP = [
	"last_measure_date": [
    	"Korean": "측정시간",
        "English": "Measure Date"
    ],
    "weight": [
        "Korean": "몸무게",
        "English": "Weight"
    ],
    "fat_free_mass": [
        "Korean": "제지방량",
        "English": "Fat Free Mass"
    ],
    "fat_ratio": [
        "Korean": "체지방율",
        "English": "Fat Ratio"
    ],
    "fat_mass_weight": [
        "Korean": "체지방량",
        "English": "Fat Mass Weight"
    ],
    "heart_rate": [
        "Korean": "심박수",
        "English": "Heart Rate"
    ]
]


metadata {
	definition (name: "Withings Scale", namespace: "fison67", author: "fison67") {
      	capability "Sensor"
        capability "Refresh"	
        capability "Temperature Measurement"		
        
        attribute "status", "number"
        attribute "weight", "number"
        attribute "fat_free_mass", "number"
        attribute "fat_ratio", "number"
        attribute "fat_mass_weight", "number"
        attribute "heart_rate", "number"
        
        attribute "body_temperature", "number"
        attribute "skin_temperature", "number"
        attribute "blood_pressure_max", "number"
        attribute "blood_pressure_min", "number"
        
        attribute "lastCheckin", "Date"
        attribute "lastMeasureDate", "Date"
	}


	simulator {
	}
    
    preferences {
        input name: "language", title:"Select a language" , type: "enum", required: true, options: ["English", "Korean"], defaultValue: "English", description:"Language for DTH"
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
        
        valueTile("last_measure_date_label", "last_measure_date_label", decoration: "flat", width: 3, height: 1) {
            state "default", label:'${currentValue}'
        }   
        valueTile("lastMeasureDate", "device.lastMeasureDate", width: 3, height: 1, unit: "") {
            state("val", label:'${currentValue}', defaultState: true
            )
        }
        valueTile("weight_label", "weight_label", decoration: "flat", width: 3, height: 1) {
            state "default", label:'${currentValue}'
        }   
        valueTile("weight", "device.weight", width: 3, height: 1, unit: "kg") {
            state("val", label:'${currentValue} kg', defaultState: true
            )
        }
        valueTile("fat_free_mass_label", "fat_free_mass_label", decoration: "flat", width: 3, height: 1) {
            state "default", label:'${currentValue}'
        }    
        valueTile("fat_free_mass", "device.fat_free_mass", width: 3, height: 1, unit: "kg") {
            state("val", label:'${currentValue} kg', defaultState: true
            )
        }
        valueTile("fat_ratio_label", "fat_ratio_label", decoration: "flat", width: 3, height: 1) {
            state "default", label:'${currentValue}'
        }    
        valueTile("fat_ratio", "device.fat_ratio", width: 3, height: 1, unit: "%") {
            state("val", label:'${currentValue} %', defaultState: true
            )
        }
        valueTile("fat_mass_weight_label", "fat_mass_weight_label", decoration: "flat", width: 3, height: 1) {
            state "default", label:'${currentValue}'
        }    
        valueTile("fat_mass_weight", "device.fat_mass_weight", width: 3, height: 1, unit: "kg") {
            state("val", label:'${currentValue} kg', defaultState: true
            )
        }
        valueTile("heart_rate_label", "heart_rate_label", decoration: "flat", width: 3, height: 1) {
            state "default", label:'${currentValue}'
        }    
        valueTile("heart_rate", "device.heart_rate", width: 3, height: 1, unit: "bpm") {
            state("val", label:'${currentValue} bpm', defaultState: true
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

def setUserName(name){
	state._name = name
}

def getUserName(){
	return state._name
}

def refresh(){
	log.debug "Refresh"
    _getData()
}

def _getData(){
	def accessToken = parent.getAccountAccessToken(state._name, "user.metrics")    
   	def dateInfo = getDateArray(5)
    def first = dateInfo[0], end = dateInfo[1]
    def params = [
    	uri: "https://wbsapi.withings.net/measure?action=getmeas&category=1&startdate=${first}&enddate=${end}",
        headers:[
        	"Authorization": "Bearer ${accessToken}"
        ]
    ]
    httpGet(params) { resp ->
        def result =  new JsonSlurper().parseText(resp.data.text)
        if(result.status == 0){
        //	log.debug result
            
            def type1Check = false,  type4Check = false, type5Check = false, type6Check = false, type8Check = false, type9Check = false, type10Check = false, type11Check = false, type12Check = false, type71Check = false, type73Check = false
            def list = result.body.measuregrps
            list.each { item ->
            //	def date = item.date
                
            	def subList = item.measures
                subList.each { subItem ->
                	if(item.deviceid == state._device_id){
                        def unitVal = 1
                        def _tmp = subItem.unit
                        while(_tmp < 0){
                        	_tmp++
                            unitVal = unitVal * 10
                        }
                        
                    	if(subItem.type == 1 && type1Check == false){
                            sendEvent(name: "status", value: (subItem.value / unitVal))
                            sendEvent(name: "weight", value: (subItem.value / unitVal))
                            log.debug "Weight >> " + (subItem.value / unitVal)
                            
    						def time = new Date( ((long)item.date) * 1000 ).format("yyyy-MM-dd HH:mm:ss", location.timeZone)
                            sendEvent(name: "lastMeasureDate", value: time, displayed: false)
                            type1Check = true
                        }else if(subItem.type == 4 && type4Check == false){
                            sendEvent(name: "height", value: subItem.value / unitVal)
                            log.debug "Height >> " + (subItem.value / unitVal)
                            type4Check = true
                        }else if(subItem.type == 5 && type5Check == false){
                            sendEvent(name: "fat_free_mass", value: (subItem.value / unitVal))
                            log.debug "Fat Free Mass >> " + (subItem.value / unitVal)
                            type5Check = true
                        }else if(subItem.type == 6 && type6Check == false){
                            sendEvent(name: "fat_ratio", value: (subItem.value / unitVal))
                            log.debug "Fat Ratio >> " + (subItem.value / unitVal)
                            type6Check = true
                        }else if(subItem.type == 8 && type8Check == false){
                            sendEvent(name: "fat_mass_weight", value: (subItem.value / unitVal))
                            log.debug "Fat Mass Weight >> " + (subItem.value / unitVal)
                            type8Check = true
                        }else if(subItem.type == 9 && type9Check == false){
                            sendEvent(name: "blood_pressure_max", value: (subItem.value / 1000))
                            log.debug "Blood Pressure Max >> " + (subItem.value / 1000)
                            type9Check = true
                        }else if(subItem.type == 10 && type10Check == false){
                            sendEvent(name: "blood_pressure_min", value: (subItem.value / unitVal))
                            log.debug "Blood Pressure Min >> " + (subItem.value / unitVal)
                            type10Check = true
                        }else if(subItem.type == 11 && type11Check == false){
                            sendEvent(name: "heart_rate", value: (subItem.value / unitVal ))
                            log.debug "Heart Rate >> " + (subItem.value / unitVal)
                            type11Check = true
                        }else if(subItem.type == 12 && type12Check == false){
                            sendEvent(name: "temperature", value: (subItem.value / unitVal))
                            log.debug "Temperature >> " + (subItem.value / unitVal)
                            type12Check = true
                        }else if(subItem.type == 71 && type71Check == false){
                            sendEvent(name: "body_temperature", value: (subItem.value / unitVal))
                            log.debug "Body Temperature >> " + (subItem.value / unitVal)
                            type71Check = true
                        }else if(subItem.type == 73 && type73Check == false){
                            sendEvent(name: "skin_temperature", value: (subItem.value / unitVal))
                            log.debug "Skin Temperature >> " + (subItem.value / unitVal)
                            type73Check = true
                        }
                    }
            	}
            }
        }else{
        	log.debug result
            parent.getAccessTokenByRefreshToken(state._name, "user.metrics")
        }
        def time = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
        sendEvent(name: "lastCheckin", value: time, displayed: false)
    }
    
}

def updated() {
	setLanguage()
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


def setLanguage(){
    log.debug "Languge >> ${language}"
	
    sendEvent(name:"last_measure_date_label", value: LANGUAGE_MAP["last_measure_date"][language] )
    sendEvent(name:"weight_label", value: LANGUAGE_MAP["weight"][language] )
    sendEvent(name:"fat_free_mass_label", value: LANGUAGE_MAP["fat_free_mass"][language] )
    sendEvent(name:"fat_ratio_label", value: LANGUAGE_MAP["fat_ratio"][language] )
	sendEvent(name:"fat_mass_weight_label", value: LANGUAGE_MAP["fat_mass_weight"][language] )
	sendEvent(name:"heart_rate_label", value: LANGUAGE_MAP["heart_rate"][language] )
}
