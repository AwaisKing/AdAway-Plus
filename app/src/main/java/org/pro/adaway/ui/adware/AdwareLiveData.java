package org.pro.adaway.ui.adware;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import org.pro.adaway.BuildConfig;
import org.pro.adaway.util.AppExecutors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class is {@link LiveData} to represents installed adware on device.
 */
class AdwareLiveData extends LiveData<List<AdwareInstall>> {
    /**
     * The adware package prefixes.
     */
    private static final String[] AD_PACKAGE_PREFIXES = {
            "com.airpush.",
            "com.adnotify.",
            "com.appbucks.sdk.",
            "com.appenda.",
            "com.applovin.",
            "com.iac.notification.",
            "com.inmobi.",
            "com.Leadbolt.",
            "com.sellaring.",
            "com.senddroid.",
            "com.tapjoy.",
            "cn.kuguo."
    };

    /**
     * The application context.
     */
    private final Context context;

    /**
     * Constructor.
     *
     * @param context The application context.
     */
    AdwareLiveData(Context context) {
        this.context = context;
        AppExecutors.getInstance().diskIO().execute(this::loadData);
    }

    @WorkerThread
    private void loadData() {
        // Get the package manager
        PackageManager pm = this.context.getPackageManager();
        // Get the adware packages
        List<PackageInfo> adwarePackages = this.getAdwarePackages(pm);
        // Create related adware installs
        List<AdwareInstall> adwareInstalls = new ArrayList<>();
        for (PackageInfo adwarePackage : adwarePackages) {
            AdwareInstall installFromPackageInfo = createInstallFromPackageInfo(adwarePackage);
            adwareInstalls.add(installFromPackageInfo);
        }
        Collections.sort(adwareInstalls);
        // Post loaded adware installs
        this.postValue(adwareInstalls);
    }

    /**
     * Finds all installed packages that look like they include a known ad framework
     *
     * @param pm The package manager.
     *
     * @return The found adware package information.
     */
    private List<PackageInfo> getAdwarePackages(PackageManager pm) {
        List<PackageInfo> adPackages = new ArrayList<>();
        // It'd be simpler to just use pm.getInstalledPackages here, but apparently it's broken
        List<ApplicationInfo> applicationInfoList = pm.getInstalledApplications(0);
        for (ApplicationInfo applicationInfo : applicationInfoList) {
            try {
                // Retrieve package information
                PackageInfo packageInfo = pm.getPackageInfo(
                        applicationInfo.packageName,
                        PackageManager.GET_ACTIVITIES | PackageManager.GET_RECEIVERS | PackageManager.GET_SERVICES
                );
                if (this.isAdware(packageInfo)) {
                    adPackages.add(packageInfo);
                }
            } catch (Exception exception) {
                if (BuildConfig.DEBUG) {
                    Log.e("AWAISKING_APP", "An error occurred while scanning applications for adware", exception);
                }
            }
        }
        return adPackages;
    }

    /**
     * Check if application is an adware.
     *
     * @param info The application package information.
     *
     * @return <code>true</code> if the application is an adware, <code>false</code> otherwise.
     */
    private boolean isAdware(PackageInfo info) {
        // Get package name
        String packageName = info.packageName;
        if (BuildConfig.DEBUG) {
            Log.i("AWAISKING_APP", "Scanning package " + packageName);
        }
        // Check package components
        boolean matchActivity = info.activities != null && checkComponent(packageName, "activity", info.activities);
        boolean matchReceiver = info.receivers != null && checkComponent(packageName, "receiver", info.receivers);
        boolean matchService = info.services != null && checkComponent(packageName, "service", info.services);
        return matchActivity || matchReceiver || matchService;
    }

    /**
     * Check if an application component match the adware signature.
     *
     * @param packageName The application package name.
     * @param type        The component type.
     * @param info        The application components to check.
     *
     * @return <code>true</code> if a component matches adware signature, <code>false</code> otherwise.
     */
    private boolean checkComponent(String packageName, String type, ComponentInfo[] info) {
        for (ComponentInfo componentInfo : info) {
            String componentName = componentInfo.name;
            if (BuildConfig.DEBUG) {
                Log.i("AWAISKING_APP", "[" + type + "] " + componentName);
            }
            for (String adPackagePrefix : AD_PACKAGE_PREFIXES) {
                if (componentName.startsWith(adPackagePrefix)) {
                    if (BuildConfig.DEBUG) {
                        Log.i("AWAISKING_APP", "Detected ad framework prefix " + adPackagePrefix
                                + " in package " + packageName + " as " + type + " " + componentName);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Create {@link AdwareInstall} from {@link PackageInfo}.
     *
     * @param packageInfo The package info to convert.
     *
     * @return The related adware install.
     */
    private AdwareInstall createInstallFromPackageInfo(PackageInfo packageInfo) {
        // Get the package manager
        PackageManager pm = this.context.getPackageManager();
        // Retrieve application name
        String applicationName = pm.getApplicationLabel(packageInfo.applicationInfo).toString();
        // Add adware install
        return new AdwareInstall(applicationName, packageInfo.packageName);
    }
}
