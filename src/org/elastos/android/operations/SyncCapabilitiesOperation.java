/**
 *   ownCloud Android client application
 *
 *   @author masensio
 *   Copyright (C) 2017 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.elastos.android.operations;

import android.accounts.AccountManager;

import org.elastos.android.MainApp;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.GetRemoteCapabilitiesOperation;
import com.owncloud.android.lib.resources.status.GetRemoteStatusOperation;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import org.elastos.android.operations.common.SyncOperation;

/**
 * Get and save capabilities from the server
 */
public class SyncCapabilitiesOperation extends SyncOperation {

    private static final String TAG = SyncCapabilitiesOperation.class.getName();

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        OCCapability capabilities = null;
        OwnCloudVersion serverVersion = null;

        /// Get current value for capabilities from server
        GetRemoteCapabilitiesOperation getCapabilities = new GetRemoteCapabilitiesOperation();
        RemoteOperationResult result = getCapabilities.execute(client);
        if (result.isSuccess()){
            // Read data from the result
            if( result.getData()!= null && result.getData().size() > 0) {
                capabilities = (OCCapability) result.getData().get(0);
                serverVersion = new OwnCloudVersion(capabilities.getVersionString());
            }

        } else {
            Log_OC.w(TAG, "Remote capabilities not available");

            // server version is important; this fallback will try to get it from status.php
            // if capabilities API is not available
            GetRemoteStatusOperation getStatus = new GetRemoteStatusOperation(MainApp.getAppContext());
            RemoteOperationResult statusResult = getStatus.execute(client);
            if (statusResult.isSuccess()) {
                serverVersion = (OwnCloudVersion) (statusResult.getData().get(0));
            }
        }

        /// save data - capabilities in database
        if (capabilities != null) {
            getStorageManager().saveCapabilities(capabilities);
        }

        /// save data - OC version
        // need to save separately version in AccountManager, due to bad dependency in
        // library: com.owncloud.android.lib.common.accounts.AccountUtils#getCredentialsForAccount(...)
        // and com.owncloud.android.lib.common.accounts.AccountUtils#getServerVersionForAccount(...)
        if (serverVersion != null) {
            AccountManager accountMngr = AccountManager.get(MainApp.getAppContext());
            accountMngr.setUserData(
                getStorageManager().getAccount(),
                AccountUtils.Constants.KEY_OC_VERSION,
                serverVersion.getVersion()
            );
        }

        return result;
    }

}
