/**
 *  Withings Sleep Sensor (v.0.0.7)
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
import java.text.SimpleDateFormat
import java.util.Calendar.*

metadata {
	definition (name: "Withings Sleep Sensor", namespace: "streamorange58819", author: "fison67", mnmn:"fison67", vid: "d1fad5dd-c32d-3ae4-a77b-188a6a209e27") {
    	capability "Sleep Sensor"
        capability "streamorange58819.sleeptime"
        capability "streamorange58819.sleepstarttime"
        capability "streamorange58819.sleependtime"
        capability "streamorange58819.wakeupcount"
        capability "streamorange58819.sleeplighttime"
        capability "streamorange58819.sleepremtime"
        capability "streamorange58819.sleepdeeptime"
        capability "Refresh"		
        
        /**
        * Sleep Start Time
        */
        attribute "sleep_start_time_str", "string"
        attribute "sleep_start_time_hour", "number"
        attribute "sleep_start_time_min", "number"
        attribute "sleep_start_time_value", "number"
       
        
        /**
        * Sleep End Time
        */
        attribute "sleep_end_time_str", "string"
        attribute "sleep_end_time_hour", "number"
        attribute "sleep_end_time_min", "number"
        attribute "sleep_end_time_value", "number"
  
        
        /**
        * Sleep
        */
        attribute "sleep_duration_str", "string"
        attribute "sleep_duration_hour", "number"
        attribute "sleep_duration_min", "number"
        attribute "sleep_duration_value", "number"
        
        /**
        * Deep Sleep
        */
        attribute "sleep_duration_deep_str", "string"
        attribute "sleep_duration_deep_hour", "number"
        attribute "sleep_duration_deep_min", "number"
        attribute "sleep_duration_deep_value", "number"
        
        /**
        * Light Sleep
        */
        attribute "sleep_duration_light_str", "string"
        attribute "sleep_duration_light_hour", "number"
        attribute "sleep_duration_light_min", "number"
        attribute "sleep_duration_light_value", "number"
        
        /**
        * Rem Sleep
        */
        attribute "sleep_duation_rem_str", "string"
        attribute "sleep_duation_rem_hour", "number"
        attribute "sleep_duation_rem_min", "number"
        attribute "sleep_duation_rem_value", "number"
        
        /**
        * Not Sleep
        */
        attribute "not_sleep_duration_str", "string"
        attribute "not_sleep_duration_hour", "number"
        attribute "not_sleep_duration_min", "number"
        attribute "not_sleep_duration_value", "number"
        
        attribute "wakeup_duration_str", "string"
        attribute "wakeup_duration_hour", "number"
        attribute "wakeup_duration_min", "number"
        attribute "wakeup_duration_value", "number"
        
        attribute "wakeupcount", "number"
        
        attribute "status", "string"
        
	}

	simulator {}
    
    preferences {
        input name: "pollingTime", title:"Polling Time[Hour]" , type: "number", required: true, defaultValue: 1, description:"Polling Hour", range: "1..12"
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
	log.debug "refresh"
    currentStatus()
}

def currentStatus(){
	log.debug "schedule"
	getSleepSummaryData()
    getSleepData()
}

def getSleepData(){
   	def dateInfo = getDateMinArray(60 * 24)
    def first = dateInfo[0], end = dateInfo[1]
    
    def params = [
        uri: "https://wbsapi.withings.net/v2/sleep?action=get&startdate=${first}&enddate=${end}",
        headers:[
        	"Authorization": "Bearer ${parent.getAccountAccessToken(userName)}"
        ]
    ]
    
    httpGet(params) { resp ->
        def result =  resp.data
        log.debug result
        if(result.status == 0){
        	def list = result.body.series
        
            def lastTarget = ["startdate":1]
            list.each { item ->
                if(item.startdate > lastTarget.startdate){
                    lastTarget = item 
                }
            }
            if(lastTarget.enddate){
                def date = new Date( (long)lastTarget.enddate * 1000)
                def dateStr = date.format( 'yyyy-MM-dd hh:mm:ss', location.timeZone )

                def state = ""
                switch(lastTarget.state){
                case 0:
                    state = "awake"
                    break
                case 1:
                    state = "light_sleep"
                    break
                case 2:
                    state = "deep_sleep"
                    break
                case 3:
                    state = "rem_sleep"
                    break
                }
                log.debug "SleepData Result >> ${state} [${dateStr}]"
                if(state != ""){
                    sendEvent(name:"status", value: state )
                }
            }
            
        }
    }
}

def getSleepSummaryData(){
    def start
    def end
    def day = 1
    use (groovy.time.TimeCategory) {
      def now = new Date()
//      start = (now - day.days).format( 'yyyy-MM-dd', location.timeZone )
      end = now.format( 'yyyy-MM-dd', location.timeZone )
      start = end
    }
    
    def params = [
        uri: "https://wbsapi.withings.net/v2/sleep?action=getsummary&startdateymd=${start}&enddateymd=${end}",
        headers:[
        	"Authorization": "Bearer ${parent.getAccountAccessToken(userName)}"
        ]
    ]
    log.debug "SleepSummaryData URL >>  ${params.uri}"
    httpGet(params) { resp ->
        def result =  resp.data
        if(result.status == 0){
//            log.debug "SleepSummaryData Result >> ${result.body.series}"
            if(result.body.series.size() == 0){
            	log.warn "No Sleep Summary Data"
            	return
            }
            
    		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss"); // YYYY-MM-dd 
            sdf.setTimeZone(TimeZone.getTimeZone(result.body.series[0].timezone));
            
            def startTime, endTime, wakeupcount=0, gap=0
            def lightsleepduration=0, deepsleepduration=0, remsleepduration=0
            for(def i=0; i<result.body.series.size(); i++){
            	def data = result.body.series[i]
                log.debug data
            	if(i == 0){
                	startTime = sdf.format(new Date( ((long)data.startdate) * 1000 ))
                }else if(i == result.body.series.size() - 1){
                	endTime = sdf.format(new Date( ((long)data.enddate) * 1000 ))
                }
                if(data.data["wakeupcount"] != null){
                	wakeupcount += (data.data.wakeupcount as int)
                }
                
            	gap += (long)data.enddate - (long)data.startdate - data.data.wakeupduration - data.data.durationtowakeup - data.data.durationtosleep
                
                lightsleepduration += data.data.lightsleepduration
                deepsleepduration += data.data.deepsleepduration
                remsleepduration += data.data.remsleepduration
            }
            
            def remSleepTime = msToTime(remsleepduration)
            def remSleepTimeTmp = remSleepTime.split(":")
    		sendEvent(name:"sleep_duation_rem_str", value: remSleepTime, displayed:false )
    		sendEvent(name:"sleep_duation_rem_hour", value: remSleepTimeTmp[0] as int, displayed:false  )
    		sendEvent(name:"sleep_duation_rem_min", value: remSleepTimeTmp[1] as int, displayed:false )
    		sendEvent(name:"sleep_duation_rem_value", value: remsleepduration/60, displayed:false )
    		sendEvent(name:"sleepremtime", value: remSleepTime)
            
            def deepSleepTime = msToTime(deepsleepduration)
            def deepSleepTimeTmp = deepSleepTime.split(":")
    		sendEvent(name:"sleep_duration_deep_str", value: deepSleepTime, displayed:false )
    		sendEvent(name:"sleep_duration_deep_hour", value: deepSleepTimeTmp[0] as int, displayed:false )
    		sendEvent(name:"sleep_duration_deep_min", value: deepSleepTimeTmp[1] as int, displayed:false )
    		sendEvent(name:"sleep_duration_deep_value", value: deepsleepduration/60, displayed:false )
    		sendEvent(name:"sleepdeeptime", value: deepSleepTime)
            
            def lightSleepTime = msToTime(lightsleepduration)
            def lightSleepTimeTmp = lightSleepTime.split(":")
    		sendEvent(name:"sleep_duration_light_str", value: lightSleepTime, displayed:false )
    		sendEvent(name:"sleep_duration_light_hour", value: lightSleepTimeTmp[0] as int, displayed:false )
    		sendEvent(name:"sleep_duration_light_min", value: lightSleepTimeTmp[1] as int, displayed:false )
    		sendEvent(name:"sleep_duration_light_value", value: lightsleepduration/60, displayed:false )
    		sendEvent(name:"sleeplighttime", value: lightSleepTime)
            
            def startTimeTmp = startTime.split(":")
            def endTimeTmp = endTime.split(":")
    		sendEvent(name:"sleep_start_time_str", value: startTime, displayed:false)
    		sendEvent(name:"sleep_end_time_str", value: endTime, displayed:false)
    		sendEvent(name:"sleep_start_time_hour", value: startTimeTmp[0] as int, displayed:false)
    		sendEvent(name:"sleep_start_time_min", value: startTimeTmp[1] as int, displayed:false)
    		sendEvent(name:"sleep_end_time_hour", value: endTimeTmp[0] as int, displayed:false)
    		sendEvent(name:"sleep_end_time_min", value: endTimeTmp[1] as int, displayed:false)
            
    		sendEvent(name:"sleepstarttime", value: startTime)
    		sendEvent(name:"sleependtime", value: endTime)
    		sendEvent(name:"wakeupcount", value: wakeupcount )
    		sendEvent(name:"sleeptime", value: (gap/60) as int )
            
           /* 
            sendEvent(name: "lastMeasureDate", value: result.body.series[0].date, displayed: false)
            
            def notSleepTime = msToTime(data.wakeupduration)
            def notSleepTimeTmp = notSleepTime.split(":")
    		sendEvent(name:"not_sleep_duration_str", value: notSleepTime, displayed:false )
    		sendEvent(name:"not_sleep_duration_hour", value: notSleepTimeTmp[0] as int, displayed:false )
    		sendEvent(name:"not_sleep_duration_min", value: notSleepTimeTmp[1] as int, displayed:false )
    		sendEvent(name:"not_sleep_duration_value", value: data.wakeupduration, displayed:false )
            
            def toWakeupTime = msToTime(data.durationtowakeup)
            def toWakeupTimeTmp = toWakeupTime.split(":")
    		sendEvent(name:"wakeup_duration_str", value: toWakeupTime, displayed:false )
    		sendEvent(name:"wakeup_duration_hour", value: toWakeupTimeTmp[0] as int, displayed:false  )
    		sendEvent(name:"wakeup_duration_min", value: toWakeupTimeTmp[1] as int, displayed:false )
    		sendEvent(name:"wakeup_duration_value", value: data.durationtowakeup/60, displayed:false )
            */
        //    getBPM(result.body.series[0].startdate, result.body.series[0].enddate)
        }else{
            parent.refreshToken(userName)
            runIn(60, getSleepSummaryData)
        }
        
    }
}

def getBPM(startDate, endDate){
    def params = [
        uri: "https://wbsapi.withings.net/measure?action=getmeas&access_token=${parent.getAccountAccessToken(userName)}&meastype=11&category=1&startdate=${startDate}&enddate=${endDate}"
    ]
    httpGet(params) { resp ->
        def result =  resp.data
        log.debug result
        
        if(result.status == 0){
        }else{
            parent.refreshToken(userName)
        }
    }
}

def installed(){
    sendEvent(name:"sleeping", value: "not sleeping")
    updated()
}

def updated() {
    log.debug "updated" 
	unschedule()
    runEvery10Minutes(currentStatus)
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

def getDateMinArray(min){
 	def first
    def end
    def now = new Date()
    use (groovy.time.TimeCategory) {
        first =  (int)((now - min.minutes).getTime() / 1000)
        end =  (int)((now).getTime() / 1000)
    }
    return [first, end]
}

def msToTime(duration) {
    def seconds = (duration%60).intValue()
    def minutes = ((duration/60).intValue() % 60).intValue()
    def hours = ( (duration/(60*60)).intValue() %24).intValue()

    hours = (hours < 10) ? "0" + hours : hours
    minutes = (minutes < 10) ? "0" + minutes : minutes
    seconds = (seconds < 10) ? "0" + seconds : seconds

    return hours + ":" + minutes + ":" + seconds
}
