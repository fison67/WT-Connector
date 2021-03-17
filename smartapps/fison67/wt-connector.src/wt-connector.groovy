/**
 *  WT Connector (v.0.0.3)
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
   page(name: "addPage")
   page(name: "authUserPage")
   page(name: "devicePage")
}


def mainPage() {
	dynamicPage(name: "mainPage", title: "Withings Connector", nextPage: null, uninstall: true, install: true) {
   		section("Default Information"){
            input "client_id", "text", title: "Client ID", required: false, description:"Client ID"
            input "client_pwd", "text", title: "Client Password", required: false, description:"Client Password"
        }
        
       	section() {
            paragraph "View this SmartApp's configuration to use it in other places."
            href url:"${apiServerUrl("/api/smartapps/installations/${app.id}/config?access_token=${state.accessToken}")}", style:"embedded", required:false, title:"Config", description:"Copy wt_url"
       	}
        section("Add"){
        	href "addPage", title: "Add Page", description:""
        }
	}
}

def addPage(){
    
	state.info_access_token = ""
    state.info_refresh_token = ""
	dynamicPage(name: "addPage", title: "Add", nextPage: "authUserPage") {
   		section("Default Information"){
            input "user_name", "text", title: "User name", required: true, description:"Only English"
        }
	}
}

def getBaseURL(){
	return apiServerUrl("/api/smartapps/installations/") + app.id + "/request?access_token=${state.accessToken}"
}

def authUserPage(){
    def url = getBaseURL()
    state[settings.user_name] = ""
	state.authMode = "user"
	dynamicPage(name: "authUserPage", title: "Auth - User", nextPage: "devicePage") {
        section() {
            href url: "https://fison67.duckdns.org/wt?data=" + "https://account.withings.com/oauth2_user/authorize2?response_type=code&client_id=${client_id}&scope=user.info,user.metrics,user.activity,user.sleepevents&state=1234&redirect_uri=${url}".bytes.encodeAsBase64().toString(), style:"embedded", required:false, title:"Click Here", description:""
            paragraph "Access Token: ${state.info_access_token}"
            paragraph "Refresh Token: ${state.info_refresh_token}"
       	}
	}
}

def devicePage(){
	dynamicPage(name: "devicePage", title:"Done", nextPage: "mainPage") {
    	section ("Select") {
        	paragraph "Done"
        }
    }
}

def getUserName(){
	return settings.user_name.toLowerCase()
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
	unschedule()
    schedule("0 */12 * * * ?", takeTokenAuto)
}

def takeTokenAuto(){
	log.debug "Try to a new Access Token by Refresh Token per 12 hours."
    
    def names = []
    def list = getChildDevices()
    list.each { child ->
    	def dni = child.getDeviceNetworkId()
        if(dni.startsWith("wt-connector-person-")){
        	names.add(dni.split("-")[3])
        }
    }
    
    names.each { name ->
    	refreshToken(name)
    }
	
}

def authError() {
    [error: "Permission denied"]
}

def request(){
    if(params.code){
    	state._code = params.code
        getAccessToken()
    }
    render contentType: "text/plain", data: "Click Done"
}

def getAccessToken(){
	def url = apiServerUrl("/api/smartapps/installations/") + app.id + "/request?access_token=${state.accessToken}"
	try {
        httpPost("https://account.withings.com/oauth2/token", "action=requesttoken&grant_type=authorization_code&client_id=${client_id}&client_secret=${client_pwd}&code=${state._code}&redirect_uri=${url}") { resp ->
            processToken(resp)
        }
    } catch (e) {
        log.warn "getAccessToken >> something went wrong: $e"
    }
}

def registerNotifyListener(appli){
	def accesstoken = state.actiity_access_token
    def callbackURL = URLEncoder.encode(apiServerUrl("/api/smartapps/installations/") + app.id + "/request2?access_token=${state.accessToken}", "UTF-8")

    def params = [
        uri: "https://wbsapi.withings.net/notify?action=subscribe&access_token=${accesstoken}&callbackurl=${callbackURL}&appli=${appli}"
    ]
    httpGet(params) { resp ->
        def result =  resp.data
        log.debug result
    }
}

def refreshToken(name){
	try {
    	def param = [
        	uri: "https://account.withings.com/oauth2/token",
            body:[
            	"grant_type": "refresh_token",
                "client_id": "${settings.client_id}",
                "client_secret": "${settings.client_pwd}",
                "refresh_token": "${getAccountRefreshToken(name)}",
            ]
        ]
        httpPost(param) { resp ->
        	processRefreshToken(name, resp)
        }
    } catch (e) {
        log.warn "something went wrong: $e"
    }
}

def processToken(resp){
	log.info resp.data
	if(resp.data.scope == "user.info,user.metrics,user.activity,user.sleepevents"){
        state.info_access_token = resp.data.access_token
        state.info_refresh_token = resp.data.refresh_token
        state["${userName}_at"] = resp.data.access_token
        state["${userName}_rt"] = resp.data.refresh_token
        log.warn "${userName}_at"
        log.warn "${userName}_rt"
        getDeviceData()
    }
}

def processRefreshToken(userName, resp){
    state["${userName}_at"] = resp.data.access_token
    state["${userName}_rt"] = resp.data.refresh_token
}

def getAccountAccessToken(userName){
	return state["${userName}_at"]
}

def getAccountRefreshToken(userName){
	return state["${userName}_rt"]
}

def getDeviceData(){
    try {
        def params = [
            uri: "https://wbsapi.withings.net/v2/user?action=getdevice&access_token=${state.info_access_token}"
        ]
        
        httpGet(params) { resp ->
            def result =  resp.data
            if(result.status == 0){
            	def list = result.body.devices
                
                def userName = settings.user_name.toLowerCase()
                if(!existChild("wt-connector-person-" + userName)){
                	try{
                        def childDevice = addChildDevice("streamorange58819", "Withings Person", "wt-connector-person-${userName}", null, [
                            "label": "Withings Person ${userName}"
                        ])    
                        childDevice.setUserName(userName)
                    }catch(err){
                        log.error err
                    }
                }
                
                list.each { device ->
                    def dni = "wt-connector-${userName}-${device.deviceid}"
                    def exist = existChild(dni)
                    def dth = ""
                    if(device.type == "Sleep Monitor"){
                       dth = "Withings Sleep Sensor"
                    }else if(device.type == "Scale"){
                       dth = "Withings Scale"
                    }
                    
                    if(!exist && dth != ""){
                        try{
                            def childDevice = addChildDevice("fison67", dth, dni, location.hubs[0].id, [
                                "label": dth + " " + userName
                            ])    
                            childDevice.setID(device.deviceid)
                        	childDevice.setUserName(userName)
                     //       childDevice.updated()
                        }catch(err){
                            log.error err
                        }
                    }else{
                    	log.warn "Exist Device or Not Support DTH >> " + device.type + "(" + device.deviceid + ")"
                    //	def chlid = getChildDevice(dni)
                    }
                    
                }
                
            }else{
        		//getAccessTokenByRefreshToken("user.info")
            }
        }
    } catch (e) {
        log.warn "getDeviceData >> something went wrong: $e"
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

def nofiyData(){
    render contentType: "text/plain", data:  "Done"
}

mappings {
    if (!params.access_token || (params.access_token && params.access_token != state.accessToken)) {
        path("/config")                         { action: [GET: "authError"] }
        path("/request")                         { action: [GET: "authError"]  }
        path("/request")                         { action: [POST: "authError"]  }
        path("/notify")                         { action: [GET: "authError"]  }
        path("/notify")                         { action: [POST: "authError"]  }

    } else {
        path("/config")                         { action: [GET: "renderConfig"]  }
        path("/request")                         { action: [GET: "request"]  }
        path("/request")                         { action: [POST: "request"]  }
        path("/notify")                         { action: [GET: "nofiyData"]  }
        path("/notify")                         { action: [POST: "nofiyData"]  }
    }
}
