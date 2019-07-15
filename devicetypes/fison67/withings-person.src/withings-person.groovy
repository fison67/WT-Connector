/**
 *  Withings Person (v.0.0.2)
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
	definition (name: "Withings Person", namespace: "fison67", author: "fison67") {
      	capability "Sensor"
        capability "Refresh"		
        
        attribute "status", "number"
        
        attribute "calories", "number"
        attribute "distance", "number"
        attribute "elevation", "number"
        attribute "intense", "number"
        attribute "moderate", "number"
        attribute "soft", "number"
        attribute "steps", "number"
        attribute "totalcalories", "number"
        
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
        
        valueTile("steps_label", "", decoration: "flat", width: 3, height: 1) {
            state "default", label:'Steps'
        }   
        valueTile("steps", "device.steps", width: 3, height: 1, unit: "") {
            state("val", label:'${currentValue}', defaultState: true
            )
        }
        valueTile("calories_label", "", decoration: "flat", width: 3, height: 1) {
            state "default", label:'Calories'
        }    
        valueTile("calories", "device.calories", width: 3, height: 1, unit: "") {
            state("val", label:'${currentValue}', defaultState: true
            )
        }
        valueTile("totalcalories_label", "", decoration: "flat", width: 3, height: 1) {
            state "default", label:'Total Calories'
        }    
        valueTile("totalcalories", "device.totalcalories", width: 3, height: 1, unit: "") {
            state("val", label:'${currentValue}', defaultState: true
            )
        }
        valueTile("distance_label", "", decoration: "flat", width: 3, height: 1) {
            state "default", label:'Distance'
        }  
        valueTile("distance", "device.distance", width: 3, height: 1, unit: "") {
            state("val", label:'${currentValue}', defaultState: true
            )
        }
        valueTile("elevation_label", "", decoration: "flat", width: 3, height: 1) {
            state "default", label:'Elevation'
        }  
        valueTile("elevation", "device.elevation", width: 3, height: 1, unit: "") {
            state("val", label:'${currentValue}', defaultState: true
            )
        }
        valueTile("intense_label", "", decoration: "flat", width: 3, height: 1) {
            state "default", label:'Intense'
        }  
        valueTile("intense", "device.intense", width: 3, height: 1, unit: "") {
            state("val", label:'${currentValue}', defaultState: true
            )
        }
        valueTile("moderate_label", "", decoration: "flat", width: 3, height: 1) {
            state "default", label:'Moderate'
        }  
        valueTile("moderate", "device.moderate", width: 3, height: 1, unit: "") {
            state("val", label:'${currentValue}', defaultState: true
            )
        }
        valueTile("soft_label", "", decoration: "flat", width: 3, height: 1) {
            state "default", label:'Soft'
        }  
        valueTile("soft", "device.soft", width: 3, height: 1, unit: "") {
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

def setUserName(name){
	state._name = name
}

def getUserName(){
	return state._name
}

def refresh(){
	log.debug "Refresh"
    getUserHealth()
}

def getUserHealth(){
	log.debug "Get User Health"
	def accessToken = parent.getAccountAccessToken(state._name, "user.activity")
    def now = new Date()
    def start = now.format( 'yyyy-MM-dd', location.timeZone )
    def end = start
    def params = [
        uri: "https://wbsapi.withings.net/v2/measure?action=getactivity&access_token=${accessToken}&startdateymd=${start}&enddateymd=${end}"
    ]
    httpGet(params) { resp ->
        def result =  new JsonSlurper().parseText(resp.data.text)
        if(result.status == 0){
        	log.debug result
        	def data = result.body.activities[0]
            
    		try{ sendEvent(name:"status", value: data.steps ) }catch(err){}
           
    		try{ sendEvent(name:"calories", value: data.calories as int ) }catch(err){}
    		try{ sendEvent(name:"distance", value: data.distance as int ) }catch(err){}
    		try{ sendEvent(name:"elevation", value: data.elevation ) }catch(err){}
    		try{ sendEvent(name:"intense", value: data.intense ) }catch(err){}
    		try{ sendEvent(name:"moderate", value: data.moderate ) }catch(err){}
    		try{ sendEvent(name:"soft", value: data.soft ) }catch(err){}
    		try{ sendEvent(name:"steps", value: data.steps ) }catch(err){}
    		try{ sendEvent(name:"totalcalories", value: data.totalcalories as int) }catch(err){}
        }else{
        	log.debug result
            parent.getAccessTokenByRefreshToken(state._name, "user.activity")
        }
        def time = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
        sendEvent(name: "lastCheckin", value: time)
    }
    
}

def updated() {
	unschedule()
//    log.debug "Request data every ${pollingTime} hour " 
    schedule("* * * * * ?", getUserHealth)
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
