"use strict";
var cpweb = cpweb || {};

// Only submit search query if search string is not empty

$(function() {
    $('#q_btn').click(function() {
        if ( $('#q').val() == '' ) {
            return false;
        } else {
            return true;
        }

    });
});

cpweb.registerListform = function(listform) {
    $(listform).data('selectedIds',[]);
}

cpweb.setListCommand = function(listform, cmd) {
    $(listform).find("input[name=__command]").val(cmd);

    return;
}

cpweb.projectRosterListForm = function() {
    require(['dabox-jquery', 'dabox-common'], function() {
        cocobox.longOp();
        $("button", this).cocobox('inputBlock');
    });
    return true;
}

cpweb.runListCommand = function (button, listform, cmd) {
    require(['dabox-jquery'], function() {
        $(button).cocobox('inputBlock');
        cpweb.setListCommand(listform, cmd);
        $(listform).submit();
    });
}

cpweb.rosterDelete = function (button, listform, cmd, title, text) {
    require(['dabox-common'], function() {
        cocobox.confirmationDialog(title,
        text,
        function() {
            cpweb.runListCommand(button, listform, cmd);
        });
    });
    //Block the click
    return false;
};

cpweb.setExpiration = function(button, listform, cmd) {
    //log('show some lightbox');
    
    
    //open modal
    require(['dabox-jquery', 'jsrender'], function() {
        $('body').append($('#expirationdialogTemplate').render());
        
        $('#expirationDateSetter').datepicker({
            dateFormat: $.datepicker.ATOM
        });
    
        $('#expirationdialog').dialog({
            width: 400,
            height: 400,
            modal: true,
            buttons: {
                "Cancel" : function() {
                    $(this).dialog('close');
                },
                "Set" : function() {
                    //set value to the #expirationdate
                    
                    var correctDate = $.datepicker.formatDate($.datepicker.ATOM, $('#expirationDateSetter').datepicker("getDate"));
                   
                    $('#expirationdate').val(correctDate);
                   
                    cpweb.runListCommand(button, listform, cmd);
                    
                    $( this ).dialog( "destroy" );
                } 
                
            }
        });
    });
}

cpweb.adjustExpiration = function(button, listform, cmd) {
    log('show some dialog to adjust the expirationoffset');
    
    require(['dabox-jquery', 'jsrender'], function() {
        
        $('body').append($('#expirationAdjustmentDialogTemplate').render());
        $('#expirationAdjustmentDialog').dialog({
            width: 400,
            height: 400,
            modal: true,
            buttons: {
                "Cancel" : function() {
                    $(this).dialog('close');
                },
                "Adjust" : function() {
                    //set value to the #adjusteddate
                    $('#adjusteddate').val($('#adjustedDateSetter').val());
                    
                    cpweb.runListCommand(button, listform, cmd);
                    
                    $( this ).dialog( "destroy" );
                } 
            }
        });
    });
};



cpweb.rowcbChange = function(checkbox) {
    var pForm = $(checkbox).parents('form').first();

    if (pForm == null) {
        log('No form found');
        return false;
    }

    var hiddenString = $(pForm).find("input[name=__ids]").val();

    var val = String($(checkbox).data('rowid'));

    var allValues = hiddenString == "" ? [] : hiddenString.split(',');

    var checked = $(checkbox).prop('checked');

    if (checked) {
        if (jQuery.inArray(val, allValues) == -1) {
            allValues.push(val);
        }
    } else {
        //Remove state
        var index = jQuery.inArray(val, allValues);
        if (index != -1) {
            allValues.splice(index,1);
        }
    }

    hiddenString = allValues.join(',');

    $(pForm).find("input[name=__ids]").val(hiddenString);

    log('Setting selected values to ',hiddenString);
        
    return true;
}

$('#uploadRoster').submit(function() {
    $('span', this).empty();
 
    $('button', this).removeClass('upload').addClass('uploading');
    $('button', this).animate({
        width: '200px'
    }, 1000);
});


// GENERIC EDITABLE HANDLER FOR CPWEB

cpweb.editable = function(selectorContent, selectorControl, changeListener, opts) {
    require(['bootstrap/editable'], function() {
        var defaultOpts = {
            mode: 'popup',
            emptytext: ''
        };
        var editableOpts =  $.extend(defaultOpts, opts);

        // Make edit control show the editable field
        selectorControl.click(function() {
            selectorContent.click();
            return false;
        });


        // Initiate selectorContent as editable field
        selectorContent.editable(
            editableOpts,
            function(value) {
                changeListener(value);
                return value;
            }
        );

        // Hide and show edit control
        selectorContent.on('shown hidden', function() {
                selectorControl.toggle();
        });

    });
};