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
	log.debug "Refresh"
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
            def result =  resp.data//new JsonSlurper().parseText(resp.data.text)
            if(result.status == 0){
                log.debug result
                def data = result.body.activities[0]

                try{ sendEvent(name:"calories", value: data.calories as int ) }catch(err){log.error err}
                try{ sendEvent(name:"distance", value: data.distance as int ) }catch(err){}
                try{ sendEvent(name:"steps", value: data.steps as int ) }catch(err){log.error err}
                try{ sendEvent(name:"totalcalories", value: data.totalcalories as int) }catch(err){}
                try{ sendEvent(name:"active", value: data.active as int) }catch(err){}
                try{ sendEvent(name:"elevation", value: data.elevation ) }catch(err){}
                try{ sendEvent(name:"intense", value: data.intense ) }catch(err){}
                try{ sendEvent(name:"moderate", value: data.moderate ) }catch(err){}
                try{ sendEvent(name:"soft", value: data.soft ) }catch(err){}
            }else{
                log.warn "request refresh token"
                parent.refreshToken(userName)
            }
        }else{
            log.warn "request refresh token"
            parent.refreshToken(state._name)
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
