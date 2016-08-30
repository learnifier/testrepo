define([], function () {
    var exports = {};
    exports.setExpiration = function (button, listform, cmd) {
        //log('show some lightbox');

        //open modal
        require(['bootstrap/dialog', 'cocobox-datetime', 'jsrender', ], function (bd, ccbDt) {
            var content = $($('#expirationdialogTemplate').render());
            var myHidden = $("input[data-expinp=datetime]", content);
            ccbDt.dateTime(myHidden);

            var state = {};

            state.cmd = 'set';

            $('a[data-toggle="tab"]',content).on('shown.bs.tab', function (e) {
                if($(e.target).attr("aria-controls") === "exp-adjust") {
                    state.cmd = 'adjust';
                } else {
                    state.cmd = 'set';
                }

                console.log("New command", state.cmd);
            });

            bd.show({
                title: 'Change expiration',
                message: content,
                buttons: [{
                        label: 'Cancel',
                        action: function (dialog) {
                            dialog.close();
                        }
                    }, {
                        label: 'Change',
                        action: function (dialog) {
                            if (state.cmd === "set") {

                                var correctDate = myHidden.val();
                                $('#expirationdate').val(correctDate);
                                if(correctDate) {
                                    cpweb.runListCommand(button, listform, 'setExpiration');
                                }
                            } else {
                                var selectedNum = $("#adjustedDateSetter").val();
                                if (selectedNum) {
                                    $("#adjusteddate").val(selectedNum);
                                    cpweb.runListCommand(button, listform, 'adjustExpiration');
                                }
                            }
                        }
                    }]
            });
        });
    };
    return exports;
});