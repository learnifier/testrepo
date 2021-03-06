<script id="roster-expand-template" type="text/x-handlebars-template">

<div>
{{#pages}}
  <div>
    <span class="pull-right" style="font-size: 0.8em; color: #999;">Activity/Quiz Completion</span>
    <h5 class="page-title">{{title}}</h5>
  </div>
    <div class="table-responsive">
        <table class="table table-hover ccb-table ToggleDrop" width="100%" id="rosterDetails">
            <tbody>
                {{#activity}}
                  <tr class="{{activityStatus}}">
                      <td class="activityTitle">
                        <span class="truncate navColor">{{title}}</span>
                      </td>
                      <td class="activityStatusToggle">

                      </td>
                      <td class="completionDate">
                          {{completedDateStr}}
                      </td>
                      <td class="statusName">
                          <span class="pull-right">{{StatusName activityStatus}}</span>
                      </td>
                      <td class="statusImage">
                          <span class="pull-right">{{StatusImage activityStatus}}</span>
                      </td>
                  </tr>
                   {{#component}}
                      <tr class="hide white {{componentStatus}}">
                           <td class="ptb0" colspan="8">
                               <div>
                                      <ul class="dropdownUl clearfix">
                                           <!-- Stacking from right from here -->
                                          <li class="statusImage pull-right">
                                              {{StatusImage componentStatus}}
                                          </li>
                                          <li class="statusName pull-right">
                                              {{StatusName componentStatus}}
                                          </li>
                                          <li class="completionDate pull-right">
                                              {{#if completedDate}}
                                                  <abbr data-toggle="tooltip" class="timeago" title="{{completedDateAgo}}"> {{completedDateStr}} </abbr>
                                              {{/if}}
                                          </li>
                                          <li class="score pull-right">
                                              {{#if scoreStr}}
                                                  <span class="row-label"> Quiz Score:</span><span class="row-value">{{scoreStr}}</span>
                                              {{/if}}
                                          </li>
                                          <!-- Removing as duplicate info as passed/failed now is part of extendedStatus
                                          <li class="successStatus pull-right">
                                              {{#if completed}}
                                                {{#if successStatus }}
                                                    <span class="row-label">Quiz/test:</span><span class="row-value">{{successStatus}}</span>
                                                {{/if}}
                                              {{/if}}
                                          </li> -->
                                           <!-- Stacking from left from here -->
                                          <li class="componentThumbnail pull-left">
                                              <img src="{{thumbnail}}">
                                          </li>
                                          <li class="componentTitle pull-left">
                                              <span class="truncate">{{componentTitle}}</span>
                                          </li>
                                      </ul>
                               </div>
                          </td>
                      </tr>
                  {{/component}}
                {{/activity}}
            </tbody>
        </table>
      </div>
{{/pages}}
</div>

</script>
