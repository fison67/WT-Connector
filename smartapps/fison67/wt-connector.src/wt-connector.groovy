/**
 *  WT Connector (v.0.0.1)
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
 
import groovy.transform.Field
import groovy.json.*

definition(
    name: "WT Connector",
    namespace: "fison67",
    author: "fison67",
    description: "A Connector between Withings and ST",
    category: "My Apps",
    iconUrl: "https://is5-ssl.mzstatic.com/image/pf/us/r30/Purple7/v4/b1/58/1f/b1581f34-0fc0-8901-7ac7-d040699f1d2f/mzl.plcesdmu.png",
    iconX2Url: "https://is5-ssl.mzstatic.com/image/pf/us/r30/Purple7/v4/b1/58/1f/b1581f34-0fc0-8901-7ac7-d040699f1d2f/mzl.plcesdmu.png",
    iconX3Url: "https://is5-ssl.mzstatic.com/image/pf/us/r30/Purple7/v4/b1/58/1f/b1581f34-0fc0-8901-7ac7-d040699f1d2f/mzl.plcesdmu.png",
    oauth: true
)

preferences {
   page(name: "mainPage")
   page(name: "devicePage")
}


def mainPage() {
	def url = apiServerUrl("/api/smartapps/installations/") + app.id + "/request?access_token=${state.accessToken}"
	dynamicPage(name: "mainPage", title: "Withings Connector", nextPage: null, uninstall: true, install: true) {
   		section("Default Information"){
            input "client_id", "string", title: "Client ID", required: false, description:"Client ID"
            input "client_pwd", "string", title: "Client Password", required: false, description:"Client Password"
        }
        
       	section() {
            paragraph "View this SmartApp's configuration to use it in other places."
            href url:"${apiServerUrl("/api/smartapps/installations/${app.id}/config?access_token=${state.accessToken}")}", style:"embedded", required:false, title:"Config", description:"Copy wt_url"
       	}
        
        section("User Information Auth"){
        	href url:"https://account.withings.com/oauth2_user/authorize2?response_type=code&client_id=${client_id}&scope=user.info&state=1234&redirect_uri=${url}", style:"embedded", required:false, title:"Request", description:""
        }
        section("User Metrics Data Auth"){
        	href url:"https://account.withings.com/oauth2_user/authorize2?response_type=code&client_id=${client_id}&scope=user.metrics&state=1234&redirect_uri=${url}", style:"embedded", required:false, title:"Request", description:""
        }
        section("User Activity Auth"){
        	href url:"https://account.withings.com/oauth2_user/authorize2?response_type=code&client_id=${client_id}&scope=user.activity&state=1234&redirect_uri=${url}", style:"embedded", required:false, title:"Request", description:""
        }
        section() {
          	href "devicePage", title: "Device Page", description:""
       	}
	}
}

def devicePage(){
	getDeviceData()
	dynamicPage(name: "devicePage", title:"Select a Language") {
    	section ("Select") {
        	paragraph "Done"
        }
    }
}


def installed() {
    log.debug "Installed with settings: ${settings}"
    
    if (!state.accessToken) {
        createAccessToken()
    }
    
	state.dniHeaderStr = "wt-connector-"
    
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
	
    initialize()
}

def existChild(dni){
	def result = false
	def list = getChildDevices()
    list.each { child ->
        if(child.getDeviceNetworkId() == dni){
        	result = true
        }
    }
    return result
}

def initialize() {
	log.debug "initialize"
    schedule("0 */12 * * * ?", takeTokenAuto)
}

def takeTokenAuto(){
	log.debug "Try to a new Access Token by Refresh Token per 12 hours."
	unschedule()
	getAccessTokenByRefreshToken("user.info")
	getAccessTokenByRefreshToken("user.metrics")
	getAccessTokenByRefreshToken("user.activity")
}

def authError() {
	log.debug state.accessToken
    [error: "Permission denied"]
}

def request(){
    log.debug params
    if(params.access_token){
    	state._access_token = params.access_token
		log.debug "Access Token >> ${state._access_token}"
    }
    if(params.code){
    	state._code = params.code
		log.debug "Access Code >> ${state._code}"
        getAccessToken()
    }
    def configString = new groovy.json.JsonOutput().toJson("list":true)
    render contentType: "text/plain", data: configString
}

def getAccessToken(){
	def url = apiServerUrl("/api/smartapps/installations/") + app.id + "/request?access_token=${state.accessToken}"
	try {
        httpPost("https://account.withings.com/oauth2/token", "grant_type=authorization_code&client_id=${client_id}&client_secret=${client_pwd}&code=${state._code}&redirect_uri=${url}") { resp ->
            processToken(resp)
        }
    } catch (e) {
        log.debug "getAccessToken >> something went wrong: $e"
    }
}

def registerNotifyListener(appli){
	def accesstoken = state.actiity_access_token
    def callbackURL = URLEncoder.encode(apiServerUrl("/api/smartapps/installations/") + app.id + "/request2?access_token=${state.accessToken}", "UTF-8")

    def params = [
        uri: "https://wbsapi.withings.net/notify?action=subscribe&access_token=${accesstoken}&callbackurl=${callbackURL}&appli=${appli}"
    ]
    httpGet(params) { resp ->
        def result =  new JsonSlurper().parseText(resp.data.text)
        log.debug result
    }
}

def getAccessTokenByRefreshToken(type){
	def token = getTokenByType(type)
	try {
        httpPost("https://account.withings.com/oauth2/token", "grant_type=refresh_token&client_id=${client_id}&client_secret=${client_pwd}&refresh_token=${token}") { resp ->
            processToken(resp)
        }
    } catch (e) {
        log.debug "something went wrong: $e"
    }
}

def processToken(resp){
	if(resp.data.scope == "user.info"){
        state.info_access_token = resp.data.access_token
        state.info_refresh_token = resp.data.refresh_token
        log.debug "User Info >> access_token(${state.info_access_token}), refresh_token(${state.info_refresh_token})"
        getDeviceData()
    }else if(resp.data.scope == "user.metrics"){
        state.metrics_access_token = resp.data.access_token
        state.metrics_refresh_token = resp.data.refresh_token
        log.debug "User Metric >> access_token(${state.metrics_access_token}), refresh_token(${state.metrics_refresh_token})"
    }else if(resp.data.scope == "user.activity"){
        state.actiity_access_token = resp.data.access_token
        state.actiity_refresh_token = resp.data.refresh_token
        log.debug "User Activity >> access_token(${state.actiity_access_token}), refresh_token(${state.actiity_refresh_token})"
    }
}

def getAccountAccessToken(type){
	def result = ""
	switch(type){
    case "user.info":
    	result = state.actiity_access_token
    	break
    case "user.metrics":
    	result = state.metrics_access_token
    	break
    case "user.activity":
    	result = state.actiity_access_token
    	break
    }
	return result
}

def getTokenByType(type){
	def token = ""
	switch(type){
    case "user.info":
    	token = state.info_refresh_token
    	break
    case "user.metrics":
    	token = state.metrics_refresh_token
    	break
    case "user.activity":
        token = state.actiity_refresh_token
        break
    }
    return token
}

def getDeviceData(){
    try {
        def params = [
            uri: "https://wbsapi.withings.net/v2/user?action=getdevice&access_token=${state.info_access_token}"
        ]
        httpGet(params) { resp ->
            def result =  new JsonSlurper().parseText(resp.data.text)
            if(result.status == 0){
            	def list = result.body.devices
                log.debug list
                
                if(!existChild("wt-connector-person")){
                	try{
                        def childDevice = addChildDevice("fison67", "Withings Person", "wt-connector-person", location.hubs[0].id, [
                            "label": "Withings Person"
                        ])    
                        childDevice.setID("wt-connector-person")
                    }catch(err){
                        log.error err
                    }
                }
                
                list.each { device ->
                    def dni = "wt-connector-${device.deviceid}"
                    def exist = existChild(dni)
                    def dth = ""
                    if(device.type == "Sleep Monitor"){
                       dth = "Withings Sleep Sensor"; 
                    }
                    
                    if(!exist && dth != ""){
                        try{
                            def childDevice = addChildDevice("fison67", dth, dni, location.hubs[0].id, [
                                "label": dth
                            ])    
                            childDevice.setID(device.deviceid)
                            childDevice.updated()
                        }catch(err){
                            log.error err
                        }
                    }else{
                    	def chlid = getChildDevice(dni)
                    }
                    
                }
                
            }else{
        		getAccessTokenByRefreshToken("user.info")
            }
        }
    } catch (e) {
        log.debug "getDeviceData >> something went wrong: $e"
    }
}

def renderConfig() {
    def configJson = new groovy.json.JsonOutput().toJson([
        description: "WT Connector API",
        platforms: [
            [
                platform: "SmartThings WT Connector",
                name: "Witnigs Connector",
                app_url: apiServerUrl("/api/smartapps/installations/"),
                app_id: app.id,
                access_token:  state.accessToken,
                wt_url: apiServerUrl("/api/smartapps/installations/") + app.id + "/request?access_token=" + state.accessToken
            ]
        ],
    ])

    def configString = new groovy.json.JsonOutput().prettyPrint(configJson)
    render contentType: "text/plain", data: configString
}

mappings {
    if (!params.access_token || (params.access_token && params.access_token != state.accessToken)) {
        path("/config")                         { action: [GET: "authError"] }
        path("/request")                         { action: [GET: "authError"]  }
        path("/request")                         { action: [POST: "authError"]  }

    } else {
        path("/config")                         { action: [GET: "renderConfig"]  }
        path("/request")                         { action: [GET: "request"]  }
        path("/request")                         { action: [POST: "request"]  }
    }
}
