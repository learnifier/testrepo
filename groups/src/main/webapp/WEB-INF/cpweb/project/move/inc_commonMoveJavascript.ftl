[#ftl /]
[#-- Leave this line (2) here only with this comment --]

require(["[@modal.clientJavascript /]"], function (modalClient) {

        modalClient.setButtons(
            [
                {
                    text: "Cancel",
                    cssClass: "btn-cancel",
                    action: function (dlg) {
                        dlg.cancel();
                    }
                }
            ]
        );

});