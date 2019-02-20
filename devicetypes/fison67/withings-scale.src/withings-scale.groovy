/**
 *  Withings Scale (v.0.0.1)
 *
 *  Authors
 *   - fison67@nate.com
 *  Copyright 2018
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
import groovy.json.JsonSlurper
import java.text.DateFormat

metadata {
	definition (name: "Withings Scale", namespace: "fison67", author: "fison67") {
      	capability "Sensor"
        capability "Refresh"		
        
        attribute "status", "number"
        
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
    getData()
}

def getData(){
	def accessToken = parent.getAccountAccessToken("user.metrics")
    def params = [
    	uri: "https://wbsapi.withings.net/measure?action=getmeas&access_token=${accessToken}&meastype=1"
    ]
    httpGet(params) { resp ->
        def result =  new JsonSlurper().parseText(resp.data.text)
        if(result.status == 0){
        	log.debug result
        }else{
        	log.debug result
            parent.getAccessTokenByRefreshToken("user.metrics")
        }
        def time = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
        sendEvent(name: "lastCheckin", value: time)
    }
    
}

def updated() {
	unschedule()
//    log.debug "Request data every ${pollingTime} hour " 
    schedule("* * * * * ?", getData)
}

def getDateArray(day){
 	def first
    def end
    def now = new Date()
    use (groovy.time.TimeCategory) {
        first =  (int)((now - day.days).getTime() / 1000)
        end =  (int)((now + 1.days).getTime() / 1000)
    }
    return [first, end]
}
