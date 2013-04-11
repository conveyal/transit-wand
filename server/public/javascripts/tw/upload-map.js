

var uploadMap;

// dynamic height management

$(document).ready(sizeContent);
$(window).resize(sizeContent);

function sizeContent() {
  var newHeight = $(window).height() - $("#header").height() + "px";
  $("#map").css("height", newHeight);
}

function loadPattern() {

	$.get('/pattern', {patternId: $('#patternSelector').val()}, function(data){

		uploadMap.update(data);

	});

}

$(document).ready(function() {
  
	uploadMap = new UploadMapView({unitId: unitId});

	$('#patternSelector').change(function() {
		loadPattern();

	});

	loadPattern();

});


