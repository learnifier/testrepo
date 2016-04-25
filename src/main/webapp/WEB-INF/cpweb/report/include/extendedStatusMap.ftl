[#ftl strip_text="true" /]

[#macro langKeyMap]
[#compress]
{
        "notAttempted": "${ctext('cpweb:extendedstatus.notAttempted')}",
        "incomplete": "${ctext('cpweb:extendedstatus.incomplete')}",
        "overdue": "${ctext('cpweb:extendedstatus.overdue')}",
        "locked": "${ctext('cpweb:extendedstatus.locked')}",
        "failed": "${ctext('cpweb:extendedstatus.failed')}",
        "passed": "${ctext('cpweb:extendedstatus.passed')}",
        "completed": "${ctext('cpweb:extendedstatus.completed')}",
        "notTracked": "${ctext('cpweb:extendedstatus.notTracked')}"
}
[/#compress]
[/#macro]
