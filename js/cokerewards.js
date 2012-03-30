var URL = "https://www.mycokerewards.com/xmlrpc";

$().ready(function() {
	chooseView();
});

function chooseView() {
	if (isLoggedIn()) {
		$.mobile.changePage("#submitCode");
	} else {
		$.mobile.changePage("#logon");
	}
};

function isLoggedIn() {
	var email = localStorage.getItem("username");
	var password = localStorage.getItem("password");
	var loggedIn = localStorage.getItem("loggedIn");
	
	return loggedIn && (email !== undefined) && (email !== null) &&
						(password !== undefined) && (password !== null);
};

function recordLogin(data) {
	console.log(data);
	var res = document.evaluate("/methodResponse//member/value[../name/text()='LOGIN_RESULT']", 
				data, null, XPathResult.BOOLEAN_TYPE, null);
	console.log(res.booleanValue);
	
	if (res.booleanValue) {
		var points = document.evaluate("/methodResponse//member/value[../name/text()='POINTS']", 
				data, null, XPathResult.NUMBER_TYPE, null);	
		localStorage.setItem("points", points);
		localStorage.setItem("loggedIn", true);
	} else {
		localStorage.clear();
	}
	
	chooseView();
}

function performLogin() {
	var emailAddress = $("#username").val();
	var password = $("#password").val();
	
	localStorage.setItem("username", encode64(emailAddress));
	localStorage.setItem("password", encode64(password));
	
	getPointCount(recordLogin);
};

function submitCode() {
	var data = {};
	var code = $("#code").val();
	code = code.replace(" ", "");
	code = code.toUpperCase();
	
	if (code.length < 10) {
		showError("Code not long enough", false);
		return;
	}
	
	var emailAddress = decode64(localStorage.getItem("username"));
	var password = decode64(localStorage.getItem("password"));
	
	data.methodCall = {};
	data.methodCall.methodName = "points.enterCode";
	data.methodCall.params = {};
	data.methodCall.params.param = {};
	data.methodCall.params.param.value = {};
	data.methodCall.params.param.value.struct = {};
	data.methodCall.params.param.value.struct.member = [];
	
	var member = {};
	member.name = "emailAddress";
	member.value = {};
	member.value.string = emailAddress;
	data.methodCall.params.param.value.struct.member.push(member);
	
	member = {};
	member.name = "password";
	member.value = {};
	member.value.string = password;
	data.methodCall.params.param.value.struct.member.push(member);
	
	member = {};
	member.name = "screenName";
	member.value = {};
	member.value.string = "";
	data.methodCall.params.param.value.struct.member.push(member);
	
	member = {};
	member.name = "capCode";
	member.value = {};
	member.value.string = code;
	data.methodCall.params.param.value.struct.member.push(member);
	
	member = {};
	member.name = "VERSION";
	member.value = {};
	member.value.string = "4.1";
	data.methodCall.params.param.value.struct.member.push(member);
	
	var result = json2xml(data, 0);
	console.log(result);
	
	$.ajax({ 
			url: URL,
			dataType: 'xml',
			data: result,
			type: "POST",
			crossDomain: true,
			success: function(data) {
				console.log(data);
				
				var res = document.evaluate("/methodResponse//member/value[../name/text()='ENTER_CODE_RESULT']",
								data, null, XPathResult.BOOLEAN_TYPE, null);
				
				var messages = document.evaluate("/methodResponse//member/value[../name/text()='MESSAGES']",
								data, null, XPathResult.STRING_TYPE, null);
				
				if (messages.stringValue.length > 0) {
					console.log(messages);
					showError(messages.stringValue, false);
				}
				
				chooseView();
			}
	});
};

function getPointCount(successFunction) {
	var data = {};

	var emailAddress = decode64(localStorage.getItem("username"));
	var password = decode64(localStorage.getItem("password"));

	data.methodCall = {};
	data.methodCall.methodName = "points.pointsBalance";
	
	data.methodCall.params = {};
	data.methodCall.params.param = {};
	data.methodCall.params.param.value = {};
	data.methodCall.params.param.value.struct = {};
	data.methodCall.params.param.value.struct.member = [];
	
	var member = {};
	member.name = "emailAddress";
	member.value = {};
	member.value.string = emailAddress;
	
	data.methodCall.params.param.value.struct.member.push(member);

	member = {};
	member.name = "password";
	member.value = {};
	member.value.string = password;
	
	data.methodCall.params.param.value.struct.member.push(member);
	
	member = {};
	member.name = "screenName";
	member.value = {};
	member.value.string = "";
	
	data.methodCall.params.param.value.struct.member.push(member);
	
	member = {};
	member.name = "VERSION";
	member.value = {};
	member.value.string = "4.1";
	
	data.methodCall.params.param.value.struct.member.push(member);
	
	var result = json2xml(data, 0);
	console.log(result);
	
	$.ajax({
		url: URL,
		dataType: 'xml',
		data: result,
		type: "POST",
		crossDomain: true,
		success: successFunction
	});
};

function showError(msg, clearTextBox) {
	if (clearTextBox) {
		$("#code").text("");
	}
	
	$("#errorMsg").html(msg);
};
