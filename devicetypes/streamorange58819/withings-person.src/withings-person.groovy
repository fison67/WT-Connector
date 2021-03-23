/**
 *  Withings Person (v.0.0.3)
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
	definition (name: "Withings Person", namespace: "streamorange58819", mnmn: "fison67", author: "fison67", vid:"d292b02f-4fa1-34b0-b82e-07755e171e83", ocfDeviceType:"x.com.st.d.healthtracker") {
		capability "streamorange58819.totalcalories"
		capability "streamorange58819.calories"
		capability "streamorange58819.steps"
		capability "streamorange58819.active"
		capability "streamorange58819.distance"
      	capability "Sensor"
        capability "Refresh"		
        
        attribute "elevation", 	"number"
        attribute "intense", 	"number"
        attribute "moderate", 	"number"
        attribute "soft", 		"number"
	}

	simulator {}
    preferences {}
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
	log.debug "Refresh Token: ${parent.getAccountAccessToken(userName)}"
    getUserHealth()
}

def getUserHealth(){
	log.debug "Get User Health [${userName}]"
    def start = new Date().format('yyyy-MM-dd', location.timeZone )
    def params = [
    	"uri": "https://wbsapi.withings.net/v2/measure",
        "headers": [
            "Authorization": "Bearer ${parent.getAccountAccessToken(userName)}",
            "Content-Type": "application/x-www-form-urlencoded"
        ],
        "body":[
            "action": "getactivity",
            "startdateymd": start, 
            "enddateymd": start
        ]
    ]
    
    httpPost(params) { resp ->
    	if(resp.status == 200){
            def result =  resp.data
            
            if(result.status == 0){
				def calories=0, distance=0, steps=0, totalcalories=0, active=0, elevation=0, intense=0, moderate=0, soft=0
                for(def i=0; i<result.body.activities.size(); i++){
                	def data = result.body.activities[i]
                    log.debug data
                	calories += data.calories as int
                	distance += data.distance as int
                	steps += data.steps as int
                	totalcalories += data.totalcalories as int
                	active += data.active as int
                	elevation += data.elevation as int
                	intense += data.intense as int
                	moderate += data.moderate as int
                	soft += data.soft as int
                }
                
                sendEvent(name:"calories", value:  calories)
                sendEvent(name:"distance", value: distance)
                sendEvent(name:"steps", value: steps)
                sendEvent(name:"totalcalories", value: totalcalories)
                sendEvent(name:"active", value: active)
                sendEvent(name:"elevation", value: elevation)
				sendEvent(name:"intense", value: intense)
				sendEvent(name:"moderate", value: moderate)
                sendEvent(name:"soft", value: data.soft)
            }else{
                log.warn "request refresh token"
                parent.refreshToken(userName)
            }
        }else{
            log.warn "request refresh token"
            parent.refreshToken(userName)
        }
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
