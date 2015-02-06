<script id="expirationdialogTemplate" type="text/x-jsrender"> 
        <div role="tabpanel">

  <!-- Nav tabs -->
  <ul class="nav nav-tabs" role="tablist">
    <li role="presentation" class="active"><a href="#exp-set" aria-controls="exp-set" role="tab" data-toggle="tab">Set expiration</a></li>
    <li role="presentation"><a href="#exp-adjust" aria-controls="exp-adjust" role="tab" data-toggle="tab">Adjust expiration</a></li>
  </ul>

  <!-- Tab panes -->
  <div class="tab-content">
    <div role="tabpanel" class="tab-pane active" id="exp-set">
        <p>Select a date for when the participation(s) will expire.</p>
        <input data-expinp='datetime' type="hidden" />
    </div>
    <div role="tabpanel" class="tab-pane" id="exp-adjust">
        <p>Adjust the expiration by setting an offset value. The number of days will be added or withdrawn from the current expiration day.</p>
        <p>for example -7 days to shorten with a week</p>

        <input id="adjustedDateSetter" type="number" class="form-control"/>
    </div>

  </div>

</div>

</script>