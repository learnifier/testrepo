[#ftl strip_text="true" /]


[#function errorMessage]
    [#if result.moveError.type == "same_project"]
    [#return "You selected the same project" /]
    [#elseif result.moveError.type == "invalid_source_project"]
    [#return "There is an error related to the project that the participant belongs to" /]
    [#elseif result.moveError.type == "invalid_target_project"]
    [#return "There is an error related to the selected target project" /]
    [#elseif result.moveError.type == "product_constraint"]
    [#return "It is not possible to move the participant due to one or more product related constraints" /]
    [#elseif result.moveError.type == "participation_error"]
    [#return "There is a problem with the selected participant that blocks the move operation" /]
    [#elseif result.moveError.type == "invalid_participation"]
    [#return "There is a problem with the selected participant that blocks the move operation" /]
    [#else]
    [#return "An unknown error occured: " +  result.moveError.cause!result.moveError.type /]
    [/#if]
[/#function]


[#macro productErrors]
<ul class="list-group">
    [#list result.productErrors as productError]
        <li class="list-group-item">${productError.productId?xml} - ${productError.type?xml} </li>
    [/#list]
</ul>
[/#macro]

[#function productErrorMessage type]
    [#if type == "creditAllocationDenied"]
    [#return "Insufficient credits available" /]
    [#elseif type == "creditDeallocationDenied"]
    [#return "Unable to release credits" /]
    [#elseif type == "integrationDeleteDenied"]
    [#return "Not possible to remove this product from source project" /]
    [#elseif type == "integrationMoveDenied"]
    [#return "Not possible to move this product" /]
    [#elseif type == "integrationAddDenied"]
    [#return "Not possible to add this product" /]
    [#elseif type == "productMissing"]
    [#return "Product not found" /]
    [#elseif type == "rollback"]
    [#return "Move failed but rollback unsuccessful as well" /]
    [#else]
    [#return "Unknown error " + type /]
    [/#if]
[/#function]


