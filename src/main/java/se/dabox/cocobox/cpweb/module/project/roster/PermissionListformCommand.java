/*
 * (c) Dabox AB 2013 All Rights Reserved
 */

package se.dabox.cocobox.cpweb.module.project.roster;

import java.util.List;
import se.dabox.cocosite.security.Permission;
import se.dabox.service.webutils.listform.ListformCommand;

/**
 * Interface for ListformCommands that require certain project permissions to execute.
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 * @param <T> The command return value
 */
public interface PermissionListformCommand<T> extends ListformCommand<T> {

    /**
     * Returns the permissions this command requires.
     * <p>Returning null from this method is NOT allowed.</p>
     *
     * @return A list with permissions required
     */
    List<Permission> getPermissionsRequired();

}
