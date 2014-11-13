var argscheck = require('cordova/argscheck'),
	    channel = require('cordova/channel'),
	    utils = require('cordova/utils'),
	    exec = require('cordova/exec'),
	    cordova = require('cordova');
	
Analytics.prototype.log = function(successCallback, errorCallback) {
	exec(successCallback, errorCallback, "Analytics", "log", []);
};
	
module.exports = new Analytics();