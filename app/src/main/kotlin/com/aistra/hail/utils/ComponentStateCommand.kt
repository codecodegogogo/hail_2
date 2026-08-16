package com.aistra.hail.utils

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.IBinder
import com.aistra.hail.BuildConfig
import org.lsposed.hiddenapibypass.HiddenApiBypass

object ComponentStateCommand {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 4)
        val componentName = ComponentName(args[0], args[1])
        val enabled = args[2].toBooleanStrict()
        val userId = args[3].toInt()
        val serviceManager = Class.forName("android.os.ServiceManager")
        val binder = HiddenApiBypass.invoke(
            serviceManager,
            null,
            "getService",
            "package"
        ) as IBinder
        val packageManagerStub = Class.forName("android.content.pm.IPackageManager\$Stub")
        val packageManager = HiddenApiBypass.invoke(
            packageManagerStub,
            null,
            "asInterface",
            binder
        )
        HiddenApiBypass.invoke(
            packageManager::class.java,
            packageManager,
            "setComponentEnabledSetting",
            componentName,
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
            userId,
            BuildConfig.APPLICATION_ID
        )
    }
}
