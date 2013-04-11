

var UploadMapView = Backbone.View.extend({

	initialize: function ( opts ) {
      
      var view = this;

      // create/bind collection if not set via opts
      //if(opts.collection == undefined)
      //    this.collection = new UploadCollection({success: this.onCollectionUpdate});

      this.unitId = opts.unitId;

      this.stopTemplate = Handlebars.compile( $("#stop-popup-tpl").html());

      this.patternDetailsTemplate = Handlebars.compile( $("#pattern-details-tpl").html());
      
      // Event bindings for the taxi collection
      //this.collection.on('add', this.onModelAdd, this);
      //this.collection.on('reset', this.onCollectionReset, this);
      //this.collection.on('remove', this.onModelRemove, this);
      //this.collection.on('change', this.onModelChange, this);

      // Custom icons



      this.stopIcon = L.icon({
          iconUrl: '/public/images/icons/stop_icon.png',
          iconSize: [32, 37],
          iconAnchor: [16, 37],
          popupAnchor: [0, -37],
          labelAnchor: [10, -16]
        });
    

    
     // Base layer config is optional, default to Mapbox Streets
      var url = 'http://{s}.tiles.mapbox.com/v3/openplans.map-ky03eiac/{z}/{x}/{y}.png';
          baseLayer = L.tileLayer(url, {
            attribution: '&copy; OpenStreetMap contributors, CC-BY-SA. <a href="http://mapbox.com/about/maps" target="_blank">Terms &amp; Feedback</a>' 
          });

      // Init the map
      this.map = L.map($('#map').get(0), {
        center: defaultLatLon, //TODO: add to the config file for now
        zoom: 15,
        maxZoom: 17
      });

      this.map.addLayer(baseLayer);

      this.stopLayer = L.layerGroup().addTo(this.map);

      // Remove default prefix
      this.map.attributionControl.setPrefix('');

      this.map.on('contextmenu', this.addIncident, this);

    },

    update: function(data) {
        
        if(this.polyline != undefined)
          this.map.removeLayer(this.polyline);

        $('#patternDetails').html(this.patternDetailsTemplate(data));
        
        this.polyline = L.Polyline.fromEncoded(data.shape).addTo(this.map);
        this.map.fitBounds(this.polyline.getBounds());

        this.stopLayer.clearLayers();
        for(var s in data.stops) {
          var marker = L.marker([data.stops[s].lat, data.stops[s].lon]);
  
          var remainder = data.stops[s].travelTime % 60;
          var stopData = {lat: data.stops[s].lat, lon: data.stops[s].lon, remainder: remainder, minutes: (data.stops[s].travelTime - remainder) / 60, stopSequence:  parseInt(s) + 1, board: data.stops[s].board, alight: data.stops[s].alight};

          marker.bindPopup(this.stopTemplate(stopData));
          this.stopLayer.addLayer(marker);

        }

    }

   
 });  